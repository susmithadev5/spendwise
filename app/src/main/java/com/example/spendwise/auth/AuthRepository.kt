package com.example.spendwise.auth

import com.example.spendwise.model.AuthResult
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
    fun currentUser(): FirebaseUser?
    fun signOut()
}
