package com.signlink.data.model

data class RecentChat(
    val contact: User,
    val lastMessage: Message?,
    val unreadCount: Int
)
