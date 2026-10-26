package com.example.taskrecall.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 规格九的两张核心表，加上主页拖拽排序表。
 *
 * version 1 -> 2 只新增 task_orders，不修改任务/完成记录，
 * 因此已有用户数据会完整保留。
 */
@Database(
    entities = [TaskEntity::class, CompletionEntity::class, TaskOrderEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    abstract fun completionDao(): CompletionDao

    abstract fun taskOrderDao(): TaskOrderDao

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
    }
}
