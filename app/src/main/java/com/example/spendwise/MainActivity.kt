package com.example.spendwise

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.fragment.app.FragmentActivity
import com.example.spendwise.auth.AuthRepositoryImpl
import com.example.spendwise.biometric.BiometricAuthenticator
import com.example.spendwise.data.repository.SettingsRepositoryImpl
import com.example.spendwise.notification.NotificationHelper
import com.example.spendwise.ui.navigation.SpendWiseNavHost
import com.example.spendwise.ui.theme.SpendWiseTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : FragmentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        NotificationHelper.createNotificationChannel(applicationContext)
        requestNotificationPermissionIfNeeded()

        setContent {
            SpendWiseApp(activity = this)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun SpendWiseApp(activity: FragmentActivity) {
    val authRepository = remember { AuthRepositoryImpl(FirebaseAuth.getInstance()) }
    val settingsRepository = remember { SettingsRepositoryImpl(activity.applicationContext) }
    val biometricAuthenticator = remember { BiometricAuthenticator(activity) }
    var isLocked by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val biometricEnabled by settingsRepository.biometricLockEnabled.collectAsState(initial = false)
    val currentUser by rememberUpdatedState(newValue = FirebaseAuth.getInstance().currentUser)

    SpendWiseTheme {
        LaunchedEffect(biometricEnabled, currentUser) {
            val shouldLock = biometricEnabled &&
                FirebaseAuth.getInstance().currentUser != null &&
                biometricAuthenticator.isSupported()
            isLocked = shouldLock
        }

        DisposableEffect(lifecycleOwner, biometricEnabled) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START &&
                    biometricEnabled &&
                    FirebaseAuth.getInstance().currentUser != null &&
                    biometricAuthenticator.isSupported()
                ) {
                    isLocked = true
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(isLocked) {
            if (isLocked) {
                biometricAuthenticator.authenticate(
                    onSuccess = { isLocked = false },
                    onError = { isLocked = true }
                )
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                SpendWiseNavHost(
                    authRepository = authRepository,
                    settingsRepository = settingsRepository
                )

                if (isLocked) {
                    LockOverlay(onRetry = {
                        biometricAuthenticator.authenticate(
                            onSuccess = { isLocked = false },
                            onError = { isLocked = true }
                        )
                    })
                }
            }
        }
    }
}

@Composable
private fun LockOverlay(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "App locked",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Authenticate with your fingerprint or face to continue.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            Button(onClick = onRetry) {
                Text("Unlock")
            }
        }
    }
}
