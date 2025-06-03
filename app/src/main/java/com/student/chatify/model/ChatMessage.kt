package com.student.chatify.model

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isDateHeader: Boolean = false
)
