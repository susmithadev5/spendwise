package com.example.spendwise.auth

import com.example.spendwise.model.AuthError
import com.example.spendwise.model.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(private val firebaseAuth: FirebaseAuth) : AuthRepository {

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success(firebaseAuth.currentUser?.email)
        } catch (e: Exception) {
            AuthResult.Error(mapAuthError(e))
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            AuthResult.Success(firebaseAuth.currentUser?.email)
        } catch (e: Exception) {
            AuthResult.Error(mapAuthError(e))
        }
    }

    override fun currentUser(): FirebaseUser? = firebaseAuth.currentUser

    override fun signOut() {
        firebaseAuth.signOut()
    }

    private fun mapAuthError(exception: Exception): AuthError {
        return when (exception) {
            is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
            is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials
            is FirebaseAuthInvalidUserException -> AuthError.InvalidCredentials
            is FirebaseAuthUserCollisionException -> AuthError.EmailInUse
            is FirebaseNetworkException -> AuthError.Network
            is FirebaseAuthException -> {
                when (exception.errorCode) {
                    "ERROR_USER_NOT_FOUND",
                    "ERROR_WRONG_PASSWORD" -> AuthError.InvalidCredentials

                    "ERROR_INVALID_EMAIL" -> AuthError.InvalidEmailFormat
                    "ERROR_EMAIL_ALREADY_IN_USE" -> AuthError.EmailInUse
                    "ERROR_WEAK_PASSWORD" -> AuthError.WeakPassword
                    "ERROR_NETWORK_REQUEST_FAILED" -> AuthError.Network
                    else -> AuthError.Unknown
                }
            }

            else -> AuthError.Unknown
        }
    }
}
