package com.student.chatify.model

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val profileImage: String = "",
    val statusMessage: String = "",
    val isOnline: Boolean = true,
    val lastSeen: Long = 0L,
    val createdAt: Long = 0L
)
