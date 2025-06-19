package com.student.chatify.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.student.chatify.model.Message
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun getChatId(currentUser: String, otherUser: String): String {
        return listOf(currentUser, otherUser).sorted().joinToString("_")
    }

    fun observeMessages(chatId: String, onUpdate: (List<Message>) -> Unit) {
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val messages = snapshot.documents.mapNotNull {
                    it.toObject(Message::class.java)?.copy(id = it.id)
                }
                onUpdate(messages)
            }
    }

    suspend fun sendMessage(chatId: String, message: Message): Boolean {
        return try {
            db.collection("chats").document(chatId)
                .collection("messages")
                .document(message.id)
                .set(message.copy(status = "sent"))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateStatus(chatId: String, messageId: String, status: String) {
        try {
            db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update("status", status)
                .await()
        } catch (_: Exception) {
            // Optionally log
        }
    }

    suspend fun updateChatSummary(
        chatId: String,
        participants: List<String>,
        text: String,
        timestamp: Long
    ) {
        val summary = mapOf(
            "participants" to participants.sorted(),
            "lastMessage" to text,
            "lastMessageTime" to timestamp
        )
        try {
            db.collection("chats").document(chatId)
                .set(summary, SetOptions.merge())
                .await()
        } catch (_: Exception) {
            // Optionally log
        }
    }

    suspend fun startOrCreateChat(
        currentUid: String,
        otherUid: String
    ): String? {
        return try {
            val participants = listOf(currentUid, otherUid).sorted()

            val snapshot = db.collection("chats")
                .whereEqualTo("participants", participants)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                return snapshot.documents.first().id
            }

            val chatData = mapOf(
                "participants" to participants,
                "lastMessage" to "",
                "lastMessageTime" to System.currentTimeMillis(),
                "unreadCounts" to mapOf(currentUid to 0L, otherUid to 0L)
            )

            val newDoc = db.collection("chats").add(chatData).await()
            newDoc.id
        } catch (e: Exception) {
            null // Optional: Log e.message
        }
    }
}
