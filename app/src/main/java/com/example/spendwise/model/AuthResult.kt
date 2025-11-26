package com.example.spendwise.model

sealed class AuthResult {
    data class Success(val userEmail: String?) : AuthResult()
    data class Error(val error: AuthError) : AuthResult()
}

sealed class AuthError(open val message: String) {
    object InvalidCredentials : AuthError("Incorrect email or password.")
    object Network : AuthError("Network error. Please check your connection and try again.")
    object EmailInUse : AuthError("Email already in use.")
    object WeakPassword : AuthError("Password should be at least 6 characters.")
    object InvalidEmailFormat : AuthError("Please enter a valid email address.")
    object PasswordMismatch : AuthError("Passwords do not match.")
    object EmptyFields : AuthError("All fields must be filled in.")
    object Unknown : AuthError("Authentication failed. Please try again.")
}
