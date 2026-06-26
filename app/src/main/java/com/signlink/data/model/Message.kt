package com.signlink.data.model

import com.google.firebase.Timestamp

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val seen: Boolean = false
)
