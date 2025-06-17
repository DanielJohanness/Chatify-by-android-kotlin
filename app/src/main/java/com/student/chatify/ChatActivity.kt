package com.student.chatify

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.student.chatify.model.Message
import com.student.chatify.recyclerView.MessageAdapter

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: MessageAdapter

    private lateinit var currentUserUid: String
    private lateinit var otherUserUid: String
    private lateinit var chatId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recyclerView = findViewById(R.id.messageRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        currentUserUid = auth.currentUser?.uid ?: return
        otherUserUid = intent.getStringExtra("otherUserUid") ?: return

        chatId = generateChatId(currentUserUid, otherUserUid)

        adapter = MessageAdapter(currentUserUid)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadMessages()

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun generateChatId(uid1: String, uid2: String): String {
        return listOf(uid1, uid2).sorted().joinToString("_")
    }

    private fun loadMessages() {
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val newMessages = snapshot.documents.mapNotNull { doc ->
                    val msg = doc.toObject(Message::class.java)
                    msg?.copy(id = doc.id)
                }

                if (newMessages.isNotEmpty()) {
                    adapter.submitList(newMessages)
                    recyclerView.scrollToPosition(newMessages.size - 1)
                }
            }
    }

    private fun sendMessage() {
        val text = messageEditText.text.toString().trim()
        if (text.isEmpty()) return

        val message = Message(
            senderId = currentUserUid,
            content = text,
            timestamp = System.currentTimeMillis()
        )

        val messageRef = db.collection("chats").document(chatId)
            .collection("messages").document()

        val msgWithId = message.copy(id = messageRef.id)

        messageRef.set(msgWithId).addOnSuccessListener {
            messageEditText.text.clear()

            db.collection("chats").document(chatId).set(
                mapOf(
                    "lastMessage" to text,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "participants" to listOf(currentUserUid, otherUserUid)
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }
    }
}
