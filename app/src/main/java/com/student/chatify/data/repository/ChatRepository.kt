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

    suspend fun sendMessage(chatId: String, message: Message, receiverUid: String): Boolean {
        return try {
            // Simpan pesan
            db.collection("chats").document(chatId)
                .collection("messages")
                .document(message.id)
                .set(message.copy(status = "sent"))
                .await()

            // Perbarui lastMessage, lastMessageTime, dan unreadCounts
            val chatRef = db.collection("chats").document(chatId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(chatRef)
                val currentUnread = snapshot.getLong("unreadCounts.$receiverUid") ?: 0L
                val newUnread = currentUnread + 1

                val updates = mapOf(
                    "lastMessage" to message.text,
                    "lastMessageTime" to message.timestamp,
                    "unreadCounts.$receiverUid" to newUnread
                )

                transaction.update(chatRef, updates)
            }.await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateStatus(chatId: String, messageId: String, status: String) {
        try {
            val ref = db.collection("chats").document(chatId)
                .collection("messages").document(messageId)

            val snapshot = ref.get().await()
            val currentStatus = snapshot.getString("status") ?: "sent"

            val statusPriority = listOf("sending", "sent", "delivered", "read")
            if (statusPriority.indexOf(status) > statusPriority.indexOf(currentStatus)) {
                ref.update("status", status).await()
            }
        } catch (_: Exception) {}
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

    suspend fun startOrCreateChat(currentUid: String, otherUid: String): String? {
        return try {
            val chatId = getChatId(currentUid, otherUid)
            val chatRef = db.collection("chats").document(chatId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(chatRef)
                if (!snapshot.exists()) {
                    val participants = listOf(currentUid, otherUid).sorted()
                    val now = System.currentTimeMillis()
                    val initialData = mapOf(
                        "participants" to participants,
                        "createdAt" to now,
                        "lastMessage" to "",
                        "lastMessageTime" to now,
                        "unreadCounts" to mapOf(currentUid to 0L, otherUid to 0L),
                        "typing" to mapOf<String, Boolean>()
                    )
                    transaction.set(chatRef, initialData)
                }
            }.await()

            chatId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun resetUnreadCount(chatId: String, userUid: String) {
        try {
            db.collection("chats").document(chatId)
                .update("unreadCounts.$userUid", 0)
                .await()
        } catch (e: Exception) {
            e.printStackTrace() // Bisa ditambah log jika mau
        }
    }
}
