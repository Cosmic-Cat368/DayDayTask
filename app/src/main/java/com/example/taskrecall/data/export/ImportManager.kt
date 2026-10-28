package com.example.taskrecall.data.export

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.taskrecall.data.db.AppDatabase
import com.example.taskrecall.data.db.ArchivedCompletionDao
import com.example.taskrecall.data.db.ArchivedCompletionEntity
import com.example.taskrecall.data.db.ArchivedTaskDao
import com.example.taskrecall.data.db.ArchivedTaskEntity
import com.example.taskrecall.data.db.CompletionDao
import com.example.taskrecall.data.db.CompletionEntity
import com.example.taskrecall.data.db.TaskDao
import com.example.taskrecall.data.db.TaskEntity
import com.example.taskrecall.data.db.TaskOrderDao
import com.example.taskrecall.data.db.TaskOrderEntity
import com.example.taskrecall.data.repository.ImageStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * 规格十二：覆盖导入。
 *
 * 流程：
 * 1. zip 先解到 cache 的临时目录，校验 tasks.json 与图片路径；
 * 2. 在 Room 事务里清空旧数据并写入备份数据；
 * 3. 用备份 images/ 整体替换私有图片目录。
 *
 * UI 必须先弹出“将覆盖当前数据”的确认框，本类自身不负责确认。
 */
@Singleton
class ImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val taskDao: TaskDao,
    private val completionDao: CompletionDao,
    private val taskOrderDao: TaskOrderDao,
    private val archivedTaskDao: ArchivedTaskDao,
    private val archivedCompletionDao: ArchivedCompletionDao,
    private val imageStorage: ImageStorage,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    suspend fun importFrom(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        importFromStream { context.contentResolver.openInputStream(uri) }
    }

    /** 测试入口：直接读取本地文件，生产路径仍走 content URI。 */
    internal suspend fun importFromFile(file: File): ImportResult = withContext(Dispatchers.IO) {
        importFromStream { file.inputStream() }
    }

    private suspend fun importFromStream(openStream: () -> java.io.InputStream?): ImportResult {
        val staging = File(context.cacheDir, "import-${UUID.randomUUID()}").apply { mkdirs() }
        return try {
            unzipToStaging(openStream, staging)
            val jsonFile = File(staging, "tasks.json")
            require(jsonFile.isFile) { "压缩包内缺少 tasks.json" }

            val backup = runCatching {
                json.decodeFromString<BackupBundle>(jsonFile.readText(Charsets.UTF_8))
            }.getOrElse { error ->
                throw IllegalArgumentException("tasks.json 格式不正确：${error.message}")
            }

            validate(backup)

            val tasks = backup.tasks.map { it.toEntity() }
            val completions = backup.completions
                .distinctBy { it.taskId to it.periodKey }
                .map { CompletionEntity(it.taskId, it.periodKey, it.status) }
            val orders = backup.orders
                .distinctBy { it.sectionKey to it.taskId }
                .map { TaskOrderEntity(it.sectionKey, it.taskId, it.sortOrder) }
            val archivedTasks = backup.archivedTasks.map {
                ArchivedTaskEntity(
                    id = it.id,
                    sourceTaskId = it.sourceTaskId,
                    type = it.type,
                    shortText = it.shortText,
                    createdAt = it.createdAt,
                    rangeStart = it.rangeStart,
                    rangeEnd = it.rangeEnd,
                    archivedAt = it.archivedAt,
                    deletedAt = it.deletedAt,
                )
            }
            val archivedCompletions = backup.archivedCompletions
                .distinctBy { it.archiveTaskId to it.periodKey }
                .map {
                    ArchivedCompletionEntity(it.archiveTaskId, it.periodKey, it.status)
                }

            database.withTransaction {
                completionDao.deleteAll()
                taskOrderDao.deleteAll()
                taskDao.deleteAll()
                archivedCompletionDao.deleteAll()
                archivedTaskDao.deleteAll()
                if (tasks.isNotEmpty()) taskDao.insertAll(tasks)
                if (completions.isNotEmpty()) completionDao.upsertAll(completions)
                if (orders.isNotEmpty()) taskOrderDao.upsertAll(orders)
                if (archivedTasks.isNotEmpty()) archivedTaskDao.insertAll(archivedTasks)
                if (archivedCompletions.isNotEmpty()) {
                    archivedCompletionDao.insertAll(archivedCompletions)
                }
            }

            val stagedImages = File(staging, "images").takeIf { it.isDirectory }
            val imageCount = stagedImages
                ?.walkTopDown()
                ?.count { it.isFile } ?: 0
            imageStorage.replaceImages(stagedImages)

            ImportResult(
                taskCount = tasks.size,
                completionCount = completions.size,
                imageCount = imageCount,
            )
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun unzipToStaging(openStream: () -> java.io.InputStream?, staging: File) {
        var totalBytes = 0L
        val maxBytes = 256L * 1024 * 1024
        val stagingRoot = staging.canonicalFile

        openStream()?.use { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val entryName = entry.name.replace('\\', '/').trimStart('/')
                    if (entry.isDirectory || entryName.isEmpty()) {
                        zip.closeEntry()
                        continue
                    }
                    // 只接受根 tasks.json 与 images/ 下的文件，避免 zip slip 与垃圾文件。
                    if (entryName != "tasks.json" && !entryName.startsWith("images/")) {
                        zip.closeEntry()
                        continue
                    }
                    val target = File(staging, entryName).canonicalFile
                    if (!target.path.startsWith(stagingRoot.path + File.separator)) {
                        throw IllegalArgumentException("压缩包包含非法路径")
                    }
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read <= 0) break
                            totalBytes += read
                            require(totalBytes <= maxBytes) { "备份文件过大" }
                            output.write(buffer, 0, read)
                        }
                    }
                    zip.closeEntry()
                }
            }
        } ?: throw IllegalArgumentException("无法读取所选文件")
    }

    private fun validate(backup: BackupBundle) {
        require(backup.schemaVersion <= BackupBundle.CURRENT_SCHEMA_VERSION) {
            "备份版本过新，请升级 App 后再导入"
        }
        require(backup.tasks.map { it.id }.toSet().size == backup.tasks.size) {
            "备份中存在重复的任务 id"
        }
        require(backup.tasks.all { it.shortText.isNotBlank() }) {
            "备份中存在简写为空的任务"
        }
        val ids = backup.tasks.map { it.id }.toSet()
        require(backup.completions.all { it.taskId in ids }) {
            "备份中存在找不到对应任务的完成记录"
        }
        require(backup.completions.all { it.periodKey.isNotBlank() }) {
            "备份中存在空的周期键"
        }
        require(backup.orders.all { it.taskId in ids }) {
            "备份中存在找不到对应任务的排序记录"
        }
        require(backup.orders.all { it.sectionKey.isNotBlank() }) {
            "备份中存在空的分区键"
        }
        require(backup.archivedTasks.map { it.id }.toSet().size == backup.archivedTasks.size) {
            "备份中存在重复的归档 id"
        }
        require(backup.archivedTasks.all { it.shortText.isNotBlank() }) {
            "备份中存在简写为空的归档任务"
        }
        val archiveIds = backup.archivedTasks.map { it.id }.toSet()
        require(backup.archivedCompletions.all { it.archiveTaskId in archiveIds }) {
            "备份中存在找不到对应归档任务的记录"
        }
        require(backup.archivedCompletions.all { it.periodKey.isNotBlank() }) {
            "备份中存在空的归档周期键"
        }
    }

    private fun BackupTask.toEntity(): TaskEntity = TaskEntity(
        id = id,
        type = type,
        shortText = shortText,
        detailMarkdown = detailMarkdown,
        createdAt = createdAt,
        dueAt = dueAt,
        startAt = startAt,
        deletedAt = deletedAt,
    )
}
