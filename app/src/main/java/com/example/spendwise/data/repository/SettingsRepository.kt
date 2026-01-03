package com.example.spendwise.data.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val dailyReminderEnabled: Flow<Boolean>
    val dailyReminderTime: Flow<String?>
    val budgetAlertEnabled: Flow<Boolean>
    val biometricLockEnabled: Flow<Boolean>

    suspend fun setDailyReminderEnabled(enabled: Boolean)
    suspend fun setDailyReminderTime(time: String)
    suspend fun setBudgetAlertEnabled(enabled: Boolean)
    suspend fun setBiometricLockEnabled(enabled: Boolean)
}
