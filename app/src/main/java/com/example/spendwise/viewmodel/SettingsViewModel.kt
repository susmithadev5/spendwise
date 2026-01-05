package com.example.spendwise.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spendwise.data.repository.CloudSyncRepository
import com.example.spendwise.data.repository.CloudSyncResult
import com.example.spendwise.data.repository.SettingsRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_REMINDER_TIME = "20:00"

data class SettingsUiState(
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderTime: String? = null,
    val budgetAlertEnabled: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val lastBackupTime: String? = null,
    val message: String? = null,
    val isUserAuthenticated: Boolean = false
)

private data class CloudState(
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val lastBackupTime: String? = null,
    val message: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val cloudState = MutableStateFlow(CloudState())
    private val isUserAuthenticated = MutableStateFlow(firebaseAuth.currentUser != null)

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        isUserAuthenticated.value = auth.currentUser != null
    }

    private val baseSettingsState =
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
        }

    private val _uiState: StateFlow<SettingsUiState> =
        combine(
            baseSettingsState,
            cloudState,
            isUserAuthenticated
        ) { base, cloud, authenticated ->
            base.copy(
                isBackingUp = cloud.isBackingUp,
                isRestoring = cloud.isRestoring,
                lastBackupTime = cloud.lastBackupTime,
                message = cloud.message,
                isUserAuthenticated = authenticated
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        firebaseAuth.addAuthStateListener(authListener)
    }

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

    fun onBackupClicked() {
        viewModelScope.launch {
            if (!isUserAuthenticated.value) {
                cloudState.update { it.copy(message = "Please sign in to back up data") }
                return@launch
            }
            cloudState.update { it.copy(isBackingUp = true, message = null) }
            when (val result = cloudSyncRepository.backupAllData()) {
                is CloudSyncResult.Success -> {
                    val timestamp = SimpleDateFormat(
                        "dd MMM yyyy, HH:mm",
                        Locale.getDefault()
                    ).format(System.currentTimeMillis())
                    cloudState.update {
                        it.copy(
                            isBackingUp = false,
                            lastBackupTime = timestamp,
                            message = "Backup successful"
                        )
                    }
                }

                is CloudSyncResult.Error -> {
                    cloudState.update {
                        it.copy(
                            isBackingUp = false,
                            message = result.message
                        )
                    }
                }
            }
        }
    }

    fun onRestoreClicked() {
        viewModelScope.launch {
            if (!isUserAuthenticated.value) {
                cloudState.update { it.copy(message = "Please sign in to restore data") }
                return@launch
            }
            cloudState.update { it.copy(isRestoring = true, message = null) }
            when (val result = cloudSyncRepository.restoreAllData(overwriteLocal = true)) {
                is CloudSyncResult.Success -> {
                    cloudState.update {
                        it.copy(
                            isRestoring = false,
                            message = "Restore successful"
                        )
                    }
                }

                is CloudSyncResult.Error -> {
                    cloudState.update {
                        it.copy(
                            isRestoring = false,
                            message = result.message
                        )
                    }
                }
            }
        }
    }

    fun clearMessage() {
        cloudState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        firebaseAuth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            cloudSyncRepository: CloudSyncRepository,
            firebaseAuth: FirebaseAuth
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        settingsRepository,
                        cloudSyncRepository,
                        firebaseAuth
                    ) as T
                }
            }
    }
}
