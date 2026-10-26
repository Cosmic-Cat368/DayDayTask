package com.example.taskrecall.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.taskrecall.domain.TaskType

/**
 * 规格九：TaskEntity。
 *
 * 删除一律使用 deletedAt 软删除，保证 CompletionEntity 仍可查询。
 * dueAt 与 startAt 都存 epoch millis（UTC 时间点），显示时再转本地时区。
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["type"]),
        Index(value = ["dueAt"]),
        Index(value = ["deletedAt"]),
    ],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: TaskType,
    val shortText: String,
    val detailMarkdown: String? = null,
    val createdAt: Long,
    val dueAt: Long? = null,
    val startAt: Long? = null,
    val deletedAt: Long? = null,
)
