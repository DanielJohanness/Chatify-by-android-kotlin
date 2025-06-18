package com.student.chatify.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: String = "text",
    val timestamp: Long = 0L,
    val status: String = "sent"
)