package com.example.spendwise.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spendwise.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DEFAULT_REMINDER_TIME = "20:00"

data class SettingsUiState(
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderTime: String? = null,
    val budgetAlertEnabled: Boolean = false,
    val biometricLockEnabled: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.dailyReminderEnabled,
            settingsRepository.dailyReminderTime,
            settingsRepository.budgetAlertEnabled,
            settingsRepository.biometricLockEnabled
        ) { reminderEnabled, reminderTime, budgetAlert, biometricEnabled ->
            SettingsUiState(
                dailyReminderEnabled = reminderEnabled,
                dailyReminderTime = reminderTime,
                budgetAlertEnabled = budgetAlert,
                biometricLockEnabled = biometricEnabled
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    val uiState: StateFlow<SettingsUiState> = _uiState

    fun onDailyReminderToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDailyReminderEnabled(enabled)
            if (enabled && _uiState.value.dailyReminderTime == null) {
                settingsRepository.setDailyReminderTime(DEFAULT_REMINDER_TIME)
            }
        }
    }

    fun onDailyReminderTimeSelected(time: String) {
        viewModelScope.launch {
            settingsRepository.setDailyReminderTime(time)
        }
    }

    fun onBudgetAlertToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBudgetAlertEnabled(enabled)
        }
    }

    fun onBiometricLockToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricLockEnabled(enabled)
        }
    }

    companion object {
        fun provideFactory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(settingsRepository) as T
                }
            }
    }
}
