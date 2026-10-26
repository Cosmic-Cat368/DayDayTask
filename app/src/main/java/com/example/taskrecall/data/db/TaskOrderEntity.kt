package com.example.taskrecall.data.db

import androidx.room.Entity

/**
 * 主页各分区的自定义排序。
 *
 * 使用独立表而不是 TaskEntity 字段，是因为同一任务可能同时出现在两个区
 * （例如 LONGTERM_A 同时在今日区和长期时限型区），两个区需要各自独立的顺序。
 *
 * sectionKey 取值见 TaskSectionKey。
 * 主键 (sectionKey, taskId) 保证一个任务在一个区只有一条顺序记录。
 */
@Entity(
    tableName = "task_orders",
    primaryKeys = ["sectionKey", "taskId"],
)
data class TaskOrderEntity(
    val sectionKey: String,
    val taskId: Long,
    val sortOrder: Int,
)
