package com.example.taskrecall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** 主页各分区的拖拽排序持久化。 */
@Dao
interface TaskOrderDao {

    @Query("SELECT * FROM task_orders")
    fun observeAll(): Flow<List<TaskOrderEntity>>

    @Query("SELECT * FROM task_orders")
    suspend fun getAll(): List<TaskOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(orders: List<TaskOrderEntity>)

    @Query("DELETE FROM task_orders WHERE sectionKey = :sectionKey")
    suspend fun deleteSection(sectionKey: String)

    @Query("DELETE FROM task_orders WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)

    @Query("DELETE FROM task_orders")
    suspend fun deleteAll()

    /**
     * 一个区的拖拽结束后，整体替换该区顺序。
     * 同时清除已经离开该区的任务残留顺序。
     */
    @Transaction
    suspend fun replaceSection(sectionKey: String, orders: List<TaskOrderEntity>) {
        deleteSection(sectionKey)
        if (orders.isNotEmpty()) upsertAll(orders)
    }
}
