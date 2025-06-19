package com.student.chatify.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.student.chatify.model.Message
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
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
            // Buat ID chat unik berdasarkan kombinasi UID (misal sorted dan digabung dengan "_")
            val chatId = getChatId(currentUid, otherUid)
            val docRef = db.collection("chats").document(chatId)

            // Ambil dokumen untuk cek apakah sudah ada
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                val participants = listOf(currentUid, otherUid).sorted()

                val chatData = mapOf(
                    "participants" to participants,
                    "createdAt" to System.currentTimeMillis(),
                    "lastMessage" to "",
                    "lastMessageTime" to 0L, // Awalnya belum ada pesan
                    "unreadCounts" to mapOf(
                        currentUid to 0L,
                        otherUid to 0L
                    )
                )

                // Set data awal chat
                docRef.set(chatData).await()
            }

            chatId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
