package com.example.spendwise.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val SETTINGS_DATASTORE = "spendwise_settings"
private val Context.settingsDataStore by preferencesDataStore(name = SETTINGS_DATASTORE)

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    private object Keys {
        val DAILY_REMINDER_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_TIME: Preferences.Key<String> =
            stringPreferencesKey("daily_reminder_time")
        val BUDGET_ALERT_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("budget_alert_enabled")
        val BIOMETRIC_LOCK_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("biometric_lock_enabled")
    }

    override val dailyReminderEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs ->
            prefs[Keys.DAILY_REMINDER_ENABLED] ?: false
        }

    override val dailyReminderTime: Flow<String?> =
        context.settingsDataStore.data.map { prefs ->
            prefs[Keys.DAILY_REMINDER_TIME]
        }

    override val budgetAlertEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs ->
            prefs[Keys.BUDGET_ALERT_ENABLED] ?: false
        }

    override val biometricLockEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs ->
            prefs[Keys.BIOMETRIC_LOCK_ENABLED] ?: false
        }

    override suspend fun setDailyReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DAILY_REMINDER_ENABLED] = enabled
        }
    }

    override suspend fun setDailyReminderTime(time: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DAILY_REMINDER_TIME] = time
        }
    }

    override suspend fun setBudgetAlertEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.BUDGET_ALERT_ENABLED] = enabled
        }
    }

    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }
}
