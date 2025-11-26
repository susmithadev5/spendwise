package com.example.spendwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.spendwise.auth.AuthRepositoryImpl
import com.example.spendwise.ui.navigation.SpendWiseNavHost
import com.example.spendwise.ui.theme.SpendWiseTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            SpendWiseApp()
        }
    }
}

@Composable
fun SpendWiseApp() {
    val authRepository = remember { AuthRepositoryImpl(FirebaseAuth.getInstance()) }

    SpendWiseTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SpendWiseNavHost(authRepository = authRepository)
        }
    }
}
