package com.example.taskrecall.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "taskrecall_settings",
)

/**
 * 规格五 / 十三：近一周区的显示开关需要持久化。
 *
 * 默认显示。关闭后主页不显示近一周区；重新打开后主页以可折叠形式显示。
 * 折叠状态本身不需要持久化，属于当前页面状态。
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val showNextWeekKey = booleanPreferencesKey("show_next_week")

    val showNextWeek: Flow<Boolean> = context.settingsDataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> preferences[showNextWeekKey] ?: true }

    suspend fun setShowNextWeek(show: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[showNextWeekKey] = show
        }
    }
}
