package com.demo.smartnote.reminder

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.reminderDataStore by preferencesDataStore(name = "reminder")

/** 提醒设置：是否开启 + 每天几点几分 */
data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0
) {
    val timeText: String get() = String.format("%02d:%02d", hour, minute)
}

/**
 * 用 DataStore 存提醒开关和时间。
 * 存文件而不是 Room，是因为它只有一行配置，读写频繁且不需要查询。
 */
class ReminderStore(private val context: Context) {

    private val keyEnabled = booleanPreferencesKey("enabled")
    private val keyHour = intPreferencesKey("hour")
    private val keyMinute = intPreferencesKey("minute")

    val settings: Flow<ReminderSettings> = context.reminderDataStore.data.map { prefs ->
        ReminderSettings(
            enabled = prefs[keyEnabled] ?: false,
            hour = prefs[keyHour] ?: 20,
            minute = prefs[keyMinute] ?: 0
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.reminderDataStore.edit { it[keyEnabled] = enabled }
    }

    suspend fun setTime(hour: Int, minute: Int) {
        context.reminderDataStore.edit {
            it[keyHour] = hour
            it[keyMinute] = minute
        }
    }
}
