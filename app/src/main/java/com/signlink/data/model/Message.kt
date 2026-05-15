package com.signlink.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val type: String = "text", // 'text' | 'symbol' | 'image' | 'location'
    val symbolId: String? = null,
    val imageUrl: String? = null,
    val location: GeoPoint? = null,
    val isRead: Boolean = false,
    val timestamp: Timestamp? = null
)
