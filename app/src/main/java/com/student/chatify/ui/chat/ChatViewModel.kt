package com.student.chatify.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.student.chatify.data.repository.ChatRepository
import com.student.chatify.model.Message
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages

    fun loadMessages(chatId: String) {
        repository.observeMessages(chatId) { msgs ->
            _messages.postValue(msgs)
        }
    }

    fun sendMessage(chatId: String, message: Message) = viewModelScope.launch {
        repository.sendMessage(chatId, message)
    }

    fun updateMessageStatus(chatId: String, messageId: String, status: String) = viewModelScope.launch {
        repository.updateStatus(chatId, messageId, status)
    }

    fun updateChatSummary(chatId: String, participants: List<String>, text: String, timestamp: Long) = viewModelScope.launch {
        repository.updateChatSummary(chatId, participants, text, timestamp)
    }
}
