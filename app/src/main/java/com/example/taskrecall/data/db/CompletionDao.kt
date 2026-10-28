package com.example.taskrecall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 规格十：完成记录按 (taskId, periodKey) 读写，使用 upsert。
 */
@Dao
interface CompletionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(completion: CompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(completions: List<CompletionEntity>)

    @Query("SELECT * FROM completions WHERE taskId = :taskId AND periodKey = :periodKey")
    suspend fun get(taskId: Long, periodKey: String): CompletionEntity?

    @Query("SELECT * FROM completions WHERE taskId = :taskId AND periodKey = :periodKey")
    fun observe(taskId: Long, periodKey: String): Flow<CompletionEntity?>

    @Query("SELECT * FROM completions WHERE taskId = :taskId")
    fun observeForTask(taskId: Long): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE taskId = :taskId")
    suspend fun getAllForTask(taskId: Long): List<CompletionEntity>

    @Query("DELETE FROM completions WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)

    @Query("SELECT * FROM completions WHERE taskId = :taskId AND periodKey = :periodKey")
    fun observeForPeriod(taskId: Long, periodKey: String): Flow<CompletionEntity?>

    @Query("SELECT * FROM completions ORDER BY periodKey ASC")
    fun observeAll(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions ORDER BY periodKey ASC")
    suspend fun getAll(): List<CompletionEntity>

    @Query("DELETE FROM completions")
    suspend fun deleteAll()
}
