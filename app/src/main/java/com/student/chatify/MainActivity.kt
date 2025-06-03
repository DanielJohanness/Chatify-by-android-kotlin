package com.student.chatify

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
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
    private var isAITyping = false

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi Firebase AI generative model
        generativeModel = Firebase.ai.generativeModel("gemini-1.5-flash")

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        messageEditText.requestFocus()
        sendButton = findViewById(R.id.sendButton)

        chatAdapter = ChatAdapter(chatMessages, this)
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
        addMessage(ChatMessage(message, isUser = true))

        // Tambahkan indikator bahwa AI sedang membalas
        val typingMessage = ChatMessage("AI sedang mengetik...", isUser = false)
        isAITyping = true
        addMessage(typingMessage)

        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(message)
                }

                // Hapus "AI sedang mengetik..."
                if (isAITyping) {
                    removeLastMessage() // Hapus bubble sementara
                    isAITyping = false
                }

                val aiReply = response.text
                if (aiReply.isNullOrBlank()) {
                    addMessage(ChatMessage("AI tidak memberikan balasan.", isUser = false))
                    chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                } else {
                    addMessage(ChatMessage(aiReply, isUser = false))
                    chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                }

            } catch (e: Exception) {
                if (isAITyping) {
                    removeLastMessage()
                    isAITyping = false
                }
                Log.e("ChatAI", "Gagal generate konten AI", e)
                addMessage(ChatMessage("Terjadi kesalahan: ${e.localizedMessage}", isUser = false))
                chatRecyclerView.scrollToPosition(chatMessages.size - 1)
            }
        }
    }

    private fun removeLastMessage() {
        if (chatMessages.isNotEmpty()) {
            chatMessages.removeAt(chatMessages.lastIndex) // GANTI DENGAN removeAt(lastIndex)
            chatAdapter.notifyItemRemoved(chatMessages.size)
            chatRecyclerView.scrollToPosition(chatMessages.size)
        }
    }

    private fun addMessage(chatMessage: ChatMessage) {
        // Cek apakah perlu tambah header tanggal
        val lastMessage = chatMessages.lastOrNull()
        if (lastMessage == null || !isSameDay(lastMessage.timestamp, chatMessage.timestamp)) {
            // Tambah header tanggal dulu
            chatMessages.add(ChatMessage(
                message = "", // kosong, isi hanya di adapter pake tanggal
                isUser = false,
                timestamp = chatMessage.timestamp,
                isDateHeader = true
            ))
        }

        chatMessages.add(chatMessage)
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        chatRecyclerView.scrollToPosition(chatMessages.size - 1)
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
}
