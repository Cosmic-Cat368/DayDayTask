package com.example.taskrecall.data.db

import androidx.room.Entity
import com.example.taskrecall.domain.CompletionLike
import com.example.taskrecall.domain.TaskStatus

/**
 * 规格九：CompletionEntity。
 *
 * 主键 (taskId, periodKey)：
 * - DAILY 的 periodKey 是 yyyy-MM-dd；
 * - WEEKLY 的 periodKey 是 ISO 周 yyyy-Www；
 * - SINGLE / LONGTERM 固定 once。
 *
 * 时间周期变化后，新周期查不到记录，于是自动表现为 PENDING，
 * 不需要后台任务去“重置”，避免 App 未运行时漏掉重置。
 */
@Entity(
    tableName = "completions",
    primaryKeys = ["taskId", "periodKey"],
)
data class CompletionEntity(
    val taskId: Long,
    val periodKey: String,
    override val status: TaskStatus,
) : CompletionLike
