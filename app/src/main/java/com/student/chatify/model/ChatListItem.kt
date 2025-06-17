package com.student.chatify.model

data class ChatListItem(
    val chatId: String = "",
    val chatName: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0L,
    val isGroup: Boolean = false
)
