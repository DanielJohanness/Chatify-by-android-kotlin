package com.student.chatify

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.student.chatify.model.Message
import com.student.chatify.recyclerView.MessageAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var adapter: MessageAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var currentUserUid: String
    private lateinit var otherUserUid: String
    private lateinit var chatId: String
    private lateinit var generativeModel: GenerativeModel

    private var isUserAtBottom = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.0-flash")

        recyclerView = findViewById(R.id.messageRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        currentUserUid = auth.currentUser?.uid ?: return
        otherUserUid = intent.getStringExtra("otherUserUid") ?: return
        chatId = listOf(currentUserUid, otherUserUid).sorted().joinToString("_")

        adapter = MessageAdapter(currentUserUid, this)
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerView.adapter = adapter

        setupAutoScroll()
        loadMessages()
        setupTypingIndicator()

        sendButton.setOnClickListener {
            val text = messageEditText.text.toString().trim()
            if (text.isNotEmpty()) sendMessage(text)
        }
    }

    private fun setupAutoScroll() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = rv.layoutManager as LinearLayoutManager
                isUserAtBottom = layoutManager.findLastCompletelyVisibleItemPosition() >= adapter.itemCount - 1
            }
        })
    }

    private fun loadMessages() {
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val items = mutableListOf<MessageAdapter.MessageItem>()
                var lastDateKey: String? = null

                for (doc in snapshot.documents) {
                    val msg = doc.toObject(Message::class.java)?.copy(id = doc.id) ?: continue
                    val cal = Calendar.getInstance().apply { timeInMillis = msg.timestamp }
                    val dateKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"

                    if (dateKey != lastDateKey) {
                        items.add(MessageAdapter.MessageItem.DateHeader(msg.timestamp))
                        lastDateKey = dateKey
                    }

                    items.add(MessageAdapter.MessageItem.MessageData(msg))

                    // 🔁 Update status jika pesan dari user lain
                    if (msg.senderId != currentUserUid) {
                        val messageRef = db.collection("chats").document(chatId)
                            .collection("messages").document(msg.id)

                        // Jika status masih "sent", ubah jadi "delivered"
                        if (msg.status == "sent") {
                            messageRef.update("status", "delivered")
                        }

                        // Kalau chat sedang aktif (dibuka), anggap pesan sudah dibaca
                        if (msg.status != "read") {
                            messageRef.update("status", "read")
                        }
                    }
                }

                adapter.submitList(items) {
                    if (isUserAtBottom) {
                        recyclerView.scrollToPosition(adapter.itemCount - 1)

                        // ✅ Deteksi pesan terakhir dari user lain, lalu update ke 'read'
                        val lastMessageFromOther = items.asReversed().firstOrNull {
                            it is MessageAdapter.MessageItem.MessageData &&
                                    it.message.senderId != currentUserUid &&
                                    it.message.status != "read"
                        } as? MessageAdapter.MessageItem.MessageData

                        lastMessageFromOther?.let {
                            val messageRef = db.collection("chats").document(chatId)
                                .collection("messages").document(it.message.id)
                            messageRef.update("status", "read")
                        }
                    }
                }
            }
    }

    private fun setupTypingIndicator() {
        messageEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) adapter.updateTypingStatus(true)
            else adapter.updateTypingStatus(false)
        }
    }

    private fun sendMessage(text: String) {
        val timestamp = System.currentTimeMillis()
        val messageRef = db.collection("chats").document(chatId)
            .collection("messages").document()

        val message = Message(
            id = messageRef.id,
            senderId = currentUserUid,
            text = text,
            type = "text",
            timestamp = timestamp,
            status = "sending"
        )

        adapter.addTemporaryMessage(message)
        messageEditText.text.clear()
        recyclerView.scrollToPosition(adapter.itemCount - 1)

        messageRef.set(message.copy(status = "sent")).addOnSuccessListener {
            updateChatSummary(text, timestamp)
            if (text.contains("@chatify", ignoreCase = true)) {
                adapter.updateTypingStatus(true)
                lifecycleScope.launch {
                    val reply = generateAiReply(text)
                    adapter.updateTypingStatus(false)
                    sendAiMessage(reply)
                }
            }
        }.addOnFailureListener {
            adapter.updateMessageStatus(message.id, "failed")
        }
    }

    private fun sendAiMessage(text: String) {
        val timestamp = System.currentTimeMillis()
        val aiRef = db.collection("chats").document(chatId)
            .collection("messages").document()

        val message = Message(
            id = aiRef.id,
            senderId = "chatify",
            text = text,
            type = "text",
            timestamp = timestamp,
            status = "sent"
        )

        aiRef.set(message).addOnSuccessListener {
            adapter.addTemporaryMessage(message)
            recyclerView.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun updateChatSummary(text: String, timestamp: Long) {
        val summary = mapOf(
            "participants" to listOf(currentUserUid, otherUserUid),
            "lastMessage" to text,
            "lastMessageTime" to timestamp
        )
        db.collection("chats").document(chatId).set(summary, SetOptions.merge())
    }

    private suspend fun generateAiReply(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                generativeModel.generateContent(prompt).text ?: "Maaf, saya tidak bisa menjawab saat ini."
            } catch (e: Exception) {
                "Terjadi kesalahan: ${e.message}"
            }
        }
    }
}
