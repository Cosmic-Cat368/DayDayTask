package com.example.taskrecall.data.export

import android.content.Context
import androidx.core.content.FileProvider
import com.example.taskrecall.data.db.ArchivedTaskDao
import com.example.taskrecall.data.db.CompletionDao
import com.example.taskrecall.data.db.TaskDao
import com.example.taskrecall.data.db.TaskOrderDao
import com.example.taskrecall.data.repository.ImageStorage
import com.example.taskrecall.domain.DateTimeUtils
import com.example.taskrecall.domain.MarkdownImages
import com.example.taskrecall.domain.PeriodKey
import com.example.taskrecall.domain.StatusResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 规格十二：导出 zip。
 *
 * zip 内含：
 * - tasks.json（任务 + 完成记录 + 导出时动态显示状态）
 * - images/（Markdown 引用的全部图片，保持相对路径）
 *
 * JSON 里不写图片绝对路径。
 */
@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val completionDao: CompletionDao,
    private val taskOrderDao: TaskOrderDao,
    private val archivedTaskDao: ArchivedTaskDao,
    private val imageStorage: ImageStorage,
) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun export(): ExportResult = withContext(Dispatchers.IO) {
        val prepared = buildExportZip()
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            prepared.file,
        )
        ExportResult(
            file = prepared.file,
            contentUri = uri,
            taskCount = prepared.taskCount,
            imageCount = prepared.imageCount,
        )
    }

    /**
     * 只生成 zip，不创建 FileProvider URI。
     * 抽出来是为了让 JVM/Robolectric 测试能验证 zip 内容；
     * 生产入口仍然是上面的 export()。
     */
    internal suspend fun exportToFile(): PreparedExport = withContext(Dispatchers.IO) {
        buildExportZip()
    }

    private suspend fun buildExportZip(): PreparedExport {
        val now = System.currentTimeMillis()
        val tasks = taskDao.getAllIncludingDeleted()
        val completions = completionDao.getAll()
        val orders = taskOrderDao.getAll()
        val archivedTasks = archivedTaskDao.getAllWithCompletions()
        val completionIndex = completions.associateBy { it.taskId to it.periodKey }

        val backupTasks = tasks.map { task ->
            val key = PeriodKey.forTaskAt(task.type, now)
            BackupTask(
                id = task.id,
                type = task.type,
                shortText = task.shortText,
                detailMarkdown = task.detailMarkdown,
                createdAt = task.createdAt,
                dueAt = task.dueAt,
                startAt = task.startAt,
                deletedAt = task.deletedAt,
                displayStatus = StatusResolver.displayStatus(
                    task = task,
                    completion = completionIndex[task.id to key],
                    now = now,
                ),
            )
        }
        val backup = BackupBundle(
            exportedAt = now,
            tasks = backupTasks,
            completions = completions.map {
                BackupCompletion(it.taskId, it.periodKey, it.status)
            },
            orders = orders.map {
                BackupOrder(it.sectionKey, it.taskId, it.sortOrder)
            },
            archivedTasks = archivedTasks.map {
                BackupArchivedTask(
                    id = it.task.id,
                    sourceTaskId = it.task.sourceTaskId,
                    type = it.task.type,
                    shortText = it.task.shortText,
                    createdAt = it.task.createdAt,
                    rangeStart = it.task.rangeStart,
                    rangeEnd = it.task.rangeEnd,
                    archivedAt = it.task.archivedAt,
                    deletedAt = it.task.deletedAt,
                )
            },
            archivedCompletions = archivedTasks.flatMap { task ->
                task.completions.map {
                    BackupArchivedCompletion(
                        archiveTaskId = it.archiveTaskId,
                        periodKey = it.periodKey,
                        status = it.status,
                    )
                }
            },
        )

        val referencedImages = tasks
            .flatMap { MarkdownImages.referencedPaths(it.detailMarkdown) }
            .distinct()

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        exportDir.listFiles()?.forEach { it.delete() }

        val timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.UK),
        )
        val zipFile = File(exportDir, "DayDayTask-backup-$timestamp.zip")

        var imageCount = 0
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zip ->
            val jsonBytes = json.encodeToString(backup).toByteArray(Charsets.UTF_8)
            zip.putNextEntry(ZipEntry("tasks.json"))
            zip.write(jsonBytes)
            zip.closeEntry()

            referencedImages.forEach { relativePath ->
                val imageFile = imageStorage.resolveRelative(relativePath) ?: return@forEach
                zip.putNextEntry(ZipEntry(relativePath))
                imageFile.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
                imageCount++
            }
        }

        return PreparedExport(
            file = zipFile,
            taskCount = tasks.size,
            imageCount = imageCount,
        )
    }

    /**
     * 供 UI 显示导出文件名。
     * 规格未要求，但分享时给用户一个可读名称。
     */
    fun displayName(result: ExportResult): String {
        val date = DateTimeUtils.formatFullDate(result.file.lastModified())
        return "DayDayTask 备份 $date"
    }
}

internal data class PreparedExport(
    val file: File,
    val taskCount: Int,
    val imageCount: Int,
)
