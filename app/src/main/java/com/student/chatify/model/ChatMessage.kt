package com.student.chatify.model

data class ChatMessage(
    val message: String = "",
    val user: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)