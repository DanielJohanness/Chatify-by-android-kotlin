package com.student.chatify.model

import java.util.UUID

sealed class ChatItem {
    data class Message(
        val id: String = UUID.randomUUID().toString(),
        val text: String,
        val isUser: Boolean,
        val timestamp: Long,
        val status: MessageStatus = MessageStatus.SENDING
    ) : ChatItem()

    data class DateHeader(val date: Long) : ChatItem()
}
