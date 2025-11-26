package com.example.spendwise.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spendwise.auth.AuthRepository
import com.example.spendwise.model.AuthError
import com.example.spendwise.model.AuthResult
import com.example.spendwise.model.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        val existingUser = authRepository.currentUser()
        if (existingUser != null) {
            _uiState.update {
                it.copy(
                    currentUserEmail = existingUser.email,
                    isAuthenticated = true
                )
            }
        }
    }

    fun onEmailChange(newEmail: String) {
        _uiState.update { it.copy(email = newEmail) }
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.update { it.copy(password = newPassword) }
    }

    fun onConfirmPasswordChange(newPassword: String) {
        _uiState.update { it.copy(confirmPassword = newPassword) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun login(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val email = _uiState.value.email.trim()
            val password = _uiState.value.password
            val validationError = validateLoginInput(email, password)

            if (validationError != null) {
                _uiState.update { it.copy(errorMessage = validationError.message) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            currentUserEmail = result.userEmail,
                            isAuthenticated = true,
                            password = "",
                            confirmPassword = ""
                        )
                    }
                    onSuccess()
                }

                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message,
                            isAuthenticated = false
                        )
                    }
                }
            }
        }
    }

    fun signup(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val email = _uiState.value.email.trim()
            val password = _uiState.value.password
            val confirm = _uiState.value.confirmPassword
            val validationError = validateSignupInput(email, password, confirm)

            if (validationError != null) {
                _uiState.update { it.copy(errorMessage = validationError.message) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.signUp(email, password)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            currentUserEmail = result.userEmail,
                            isAuthenticated = true,
                            password = "",
                            confirmPassword = ""
                        )
                    }
                    onSuccess()
                }

                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message,
                            isAuthenticated = false
                        )
                    }
                }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit = {}) {
        authRepository.signOut()
        _uiState.value = AuthUiState()
        onLoggedOut()
    }

    private fun validateLoginInput(email: String, password: String): AuthError? {
        if (email.isBlank() || password.isBlank()) return AuthError.EmptyFields
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return AuthError.InvalidEmailFormat
        return null
    }

    private fun validateSignupInput(email: String, password: String, confirm: String): AuthError? {
        if (email.isBlank() || password.isBlank() || confirm.isBlank()) return AuthError.EmptyFields
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return AuthError.InvalidEmailFormat
        if (password.length < 6) return AuthError.WeakPassword
        if (password != confirm) return AuthError.PasswordMismatch
        return null
    }

    companion object {
        fun provideFactory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(authRepository) as T
                }
            }
    }
}
