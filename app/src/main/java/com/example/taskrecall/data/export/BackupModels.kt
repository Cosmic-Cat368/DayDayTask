package com.example.taskrecall.data.export

import com.example.taskrecall.domain.TaskStatus
import com.example.taskrecall.domain.TaskType
import kotlinx.serialization.Serializable

/**
 * 规格十二：tasks.json 的结构。
 *
 * displayStatus 只是导出时用 StatusResolver 计算出的“当前显示状态”，
 * 方便排查与外部查看；导入时仍以 completions 里的原始记录为准，
 * 不会把动态逾期状态写回数据库。
 */
@Serializable
data class BackupBundle(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val app: String = "DayDayTask",
    val exportedAt: Long,
    val tasks: List<BackupTask>,
    val completions: List<BackupCompletion>,
    /** v1.1 新增，旧备份没有该字段时默认为空。 */
    val orders: List<BackupOrder> = emptyList(),
    /** v1.3 新增，旧备份没有该字段时默认为空。 */
    val archivedTasks: List<BackupArchivedTask> = emptyList(),
    val archivedCompletions: List<BackupArchivedCompletion> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class BackupTask(
    val id: Long,
    val type: TaskType,
    val shortText: String,
    val detailMarkdown: String? = null,
    val createdAt: Long,
    val dueAt: Long? = null,
    val startAt: Long? = null,
    val deletedAt: Long? = null,
    val displayStatus: TaskStatus? = null,
)

@Serializable
data class BackupCompletion(
    val taskId: Long,
    val periodKey: String,
    val status: TaskStatus,
)

@Serializable
data class BackupOrder(
    val sectionKey: String,
    val taskId: Long,
    val sortOrder: Int,
)

@Serializable
data class BackupArchivedTask(
    val id: Long,
    val sourceTaskId: Long,
    val type: TaskType,
    val shortText: String,
    val createdAt: Long,
    val rangeStart: Long,
    val rangeEnd: Long,
    val archivedAt: Long,
    val deletedAt: Long,
)

@Serializable
data class BackupArchivedCompletion(
    val archiveTaskId: Long,
    val periodKey: String,
    val status: TaskStatus,
)

data class ExportResult(
    val file: java.io.File,
    val contentUri: android.net.Uri,
    val taskCount: Int,
    val imageCount: Int,
)

data class ImportResult(
    val taskCount: Int,
    val completionCount: Int,
    val imageCount: Int,
)
