package com.example.taskrecall.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.taskrecall.data.db.AppDatabase
import com.example.taskrecall.data.db.CompletionDao
import com.example.taskrecall.data.db.CompletionEntity
import com.example.taskrecall.data.db.TaskDao
import com.example.taskrecall.data.db.TaskEntity
import com.example.taskrecall.data.db.TaskOrderDao
import com.example.taskrecall.data.db.TaskOrderEntity
import com.example.taskrecall.data.repository.ImageStorage
import com.example.taskrecall.data.repository.TaskRepository
import com.example.taskrecall.domain.DateTimeUtils
import com.example.taskrecall.domain.PeriodKey
import com.example.taskrecall.domain.TaskStatus
import com.example.taskrecall.domain.TaskType
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 规格十 / 十一的 DAO 验证：
 * - todayTasks 只返回当天 SINGLE + LONGTERM_A；
 * - nextWeekTasks 只返回未来 7 天 SINGLE；
 * - LONGTERM_A 同时出现在今日区与长期区；
 * - 完成记录 upsert；
 * - 软删除后查询隐藏，但完成记录仍可查。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var completionDao: CompletionDao
    private lateinit var taskOrderDao: TaskOrderDao

    private val today: LocalDate = LocalDate.of(2026, 10, 25)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
        completionDao = database.completionDao()
        taskOrderDao = database.taskOrderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun todayTasks_returnsSinglesAndLongtermAOnDueDate_sorted() = runTest {
        val todayStart = DateTimeUtils.startOfDay(today)
        val todayEnd = DateTimeUtils.endOfDay(today)

        val singleLater = insert(TaskType.SINGLE, dueAt = todayStart + 20_000)
        val longtermA = insert(TaskType.LONGTERM_A, dueAt = todayStart + 10_000)
        val singleEarlier = insert(TaskType.SINGLE, dueAt = todayStart + 5_000)
        insert(TaskType.SINGLE, dueAt = DateTimeUtils.endOfDay(today.plusDays(1)))
        insert(TaskType.LONGTERM_B)
        insert(TaskType.DAILY, startAt = todayStart)

        val result = taskDao.todayTasks(todayStart, todayEnd)

        assertEquals(
            listOf(singleEarlier, longtermA, singleLater),
            result.map { it.id },
        )
        // LONGTERM_A 同时在长期 A 查询中，实现双区出现。
        assertTrue(taskDao.longtermATasks().any { it.id == longtermA })
    }

    @Test
    fun nextWeekTasks_returnsOnlySinglesInFutureSevenDays() = runTest {
        val todayEnd = DateTimeUtils.endOfDay(today)
        val weekEnd = DateTimeUtils.endOfDay(today.plusDays(7))

        insert(TaskType.SINGLE, dueAt = todayEnd)
        val tomorrow = insert(TaskType.SINGLE, dueAt = DateTimeUtils.endOfDay(today.plusDays(1)))
        val day7 = insert(TaskType.SINGLE, dueAt = DateTimeUtils.endOfDay(today.plusDays(7)))
        insert(TaskType.SINGLE, dueAt = DateTimeUtils.endOfDay(today.plusDays(8)))
        insert(TaskType.LONGTERM_A, dueAt = DateTimeUtils.endOfDay(today.plusDays(1)))

        val result = taskDao.nextWeekTasks(todayEnd, weekEnd)

        assertEquals(listOf(tomorrow, day7), result.map { it.id })
    }

    @Test
    fun typeQueries_returnActiveTasksOnly() = runTest {
        val longtermA = insert(TaskType.LONGTERM_A, dueAt = DateTimeUtils.endOfDay(today))
        val longtermB = insert(TaskType.LONGTERM_B)
        val daily = insert(TaskType.DAILY, startAt = DateTimeUtils.startOfDay(today))
        val weekly = insert(TaskType.WEEKLY, startAt = DateTimeUtils.startOfDay(today))
        val deleted = insert(TaskType.SINGLE, dueAt = DateTimeUtils.endOfDay(today))
        taskDao.softDelete(deleted, System.currentTimeMillis())

        assertTrue(taskDao.longtermATasks().any { it.id == longtermA })
        assertTrue(taskDao.longtermBTasks().any { it.id == longtermB })
        assertTrue(taskDao.dailyTasks().any { it.id == daily })
        assertTrue(taskDao.weeklyTasks().any { it.id == weekly })
        assertTrue(taskDao.todayTasks(
            DateTimeUtils.startOfDay(today),
            DateTimeUtils.endOfDay(today),
        ).none { it.id == deleted })
        // 软删除仍然保留记录，导出时能查到。
        assertTrue(taskDao.getAllIncludingDeleted().any { it.id == deleted })
    }

    @Test
    fun completionUpsert_keepsOneRowPerPeriod() = runTest {
        val taskId = insert(TaskType.SINGLE, dueAt = DateTimeUtils.endOfDay(today))

        completionDao.upsert(CompletionEntity(taskId, "once", TaskStatus.PENDING))
        completionDao.upsert(CompletionEntity(taskId, "once", TaskStatus.DONE))

        val stored = completionDao.get(taskId, "once")
        assertEquals(TaskStatus.DONE, stored?.status)
        assertEquals(1, completionDao.getAll().count { it.taskId == taskId })
    }

    @Test
    fun softDelete_keepsCompletionRecords() = runTest {
        val taskId = insert(TaskType.SINGLE, dueAt = DateTimeUtils.endOfDay(today))
        completionDao.upsert(CompletionEntity(taskId, "once", TaskStatus.DONE))

        taskDao.softDelete(taskId, System.currentTimeMillis())

        assertEquals(TaskStatus.DONE, completionDao.get(taskId, "once")?.status)
        assertTrue(taskDao.getById(taskId)?.deletedAt != null)
    }

    @Test
    fun reorderSection_onlyChangesThatSection() = runTest {
        val taskA = insert(TaskType.SINGLE, dueAt = DateTimeUtils.endOfDay(today))
        val taskB = insert(TaskType.SINGLE, dueAt = DateTimeUtils.endOfDay(today))

        taskOrderDao.replaceSection(
            "today",
            listOf(
                TaskOrderEntity("today", taskA, 0),
                TaskOrderEntity("today", taskB, 1),
            ),
        )
        taskOrderDao.replaceSection(
            "daily",
            listOf(TaskOrderEntity("daily", taskA, 0)),
        )

        taskOrderDao.replaceSection(
            "today",
            listOf(
                TaskOrderEntity("today", taskB, 0),
                TaskOrderEntity("today", taskA, 1),
            ),
        )

        val todayOrders = taskOrderDao.getAll()
            .filter { it.sectionKey == "today" }
            .sortedBy { it.sortOrder }
        assertEquals(listOf(taskB, taskA), todayOrders.map { it.taskId })
        // 另一个区的顺序不受影响。
        assertTrue(
            taskOrderDao.getAll().any {
                it.sectionKey == "daily" && it.taskId == taskA
            },
        )
    }

    @Test
    fun toggleStatusForPeriod_writesTheSelectedHistoryPeriodOnly() = runTest {
        val taskId = insert(TaskType.DAILY, startAt = DateTimeUtils.startOfDay(today.minusDays(5)))
        val task = taskDao.getById(taskId)!!
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = TaskRepository(
            database = database,
            taskDao = taskDao,
            completionDao = completionDao,
            taskOrderDao = taskOrderDao,
            archivedTaskDao = database.archivedTaskDao(),
            archivedCompletionDao = database.archivedCompletionDao(),
            imageStorage = ImageStorage(context),
        )
        val historyDate = today.minusDays(3)
        val historyKey = PeriodKey.daily(historyDate)

        repository.toggleStatusForPeriod(task, historyKey)

        assertEquals(TaskStatus.DONE, completionDao.get(taskId, historyKey)?.status)
        // 当前周期没有被误改。
        assertNull(completionDao.get(taskId, PeriodKey.daily(today)))
    }

    @Test
    fun hardDeleteDaily_archivesOnlyPastDoneOrFailedAndRemovesFuture() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = TaskRepository(
            database = database,
            taskDao = taskDao,
            completionDao = completionDao,
            taskOrderDao = taskOrderDao,
            archivedTaskDao = database.archivedTaskDao(),
            archivedCompletionDao = database.archivedCompletionDao(),
            imageStorage = ImageStorage(context),
        )
        val createdAt = DateTimeUtils.startOfDay(today.minusDays(5))
        val taskId = taskDao.insert(
            TaskEntity(
                type = TaskType.DAILY,
                shortText = "要删除的日常",
                createdAt = createdAt,
                startAt = createdAt,
            ),
        )
        val keep1 = PeriodKey.daily(today.minusDays(3))
        val keep2 = PeriodKey.daily(today.minusDays(2))
        val todayKey = PeriodKey.daily(today)
        val futureKey = PeriodKey.daily(today.plusDays(1))
        completionDao.upsert(CompletionEntity(taskId, keep1, TaskStatus.DONE))
        completionDao.upsert(CompletionEntity(taskId, keep2, TaskStatus.FAILED))
        completionDao.upsert(CompletionEntity(taskId, todayKey, TaskStatus.DONE))
        completionDao.upsert(CompletionEntity(taskId, futureKey, TaskStatus.DONE))
        completionDao.upsert(CompletionEntity(taskId, PeriodKey.daily(today.minusDays(4)), TaskStatus.PENDING))
        taskOrderDao.upsertAll(listOf(TaskOrderEntity("daily", taskId, 0)))

        repo.deleteTaskPermanently(
            taskId = taskId,
            deletedAt = DateTimeUtils.atDateTime(today, LocalTime.NOON),
        )

        assertNull(taskDao.getById(taskId))
        assertTrue(completionDao.getAllForTask(taskId).isEmpty())
        assertTrue(taskOrderDao.getAll().none { it.taskId == taskId })

        val archived = database.archivedTaskDao().getAllWithCompletions().single()
        assertEquals("要删除的日常", archived.task.shortText)
        assertEquals(DateTimeUtils.startOfDay(today.minusDays(5)), archived.task.rangeStart)
        assertEquals(DateTimeUtils.startOfDay(today.minusDays(1)), archived.task.rangeEnd)
        assertEquals(
            setOf(keep1, keep2),
            archived.completions.map { it.periodKey }.toSet(),
        )
    }

    private suspend fun insert(
        type: TaskType,
        dueAt: Long? = null,
        startAt: Long? = null,
    ): Long = taskDao.insert(
        TaskEntity(
            type = type,
            shortText = "任务-$type",
            createdAt = System.currentTimeMillis(),
            dueAt = dueAt,
            startAt = startAt,
        ),
    )
}
