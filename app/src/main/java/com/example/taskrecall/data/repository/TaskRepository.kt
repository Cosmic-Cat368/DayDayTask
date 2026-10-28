package com.example.taskrecall.data.repository

import androidx.room.withTransaction
import com.example.taskrecall.data.db.AppDatabase
import com.example.taskrecall.data.db.ArchivedCompletionDao
import com.example.taskrecall.data.db.ArchivedCompletionEntity
import com.example.taskrecall.data.db.ArchivedTaskDao
import com.example.taskrecall.data.db.ArchivedTaskEntity
import com.example.taskrecall.data.db.ArchivedTaskWithCompletions
import com.example.taskrecall.data.db.CompletionDao
import com.example.taskrecall.data.db.CompletionEntity
import com.example.taskrecall.data.db.TaskDao
import com.example.taskrecall.data.db.TaskEntity
import com.example.taskrecall.data.db.TaskOrderDao
import com.example.taskrecall.data.db.TaskOrderEntity
import com.example.taskrecall.domain.DateTimeUtils
import com.example.taskrecall.domain.MarkdownImages
import com.example.taskrecall.domain.PeriodKey
import com.example.taskrecall.domain.StatusResolver
import com.example.taskrecall.domain.TaskStatus
import com.example.taskrecall.domain.TaskType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository 聚合任务、完成记录、拖拽排序和归档任务。
 */
@Singleton
class TaskRepository @Inject constructor(
    private val database: AppDatabase,
    private val taskDao: TaskDao,
    private val completionDao: CompletionDao,
    private val taskOrderDao: TaskOrderDao,
    private val archivedTaskDao: ArchivedTaskDao,
    private val archivedCompletionDao: ArchivedCompletionDao,
    private val imageStorage: ImageStorage,
) {

    fun observeAllActiveTasks(): Flow<List<TaskEntity>> = taskDao.observeAllActive()

    fun observeAllCompletions(): Flow<List<CompletionEntity>> = completionDao.observeAll()

    fun observeTaskOrders(): Flow<List<TaskOrderEntity>> = taskOrderDao.observeAll()

    fun observeArchivedTasks(): Flow<List<ArchivedTaskWithCompletions>> =
        archivedTaskDao.observeAllWithCompletions()

    fun observeTask(taskId: Long): Flow<TaskEntity?> = taskDao.observeById(taskId)

    fun observeCompletion(taskId: Long, periodKey: String): Flow<CompletionEntity?> =
        completionDao.observe(taskId, periodKey)

    fun observeCompletionsForTask(taskId: Long): Flow<List<CompletionEntity>> =
        completionDao.observeForTask(taskId)

    suspend fun getTask(taskId: Long): TaskEntity? = taskDao.getById(taskId)

    suspend fun addTask(task: TaskEntity): Long = taskDao.insert(task)

    suspend fun softDelete(taskId: Long, deletedAt: Long = System.currentTimeMillis()) {
        taskDao.softDelete(taskId, deletedAt)
    }

    /**
     * 真删除任务。
     *
     * 日常/周常：
     * - 只把“已过去且状态为 DONE/FAILED”的记录复制到归档表；
     * - 当天/本周及未来记录直接删除；
     * - 任务行、所有完成记录、拖拽排序全部物理删除。
     *
     * 单次/长期：任务和 once 记录直接物理删除，不归档。
     * 如果图片不再被任何剩余任务引用，同时删除私有目录里的图片文件。
     */
    suspend fun deleteTaskPermanently(
        taskId: Long,
        deletedAt: Long = System.currentTimeMillis(),
    ) = withContext(Dispatchers.IO) {
        val task = taskDao.getById(taskId) ?: return@withContext
        val today = DateTimeUtils.toLocalDate(deletedAt)

        database.withTransaction {
            if (task.type == TaskType.DAILY || task.type == TaskType.WEEKLY) {
                val cutoffKey = when (task.type) {
                    TaskType.DAILY -> PeriodKey.daily(today)
                    TaskType.WEEKLY -> PeriodKey.weekly(today)
                    else -> ""
                }
                val archivedCompletions = completionDao.getAllForTask(taskId)
                    .filter { it.status != TaskStatus.PENDING && it.periodKey < cutoffKey }

                if (archivedCompletions.isNotEmpty()) {
                    val archivedTaskId = archivedTaskDao.insert(
                        ArchivedTaskEntity(
                            sourceTaskId = task.id,
                            type = task.type,
                            shortText = task.shortText,
                            createdAt = task.createdAt,
                            rangeStart = DateTimeUtils.startOfDay(
                                DateTimeUtils.toLocalDate(task.createdAt),
                            ),
                            rangeEnd = DateTimeUtils.startOfDay(today.minusDays(1)),
                            archivedAt = deletedAt,
                            deletedAt = deletedAt,
                        ),
                    )
                    archivedCompletionDao.insertAll(
                        archivedCompletions.map {
                            ArchivedCompletionEntity(
                                archiveTaskId = archivedTaskId,
                                periodKey = it.periodKey,
                                status = it.status,
                            )
                        },
                    )
                }
            }

            completionDao.deleteForTask(taskId)
            taskOrderDao.deleteForTask(taskId)
            taskDao.deleteById(taskId)
        }

        // 真删除之后，清理只有该任务引用过的图片。
        val deletedImagePaths = MarkdownImages.referencedPaths(task.detailMarkdown)
        if (deletedImagePaths.isNotEmpty()) {
            val stillReferenced = taskDao.getAllIncludingDeleted()
                .flatMap { MarkdownImages.referencedPaths(it.detailMarkdown) }
                .toSet()
            deletedImagePaths
                .filterNot { it in stillReferenced }
                .forEach { imageStorage.resolveRelative(it)?.delete() }
        }
    }

    /**
     * 圆点点击：按任务类型选择当前周期，读取基础状态后循环到下一状态。
     */
    suspend fun toggleStatus(task: TaskEntity, now: Long = System.currentTimeMillis()): TaskStatus =
        toggleStatusForPeriod(task, PeriodKey.forTaskAt(task.type, now))

    /**
     * 指定周期键切换状态。
     * 历史日期视图会传入选中日期/周的 periodKey，从而修改“当时”的记录。
     */
    suspend fun toggleStatusForPeriod(task: TaskEntity, periodKey: String): TaskStatus {
        val current = completionDao.get(task.id, periodKey)
        val next = StatusResolver.nextBaseStatus(
            StatusResolver.baseStatus(current),
        )
        completionDao.upsert(
            CompletionEntity(
                taskId = task.id,
                periodKey = periodKey,
                status = next,
            ),
        )
        return next
    }

    suspend fun setStatus(taskId: Long, periodKey: String, status: TaskStatus) {
        completionDao.upsert(CompletionEntity(taskId, periodKey, status))
    }

    suspend fun getCompletion(taskId: Long, periodKey: String): CompletionEntity? =
        completionDao.get(taskId, periodKey)

    /**
     * 保存某个区的拖拽结果。
     * 会整体替换该区顺序，避免已移出该区的任务留下旧顺序。
     */
    suspend fun reorderSection(sectionKey: String, orderedTaskIds: List<Long>) {
        val orders = orderedTaskIds.mapIndexed { index, taskId ->
            TaskOrderEntity(
                sectionKey = sectionKey,
                taskId = taskId,
                sortOrder = index,
            )
        }
        taskOrderDao.replaceSection(sectionKey, orders)
    }

    // 以下查询保留规格要求的 DAO 形态，供测试和后续统计使用。
    suspend fun todayTasks(dayStart: Long, dayEnd: Long): List<TaskEntity> =
        taskDao.todayTasks(dayStart, dayEnd)

    suspend fun nextWeekTasks(dayEnd: Long, weekEnd: Long): List<TaskEntity> =
        taskDao.nextWeekTasks(dayEnd, weekEnd)

    suspend fun longtermATasks(): List<TaskEntity> = taskDao.longtermATasks()

    suspend fun longtermBTasks(): List<TaskEntity> = taskDao.longtermBTasks()

    suspend fun dailyTasks(): List<TaskEntity> = taskDao.dailyTasks()

    suspend fun weeklyTasks(): List<TaskEntity> = taskDao.weeklyTasks()
}
