// file: ChatViewModel.kt
package com.student.chatify.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.student.chatify.model.ChatItem
import com.student.chatify.model.MessageStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class ChatViewModel : ViewModel() {

    private val _chatItems = MutableLiveData<List<ChatItem>?>(emptyList())
    val chatItems: MutableLiveData<List<ChatItem>?> = _chatItems

    private val _typingStatus = MutableLiveData(false)
    val typingStatus: LiveData<Boolean> = _typingStatus

    private val db = FirebaseFirestore.getInstance()

    // Inisialisasi generativeModel hanya sekali
    private val generativeModel: GenerativeModel = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel("gemini-1.5-flash")
    fun loadChatHistory() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrBlank()) {
            Log.w("ChatViewModel", "User belum login, tidak memuat history")
            _chatItems.value = emptyList()
            return
        }

        db.collection("chatHistories")
            .document(userId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.e("ChatViewModel", "Error load chat: ", error)
                    _chatItems.value = emptyList()
                    return@addSnapshotListener
                }

                val items = mutableListOf<ChatItem>()
                var lastDateKey: String? = null

                for (doc in snapshot.documents) {
                    val text = doc.getString("text") ?: continue
                    val isUser = doc.getBoolean("isUser") ?: false
                    val timestamp = doc.getLong("timestamp") ?: continue
                    val statusStr = doc.getString("status") ?: "SENT"
                    val status = try {
                        MessageStatus.valueOf(statusStr)
                    } catch (_: Exception) {
                        MessageStatus.SENT
                    }

                    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                    val dayKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
                    if (dayKey != lastDateKey) {
                        items.add(ChatItem.DateHeader(timestamp))
                        lastDateKey = dayKey
                    }

                    items.add(
                        ChatItem.Message(
                            id = doc.id,
                            text = text,
                            isUser = isUser,
                            timestamp = timestamp,
                            status = status
                        )
                    )
                }
                _chatItems.value = items
            }
    }

    fun sendUserMessage(text: String) {
        if (text.isBlank()) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrBlank()) {
            Log.w("ChatViewModel", "User ID null; tidak dapat kirim pesan")
            return
        }

        val localId = UUID.randomUUID().toString()
        val newMsg = ChatItem.Message(
            id = localId,
            text = text,
            isUser = true,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING
        )

        val currentList = _chatItems.value?.toMutableList() ?: mutableListOf()
        val lastTs = (currentList.lastOrNull() as? ChatItem.Message)?.timestamp
        if (lastTs == null || !isSameDay(lastTs, newMsg.timestamp)) {
            currentList.add(ChatItem.DateHeader(newMsg.timestamp))
        }
        currentList.add(newMsg)
        _chatItems.value = currentList

        val dataMap = mapOf(
            "text" to text,
            "isUser" to true,
            "timestamp" to newMsg.timestamp,
            "status" to MessageStatus.SENDING.name
        )
        db.collection("chatHistories")
            .document(userId)
            .collection("messages")
            .add(dataMap)
            .addOnSuccessListener { docRef ->
                docRef.update("status", MessageStatus.SENT.name)
                updateLocalMessageStatus(localId, MessageStatus.SENT, docRef.id)

                // Call AI model to generate a response
                viewModelScope.launch {
                    val aiResponse = generateText("Ini adalah respons dummy untuk: \"$text\"")
                    sendAIResponse(aiResponse)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Gagal simpan user message", e)
                updateLocalMessageStatus(localId, MessageStatus.FAILED, "")
            }
    }

    private fun updateLocalMessageStatus(localId: String, newStatus: MessageStatus, newFirestoreId: String) {
        val updated = _chatItems.value?.map { item ->
            if (item is ChatItem.Message && item.id == localId) {
                item.copy(
                    id = newFirestoreId.ifBlank { localId },
                    status = newStatus
                )
            } else {
                item
            }
        }
        _chatItems.postValue(updated)
    }

    private suspend fun generateText(prompt: String): String {
        try {
            // Generate text using Firebase AI (Gemini model)
            val response = generativeModel.generateContent(prompt)

            // Extract the generated text
            return response.text ?: "Tidak ada hasil teks yang ditemukan."
        } catch (e: Exception) {
            // Handle the error
            Log.e("ChatViewModel", "Error generating text: ${e.message}")
            return "Terjadi kesalahan: ${e.message}"
        }
    }

    private suspend fun sendAIResponse(responseText: String) {
        withContext(Dispatchers.Main) {
            _typingStatus.value = true

            val currentList = _chatItems.value?.toMutableList() ?: mutableListOf()
            val aiTimestamp = System.currentTimeMillis()

            val lastTs = (currentList.lastOrNull() as? ChatItem.Message)?.timestamp
            if (lastTs == null || !isSameDay(lastTs, aiTimestamp)) {
                currentList.add(ChatItem.DateHeader(aiTimestamp))
            }

            currentList.add(
                ChatItem.Message(
                    id = UUID.randomUUID().toString(),
                    text = responseText,
                    isUser = false,
                    timestamp = aiTimestamp,
                    status = MessageStatus.SENT
                )
            )
            _chatItems.value = currentList

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (!userId.isNullOrBlank()) {
                val data = mapOf(
                    "text" to responseText,
                    "isUser" to false,
                    "timestamp" to aiTimestamp,
                    "status" to MessageStatus.SENT.name
                )
                db.collection("chatHistories")
                    .document(userId)
                    .collection("messages")
                    .add(data)
            }

            _typingStatus.value = false
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}
