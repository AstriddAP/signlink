package com.signlink.data.repository

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override fun login(email: String, pass: String): Flow<Result<AuthResult>> = callbackFlow {
        firebaseAuth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(Result.success(task.result))
                } else {
                    trySend(Result.failure(task.exception ?: Exception("Login failed")))
                }
            }
        awaitClose()
    }

    override fun register(email: String, pass: String): Flow<Result<AuthResult>> = callbackFlow {
        firebaseAuth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(Result.success(task.result))
                } else {
                    trySend(Result.failure(task.exception ?: Exception("Registration failed")))
                }
            }
        awaitClose()
    }

    override fun logout() {
        firebaseAuth.signOut()
    }

    override fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}
