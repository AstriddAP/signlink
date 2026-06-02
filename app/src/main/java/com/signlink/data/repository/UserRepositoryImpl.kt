package com.signlink.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.signlink.data.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : UserRepository {

    private val sharedPrefs = context.getSharedPreferences("signlink_prefs", Context.MODE_PRIVATE)

    override suspend fun getUserProfile(uid: String): Result<User?> {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            firestore.collection("users").document(user.uid).set(user).await()
            saveLocalProfileType(user.profileType)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfileType(uid: String, profileType: String): Result<Unit> {
        return try {
            firestore.collection("users").document(uid).update("profileType", profileType).await()
            saveLocalProfileType(profileType)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getLocalProfileType(): String? {
        return sharedPrefs.getString("profile_type", null)
    }

    override fun saveLocalProfileType(profileType: String) {
        sharedPrefs.edit().putString("profile_type", profileType).apply()
    }

    override fun clearLocalData() {
        sharedPrefs.edit().clear().apply()
    }
}
