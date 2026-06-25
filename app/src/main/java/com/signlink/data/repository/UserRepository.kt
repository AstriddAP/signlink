package com.signlink.data.repository

import com.signlink.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUserProfile(uid: String): Result<User?>
    suspend fun saveUserProfile(user: User): Result<Unit>
    suspend fun updateProfileType(uid: String, profileType: String): Result<Unit>
    suspend fun updateFcmToken(uid: String, token: String): Result<Unit>
    suspend fun addContact(currentUserUid: String, contact: User): Result<Unit>
    suspend fun getContacts(currentUserUid: String): Result<List<User>>
    suspend fun deleteContact(currentUserUid: String, contactUid: String): Result<Unit>
    fun getLocalProfileType(): String?
    fun saveLocalProfileType(profileType: String)
    fun clearLocalData()
}
