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
                close()
            }
        awaitClose()
    }

    override fun loginWithGoogle(idToken: String, isActionLogin: Boolean): Flow<Result<AuthResult>> = callbackFlow {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val authResult = task.result
                    val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false
                    
                    if (isNewUser && isActionLogin) {
                        // Si es nuevo y quería LOGUEARSE, lo rechazamos y borramos
                        authResult.user?.delete()?.addOnCompleteListener {
                            firebaseAuth.signOut()
                            trySend(Result.failure(Exception("USER_NOT_REGISTERED")))
                            close()
                        }
                    } else {
                        // Si ya existía O si quería REGISTRARSE, lo dejamos pasar
                        trySend(Result.success(authResult))
                        close()
                    }
                } else {
                    trySend(Result.failure(task.exception ?: Exception("Google login failed")))
                    close()
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
                close() // Cerrar el flujo después de una respuesta
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
