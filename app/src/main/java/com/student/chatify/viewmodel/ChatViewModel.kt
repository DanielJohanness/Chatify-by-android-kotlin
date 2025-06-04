package com.student.chatify.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.student.chatify.model.ChatMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatViewModel : ViewModel() {

    private val _chatMessages = MutableLiveData<List<ChatMessage>>(emptyList())
    val chatMessages: LiveData<List<ChatMessage>> get() = _chatMessages

    private val _typingStatus = MutableLiveData(false)
    val typingStatus: LiveData<Boolean> get() = _typingStatus

    private val db = FirebaseFirestore.getInstance()
    private lateinit var generativeModel: GenerativeModel

    // Fungsi untuk load chat history dengan listener agar tetap up-to-date
    fun loadChatHistory() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w("ChatViewModel", "User ID is null, cannot load chat history")
            _chatMessages.value = emptyList()
            return
        }

        db.collection("chatHistories")
            .document(userId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    Log.e("Firestore", "Failed to load chat", e)
                    _chatMessages.value = emptyList()
                    return@addSnapshotListener
                }

                // Memetakan snapshot ke list ChatMessage dan menyortir berdasarkan timestamp
                val messages = snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                    .sortedBy { it.timestamp }
                _chatMessages.value = messages
            }
    }

    // Fungsi untuk mengirim pesan dari pengguna
    fun sendUserMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(
            message = text,
            user = true,
            timestamp = System.currentTimeMillis()
        )

        val currentMessages = _chatMessages.value ?: emptyList()
        _chatMessages.value = currentMessages + userMessage

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w("ChatViewModel", "User ID is null, cannot send message to Firestore")
            return
        }

        // Simpan pesan pengguna ke Firestore
        db.collection("chatHistories")
            .document(userId)
            .collection("messages")
            .add(userMessage)
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to save user message", e)
            }

        // Kirim pesan ke AI setelah sedikit penundaan
        viewModelScope.launch {
            sendMessageToAI(text)
        }
    }

    // Fungsi untuk mengirim pesan ke AI
    private suspend fun sendMessageToAI(message: String) {
        // Set typingStatus true sebelum mulai
        _typingStatus.postValue(true)

        try {
            // Inisialisasi model generative AI
            generativeModel = Firebase.ai.generativeModel("gemini-1.5-flash")

            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(message)
            }

            val reply = response.text

            // Memastikan jika AI tidak memberikan balasan, maka memberikan fallback message
            val aiMessage = ChatMessage(
                message = if (reply.isNullOrBlank()) "AI tidak memberikan balasan." else reply,
                user = false,
                timestamp = System.currentTimeMillis()
            )

            // Update LiveData chatMessages di main thread
            withContext(Dispatchers.Main) {
                val currentMessages = _chatMessages.value ?: emptyList()
                _chatMessages.value = currentMessages + aiMessage

                // Scroll to the last position after adding the new AI message (if needed)
                // chatRecyclerView.scrollToPosition(chatMessages.size - 1)  // This can be triggered in the Activity
            }

            // Simpan pesan AI ke Firestore
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                db.collection("chatHistories")
                    .document(userId)
                    .collection("messages")
                    .add(aiMessage)
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to save AI message", e)
                    }
            }

        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error in sendMessageToAI", e)
            withContext(Dispatchers.Main) {
                val errorMessage = ChatMessage(
                    message = "Terjadi kesalahan: ${e.localizedMessage}",
                    user = false,
                    timestamp = System.currentTimeMillis()
                )
                val currentMessages = _chatMessages.value ?: emptyList()
                _chatMessages.value = currentMessages + errorMessage
            }
        } finally {
            // Pastikan typingStatus di-set ke false setelah selesai
            _typingStatus.postValue(false)
        }
    }
}
