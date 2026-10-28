package com.example.taskrecall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchivedTaskDao {

    @Insert
    suspend fun insert(task: ArchivedTaskEntity): Long

    /** 导入时保留归档 id，确保归档完成记录可以关联。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<ArchivedTaskEntity>)

    @Transaction
    @Query("SELECT * FROM archived_tasks ORDER BY archivedAt DESC")
    fun observeAllWithCompletions(): Flow<List<ArchivedTaskWithCompletions>>

    @Transaction
    @Query("SELECT * FROM archived_tasks ORDER BY archivedAt DESC")
    suspend fun getAllWithCompletions(): List<ArchivedTaskWithCompletions>

    @Query("SELECT * FROM archived_tasks ORDER BY archivedAt DESC")
    suspend fun getAll(): List<ArchivedTaskEntity>

    @Query("DELETE FROM archived_tasks")
    suspend fun deleteAll()
}

@Dao
interface ArchivedCompletionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(completions: List<ArchivedCompletionEntity>)

    @Query("SELECT * FROM archived_completions ORDER BY periodKey ASC")
    suspend fun getAll(): List<ArchivedCompletionEntity>

    @Query("SELECT * FROM archived_completions WHERE archiveTaskId = :archiveTaskId")
    suspend fun getForTask(archiveTaskId: Long): List<ArchivedCompletionEntity>

    @Query("DELETE FROM archived_completions")
    suspend fun deleteAll()
}
