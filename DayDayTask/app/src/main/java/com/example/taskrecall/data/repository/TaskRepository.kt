package com.example.taskrecall.data.repository

import com.example.taskrecall.data.db.CompletionDao
import com.example.taskrecall.data.db.CompletionEntity
import com.example.taskrecall.data.db.TaskDao
import com.example.taskrecall.data.db.TaskEntity
import com.example.taskrecall.data.db.TaskOrderDao
import com.example.taskrecall.data.db.TaskOrderEntity
import com.example.taskrecall.domain.PeriodKey
import com.example.taskrecall.domain.StatusResolver
import com.example.taskrecall.domain.TaskStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 规格：Repository 聚合任务与完成记录。
 *
 * UI 通过 Flow 观察数据；状态点击先取当前周期的基础状态，再 upsert 下一状态。
 */
@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val completionDao: CompletionDao,
    private val taskOrderDao: TaskOrderDao,
) {

    fun observeAllActiveTasks(): Flow<List<TaskEntity>> = taskDao.observeAllActive()

    fun observeAllCompletions(): Flow<List<CompletionEntity>> = completionDao.observeAll()

    fun observeTaskOrders(): Flow<List<TaskOrderEntity>> = taskOrderDao.observeAll()

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
     * 圆点点击：按任务类型选择当前周期，读取基础状态后循环到下一状态。
     *
     * 逾期动态 FAILED 不写库；因此逾期未处理的任务第一次点击会变成 DONE，
     * 与“点击完成”的直觉一致。
     */
    suspend fun toggleStatus(task: TaskEntity, now: Long = System.currentTimeMillis()): TaskStatus {
        val periodKey = PeriodKey.forTaskAt(task.type, now)
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
