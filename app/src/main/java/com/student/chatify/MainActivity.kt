package com.student.chatify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.student.chatify.recyclerView.ChatAdapter
import com.student.chatify.model.ChatMessage
import kotlinx.coroutines.*
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button

    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    private lateinit var generativeModel: GenerativeModel

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "defaultUser"

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_profile -> {
                // Intent ke ProfileActivity (buat sendiri)
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        // Inisialisasi Firebase AI generative model
        generativeModel = Firebase.ai.generativeModel("gemini-1.5-flash")

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        messageEditText.requestFocus()
        sendButton = findViewById(R.id.sendButton)

        chatAdapter = ChatAdapter(chatMessages, this)
        loadChatHistory()
        chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true // supaya scroll ke bawah otomatis
            }
        }

        sendButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                sendUserMessage(message)
                messageEditText.text.clear()
            }
        }

        // Optional: kirim pesan saat tekan tombol enter di keyboard
        messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun sendUserMessage(message: String) {
        // Tambah pesan user ke list
        addMessage(ChatMessage(message, user = true))

        // Cek apakah sudah ada status "AI sedang mengetik...", jika ada, hapus
        val typingMessageIndex = chatMessages.indexOfFirst { it.typingStatus }
        if (typingMessageIndex != -1) {
            chatMessages.removeAt(typingMessageIndex)
            chatAdapter.notifyItemRemoved(typingMessageIndex)
        }

        // Tambahkan indikator bahwa AI sedang membalas
        val typingMessage = ChatMessage("AI sedang mengetik...", user = false, dateHeader = false, typingStatus = true)
        chatMessages.add(typingMessage)
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        chatRecyclerView.scrollToPosition(chatMessages.size - 1)

        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(message)
                }

                // Hapus status "AI sedang mengetik..." setelah AI selesai memberikan balasan
                removeTypingMessage() // Hapus status "AI sedang mengetik..."

                val aiReply = response.text
                if (aiReply.isNullOrBlank()) {
                    addMessage(ChatMessage("AI tidak memberikan balasan.", user = false))
                    chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                } else {
                    addMessage(ChatMessage(aiReply, user = false))
                    chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                }

            } catch (e: Exception) {
                removeTypingMessage()  // Hapus status "AI sedang mengetik..." jika terjadi error
                Log.e("ChatAI", "Gagal generate konten AI", e)
                addMessage(ChatMessage("Terjadi kesalahan: ${e.localizedMessage}", user = false))
                chatRecyclerView.scrollToPosition(chatMessages.size - 1)
            }
        }
    }

    private fun removeTypingMessage() {
        // Cari dan hapus status "AI sedang mengetik..." di dalam daftar
        val typingMessageIndex = chatMessages.indexOfFirst { it.typingStatus }
        if (typingMessageIndex != -1) {
            chatMessages.removeAt(typingMessageIndex)
            chatAdapter.notifyItemRemoved(typingMessageIndex)
        }
    }

//    private fun removeLastMessage() {
//        if (chatMessages.isNotEmpty()) {
//            chatMessages.removeAt(chatMessages.lastIndex) // GANTI DENGAN removeAt(lastIndex)
//            chatAdapter.notifyItemRemoved(chatMessages.size)
//            chatRecyclerView.scrollToPosition(chatMessages.size)
//        }
//    }

    private fun addMessage(chatMessage: ChatMessage) {
        // Cek apakah perlu tambah header tanggal
        val lastMessage = chatMessages.lastOrNull()
        if (lastMessage == null || !isSameDay(lastMessage.timestamp, chatMessage.timestamp)) {
            // Tambah header tanggal dulu
            chatMessages.add(ChatMessage(
                message = "", // kosong, isi hanya di adapter pake tanggal
                user = false,
                timestamp = chatMessage.timestamp,
                dateHeader = true
            ))
        }

        // Cek jika ini status "AI sedang mengetik..." dan pastikan tidak ada duplikasi
        if (chatMessage.typingStatus) {
            val existingTypingMessageIndex = chatMessages.indexOfFirst { it.typingStatus }
            if (existingTypingMessageIndex != -1) {
                return // Jangan menambahkan lagi jika sudah ada
            }
        }

        chatMessages.add(chatMessage)
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        chatRecyclerView.scrollToPosition(chatMessages.size - 1)
        saveMessageToFirestore(chatMessage)
    }

    private fun saveMessageToFirestore(chatMessage: ChatMessage) {
        db.collection("chatHistories")
            .document(userId)
            .collection("messages")
            .add(chatMessage)
            .addOnSuccessListener { Log.d("Firestore", "Message saved") }
            .addOnFailureListener { e -> Log.e("Firestore", "Error saving message", e) }
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // pastikan coroutine dihentikan saat Activity dihancurkan
    }

    private fun loadChatHistory() {
        // Menambahkan status "loading"
        val loadingMessage = ChatMessage(message = "", user = false, dateHeader = false, typingStatus = false, loading = true)
        chatMessages.add(loadingMessage)
        chatAdapter.notifyItemInserted(chatMessages.size - 1)

        // Memuat chat history dari Firestore
        db.collection("chatHistories")
            .document(userId)
            .collection("messages")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { snapshot ->
                chatMessages.clear() // Clear loading state
                for (doc in snapshot.documents) {
                    val msg = doc.toObject(ChatMessage::class.java)
                    if (msg != null) {
                        // Pastikan untuk membedakan pesan pengguna dan AI
                        chatMessages.add(msg)
                    }
                }
                chatAdapter.notifyDataSetChanged()
                chatRecyclerView.scrollToPosition(chatMessages.size - 1)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to load chat", e)
                // Menambahkan pesan error jika gagal memuat
                chatMessages.clear()
                chatMessages.add(ChatMessage("Gagal memuat pesan", user = false))
                chatAdapter.notifyDataSetChanged()
            }
    }
}
