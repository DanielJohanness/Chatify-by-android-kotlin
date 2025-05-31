package com.student.chatify.model

data class LastMessage(
    val userId: String = "",
    val name: String = "",
    val profileUrl: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)
