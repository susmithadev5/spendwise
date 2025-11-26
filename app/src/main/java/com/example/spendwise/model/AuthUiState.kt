package com.example.spendwise.model

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUserEmail: String? = null,
    val isAuthenticated: Boolean = false
)
