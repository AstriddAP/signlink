package com.signlink.data.repository

import com.signlink.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUserProfile(uid: String): Result<User?>
    suspend fun saveUserProfile(user: User): Result<Unit>
    suspend fun updateProfileType(uid: String, profileType: String): Result<Unit>
    fun getLocalProfileType(): String?
    fun saveLocalProfileType(profileType: String)
    fun clearLocalData()
}
