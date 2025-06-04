package com.student.chatify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.student.chatify.recyclerView.ChatAdapter
import com.student.chatify.viewmodel.ChatViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private val chatAdapter by lazy { ChatAdapter() }
    private val chatViewModel: ChatViewModel by viewModels() // Using ViewModel to manage state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        setupRecyclerView()
        setupSendButton()
        setupMessageInputListener()

        observeViewModel()
        chatViewModel.loadChatHistory() // Load history when the activity is created
    }

    private fun setupRecyclerView() {
        chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true // Scroll automatically to the bottom
            }
        }
    }

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                chatViewModel.sendUserMessage(message)
                messageEditText.text.clear()
            }
        }
    }

    private fun setupMessageInputListener() {
        messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun observeViewModel() {
        chatViewModel.chatMessages.observe(this) { messages ->
            Log.d("MainActivity", "Total messages: ${messages.size}")
            messages.forEach { Log.d("MainActivity", it.toString()) }
            chatAdapter.submitList(messages)
            chatRecyclerView.scrollToPosition(messages.size - 1)
        }

        chatViewModel.typingStatus.observe(this) { isTyping ->
            chatAdapter.updateTypingStatus(isTyping) // Update typing status in the adapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_profile -> {
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
}
