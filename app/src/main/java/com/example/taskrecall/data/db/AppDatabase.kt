package com.example.taskrecall.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 核心任务表、完成记录表、拖拽排序表，以及真删除日常/周常时的归档表。
 *
 * version 1 -> 2：新增 task_orders；
 * version 2 -> 3：新增 archived_tasks / archived_completions。
 * 两次迁移都不修改已有任务数据。
 */
@Database(
    entities = [
        TaskEntity::class,
        CompletionEntity::class,
        TaskOrderEntity::class,
        ArchivedTaskEntity::class,
        ArchivedCompletionEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    abstract fun completionDao(): CompletionDao

    abstract fun taskOrderDao(): TaskOrderDao

    abstract fun archivedTaskDao(): ArchivedTaskDao

    abstract fun archivedCompletionDao(): ArchivedCompletionDao

    companion object {
        const val NAME = "daydaytask.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_orders` (
                        `sectionKey` TEXT NOT NULL,
                        `taskId` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`sectionKey`, `taskId`)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `archived_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sourceTaskId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `shortText` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `rangeStart` INTEGER NOT NULL,
                        `rangeEnd` INTEGER NOT NULL,
                        `archivedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `archived_completions` (
                        `archiveTaskId` INTEGER NOT NULL,
                        `periodKey` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        PRIMARY KEY(`archiveTaskId`, `periodKey`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
