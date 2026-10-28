package com.example.taskrecall.di

import android.content.Context
import androidx.room.Room
import com.example.taskrecall.data.db.AppDatabase
import com.example.taskrecall.data.db.ArchivedCompletionDao
import com.example.taskrecall.data.db.ArchivedTaskDao
import com.example.taskrecall.data.db.CompletionDao
import com.example.taskrecall.data.db.TaskDao
import com.example.taskrecall.data.db.TaskOrderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 模块：数据库与 DAO。
 *
 * Repository、ExportManager、ImportManager、ImageStorage 都使用
 * @Inject 构造函数，由 Hilt 自动创建。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.NAME,
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideCompletionDao(database: AppDatabase): CompletionDao = database.completionDao()

    @Provides
    fun provideTaskOrderDao(database: AppDatabase): TaskOrderDao = database.taskOrderDao()

    @Provides
    fun provideArchivedTaskDao(database: AppDatabase): ArchivedTaskDao = database.archivedTaskDao()

    @Provides
    fun provideArchivedCompletionDao(database: AppDatabase): ArchivedCompletionDao =
        database.archivedCompletionDao()
}
