package com.signlink.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val profileType: String = "", // 'auditivo' | 'habla' | 'ambos'
    val fcmToken: String = "",
    val language: String = "es",
    val photoUrl: String = "",
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val customPhrases: List<CustomPhrase> = emptyList(),
    val createdAt: Timestamp? = null,
    val lastSeen: Timestamp? = null
)

data class EmergencyContact(
    val uid: String = "",
    val name: String = "",
    val phone: String = ""
)

data class CustomPhrase(
    val id: String = "",
    val text: String = "",
    val category: String = ""
)
