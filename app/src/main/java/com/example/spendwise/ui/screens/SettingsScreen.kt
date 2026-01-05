package com.example.spendwise.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.spendwise.notification.ReminderScheduler
import com.example.spendwise.notification.NotificationHelper
import com.example.spendwise.viewmodel.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val appContext = context.applicationContext
    val snackbarHostState = remember { SnackbarHostState() }
    var showRestoreConfirmation by remember { mutableStateOf(false) }

    val reminderTime = uiState.dailyReminderTime ?: "20:00"
    val (reminderHour, reminderMinute) = parseTime(reminderTime)

    val permissionLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                viewModel.onDailyReminderToggle(true)
            } else {
                Toast.makeText(
                    context,
                    "Notification permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(appContext)
    }

    LaunchedEffect(uiState.dailyReminderEnabled, reminderTime) {
        val permissionGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        if (uiState.dailyReminderEnabled && permissionGranted) {
            ReminderScheduler.scheduleDailyReminder(
                context = appContext,
                hour = reminderHour,
                minute = reminderMinute
            )
        } else {
            ReminderScheduler.cancelDailyReminder(appContext)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Daily reminder",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            SettingsToggleRow(
                title = "Daily reminder to log expenses",
                description = "Get a reminder at your chosen time each day.",
                checked = uiState.dailyReminderEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        val permissionGranted = Build.VERSION.SDK_INT < 33 ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                        if (permissionGranted) {
                            viewModel.onDailyReminderToggle(true)
                        } else {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        viewModel.onDailyReminderToggle(false)
                    }
                }
            )

            ReminderTimeRow(
                enabled = uiState.dailyReminderEnabled,
                time = reminderTime,
                onClick = {
                    val timePickerDialog = TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val formatted = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                            viewModel.onDailyReminderTimeSelected(formatted)
                        },
                        reminderHour,
                        reminderMinute,
                        true
                    )
                    timePickerDialog.show()
                }
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Budget alert",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            SettingsToggleRow(
                title = "Alert when monthly spending reaches 75% of budget",
                description = "Receive a notification as you approach your limit.",
                checked = uiState.budgetAlertEnabled,
                onCheckedChange = viewModel::onBudgetAlertToggle
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Cloud backup",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            if (uiState.isUserAuthenticated) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.onBackupClicked() },
                            enabled = !uiState.isBackingUp && !uiState.isRestoring
                        ) {
                            if (uiState.isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                            Text("Backup now")
                        }
                        OutlinedButton(
                            onClick = { showRestoreConfirmation = true },
                            enabled = !uiState.isRestoring && !uiState.isBackingUp
                        ) {
                            if (uiState.isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                            Text("Restore from cloud")
                        }
                    }
                    if (uiState.lastBackupTime != null) {
                        Text(
                            text = "Last backup: ${uiState.lastBackupTime}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (uiState.isBackingUp || uiState.isRestoring) {
                        Text(
                            text = if (uiState.isBackingUp) {
                                "Backing up to the cloud..."
                            } else {
                                "Restoring from the cloud..."
                            },
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Text(
                    text = "Sign in to enable cloud backup and restore.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            SecuritySection(
                biometricEnabled = uiState.biometricLockEnabled,
                onToggle = viewModel::onBiometricLockToggle
            )
        }
    }

    if (showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = false },
            title = { Text(text = "Restore from cloud") },
            text = { Text("This will replace your local data with the cloud backup. Continue?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmation = false
                        viewModel.onRestoreClicked()
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ReminderTimeRow(
    enabled: Boolean,
    time: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Reminder time",
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Currently $time",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = time,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun SecuritySection(
    biometricEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val biometricManager = androidx.biometric.BiometricManager.from(context)
    val isSupported = biometricManager.canAuthenticate(
        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
    ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Security",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
        SettingsToggleRow(
            title = "Lock app with fingerprint/face ID",
            description = if (isSupported) {
                "Require biometrics when opening the app."
            } else {
                "Biometric unlock is not supported on this device."
            },
            checked = biometricEnabled && isSupported,
            onCheckedChange = { enabled ->
                if (isSupported) {
                    onToggle(enabled)
                } else {
                    Toast.makeText(
                        context,
                        "Biometric not supported on this device",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

private fun parseTime(time: String): Pair<Int, Int> {
    return try {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        hour to minute
    } catch (e: Exception) {
        20 to 0
    }
}
