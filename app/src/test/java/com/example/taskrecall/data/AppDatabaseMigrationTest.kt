package com.example.taskrecall.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.taskrecall.data.db.AppDatabase
import com.example.taskrecall.domain.TaskStatus
import com.example.taskrecall.domain.TaskType
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 升级验证：旧版本已有任务和完成记录，升级到 v2 后必须保留，
 * 同时新增 task_orders 表用于拖拽排序。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {

    @Test
    fun migrate1To2_preservesExistingDataAndAddsOrderTable() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-${UUID.randomUUID()}.db"
        context.deleteDatabase(dbName)

        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        val legacy = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        legacy.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tasks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `type` TEXT NOT NULL,
                `shortText` TEXT NOT NULL,
                `detailMarkdown` TEXT,
                `createdAt` INTEGER NOT NULL,
                `dueAt` INTEGER,
                `startAt` INTEGER,
                `deletedAt` INTEGER
            )
            """.trimIndent(),
        )
        legacy.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_type` ON `tasks` (`type`)")
        legacy.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueAt` ON `tasks` (`dueAt`)")
        legacy.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_deletedAt` ON `tasks` (`deletedAt`)")
        legacy.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `completions` (
                `taskId` INTEGER NOT NULL,
                `periodKey` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                PRIMARY KEY(`taskId`, `periodKey`)
            )
            """.trimIndent(),
        )
        legacy.execSQL(
            "INSERT INTO tasks (id, type, shortText, createdAt, dueAt) VALUES (1, 'SINGLE', '升级前任务', 100, 200)",
        )
        legacy.execSQL(
            "INSERT INTO completions (taskId, periodKey, status) VALUES (1, 'once', 'DONE')",
        )
        legacy.version = 1
        legacy.close()

        val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        val task = upgraded.taskDao().getById(1)
        assertEquals(TaskType.SINGLE, task?.type)
        assertEquals("升级前任务", task?.shortText)
        assertEquals(
            TaskStatus.DONE,
            upgraded.completionDao().get(1, "once")?.status,
        )
        assertTrue(upgraded.taskOrderDao().getAll().isEmpty())
        assertTrue(upgraded.archivedTaskDao().getAll().isEmpty())
        assertTrue(upgraded.archivedCompletionDao().getAll().isEmpty())

        upgraded.close()
        context.deleteDatabase(dbName)
    }
}
