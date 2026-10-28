package com.example.taskrecall.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.taskrecall.domain.TaskStatus
import com.example.taskrecall.domain.TaskType

/**
 * 真删除日常/周常时保留的归档任务快照。
 *
 * 归档范围按用户确认的规则：
 * - 开始：任务创建当天
 * - 结束：删除日期的前一天
 * - 只保留删除时已经过去的 DONE/FAILED 记录
 */
@Entity(tableName = "archived_tasks")
data class ArchivedTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sourceTaskId: Long,
    val type: TaskType,
    val shortText: String,
    val createdAt: Long,
    val rangeStart: Long,
    val rangeEnd: Long,
    val archivedAt: Long,
    val deletedAt: Long,
)

@Entity(
    tableName = "archived_completions",
    primaryKeys = ["archiveTaskId", "periodKey"],
)
data class ArchivedCompletionEntity(
    val archiveTaskId: Long,
    val periodKey: String,
    val status: TaskStatus,
)

/** 归档任务 + 每个周期的状态明细。 */
data class ArchivedTaskWithCompletions(
    @Embedded val task: ArchivedTaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "archiveTaskId",
    )
    val completions: List<ArchivedCompletionEntity>,
)
