package com.example.spendwise.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.spendwise.auth.AuthRepository

class SplashViewModel(private val authRepository: AuthRepository) : ViewModel() {

    fun isUserLoggedIn(): Boolean = authRepository.currentUser() != null

    companion object {
        fun provideFactory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SplashViewModel(authRepository) as T
                }
            }
    }
}
