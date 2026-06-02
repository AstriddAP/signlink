package com.signlink.data.repository

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: FirebaseUser?
    fun login(email: String, pass: String): Flow<Result<AuthResult>>
    fun loginWithGoogle(idToken: String, isActionLogin: Boolean): Flow<Result<AuthResult>>
    fun register(email: String, pass: String): Flow<Result<AuthResult>>
    fun logout()
    fun isUserLoggedIn(): Boolean
}
