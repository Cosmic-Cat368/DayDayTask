package com.example.taskrecall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 规格十：TaskDao。
 *
 * 今日区、近一周区、长期 A/B、日常、周常查询都使用 deletedAt IS NULL。
 * 删除使用 update deletedAt，而不是 DELETE。
 */
@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: TaskEntity): Long

    /** 导入覆盖时保留原 id，供 CompletionEntity 关联。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAllActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAt ASC")
    suspend fun getAllIncludingDeleted(): List<TaskEntity>

    @Query("UPDATE tasks SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    /**
     * 规格十：今日任务。
     * 包含今天到期的 SINGLE 与 LONGTERM_A，按 dueAt 升序。
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL
          AND (type = 'SINGLE' OR type = 'LONGTERM_A')
          AND dueAt BETWEEN :dayStart AND :dayEnd
        ORDER BY dueAt ASC
        """,
    )
    suspend fun todayTasks(dayStart: Long, dayEnd: Long): List<TaskEntity>

    /**
     * 规格十：近一周任务。
     * 仅 SINGLE，排除今天，截止时间在未来 7 天内。
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL
          AND type = 'SINGLE'
          AND dueAt > :dayEnd
          AND dueAt <= :weekEnd
        ORDER BY dueAt ASC
        """,
    )
    suspend fun nextWeekTasks(dayEnd: Long, weekEnd: Long): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND type = 'LONGTERM_A'
        ORDER BY dueAt ASC, createdAt ASC
        """,
    )
    suspend fun longtermATasks(): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND type = 'LONGTERM_B'
        ORDER BY createdAt ASC
        """,
    )
    suspend fun longtermBTasks(): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND type = 'DAILY'
        ORDER BY createdAt ASC
        """,
    )
    suspend fun dailyTasks(): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND type = 'WEEKLY'
        ORDER BY createdAt ASC
        """,
    )
    suspend fun weeklyTasks(): List<TaskEntity>
}
