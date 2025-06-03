package com.student.chatify.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val message: String = "",
    val user: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val dateHeader: Boolean = false,
    val typingStatus: Boolean = false,
    val loading: Boolean = false
) {
    fun timestampFormatted(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun dateFormatted(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
