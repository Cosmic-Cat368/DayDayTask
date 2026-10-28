package com.example.taskrecall.data.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import com.example.taskrecall.domain.DateTimeUtils
import com.example.taskrecall.domain.TaskStatus
import com.example.taskrecall.domain.TaskType
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipFile
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 规格十二的端到端验证（Robolectric）：
 * 导出 zip 必须包含 tasks.json 和 images/；
 * 导入必须覆盖数据库、恢复图片，并移除旧图片。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var completionDao: CompletionDao
    private lateinit var taskOrderDao: TaskOrderDao
    private lateinit var archivedTaskDao: ArchivedTaskDao
    private lateinit var archivedCompletionDao: ArchivedCompletionDao
    private lateinit var imageStorage: ImageStorage
    private lateinit var exportManager: ExportManager
    private lateinit var importManager: ImportManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.filesDir.resolve("images").deleteRecursively()
        context.cacheDir.resolve("exports").deleteRecursively()

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
        completionDao = database.completionDao()
        taskOrderDao = database.taskOrderDao()
        archivedTaskDao = database.archivedTaskDao()
        archivedCompletionDao = database.archivedCompletionDao()
        imageStorage = ImageStorage(context)
        exportManager = ExportManager(
            context,
            taskDao,
            completionDao,
            taskOrderDao,
            archivedTaskDao,
            imageStorage,
        )
        importManager = ImportManager(
            context = context,
            database = database,
            taskDao = taskDao,
            completionDao = completionDao,
            taskOrderDao = taskOrderDao,
            archivedTaskDao = archivedTaskDao,
            archivedCompletionDao = archivedCompletionDao,
            imageStorage = imageStorage,
        )
    }

    @After
    fun tearDown() {
        database.close()
        context.filesDir.resolve("images").deleteRecursively()
        context.cacheDir.resolve("exports").deleteRecursively()
    }

    @Test
    fun exportAndImport_roundTripPreservesTasksCompletionsAndImages() = runTest {
        val imageBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
        File(imagesDir, "photo.jpg").writeBytes(imageBytes)

        val imagePath = "images/photo.jpg"
        val taskId = taskDao.insert(
            TaskEntity(
                type = TaskType.SINGLE,
                shortText = "带图片的任务",
                detailMarkdown = "![图]($imagePath)",
                createdAt = 1,
                dueAt = DateTimeUtils.endOfDay(LocalDate.now()),
            ),
        )
        completionDao.upsert(CompletionEntity(taskId, "once", TaskStatus.DONE))
        taskOrderDao.upsertAll(listOf(TaskOrderEntity("today", taskId, 0)))
        val archiveId = archivedTaskDao.insert(
            ArchivedTaskEntity(
                sourceTaskId = 99,
                type = TaskType.DAILY,
                shortText = "已归档日常",
                createdAt = 10,
                rangeStart = 10,
                rangeEnd = 20,
                archivedAt = 30,
                deletedAt = 30,
            ),
        )
        archivedCompletionDao.insertAll(
            listOf(ArchivedCompletionEntity(archiveId, "2026-10-20", TaskStatus.FAILED)),
        )

        val exported = exportManager.exportToFile()
        assertTrue(exported.file.isFile)

        ZipFile(exported.file).use { zip ->
            assertTrue(zip.getEntry("tasks.json") != null)
            assertTrue(zip.getEntry(imagePath) != null)
            val json = zip.getInputStream(zip.getEntry("tasks.json")).bufferedReader().readText()
            assertTrue(json.contains("带图片的任务"))
            assertTrue(json.contains("photo.jpg"))
        }

        // 模拟换机或覆盖前本机已有其他任务/图片。
        taskDao.insert(
            TaskEntity(
                type = TaskType.LONGTERM_B,
                shortText = "旧任务",
                createdAt = 2,
            ),
        )
        File(imagesDir, "old.jpg").writeBytes(byteArrayOf(9))

        val result = importManager.importFromFile(exported.file)

        assertEquals(1, result.taskCount)
        assertEquals(1, result.completionCount)
        assertEquals(1, result.imageCount)
        assertEquals(1, taskDao.getAllIncludingDeleted().size)
        assertEquals("带图片的任务", taskDao.getAllIncludingDeleted().first().shortText)
        assertEquals(TaskStatus.DONE, completionDao.get(taskId, "once")?.status)
        assertEquals("today", taskOrderDao.getAll().single().sectionKey)
        val importedArchive = archivedTaskDao.getAllWithCompletions().single()
        assertEquals("已归档日常", importedArchive.task.shortText)
        assertEquals(TaskStatus.FAILED, importedArchive.completions.single().status)
        assertArrayEquals(imageBytes, File(imagesDir, "photo.jpg").readBytes())
        assertFalse(File(imagesDir, "old.jpg").exists())
    }
}
