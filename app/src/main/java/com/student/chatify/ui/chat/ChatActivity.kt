package com.student.chatify.ui.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.student.chatify.R
import com.student.chatify.data.PresenceManager
import com.student.chatify.data.repository.ChatRepository
import com.student.chatify.model.Message
import com.student.chatify.model.User
import com.student.chatify.recyclerView.MessageAdapter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity(), MessageAdapter.ScrollToBottomListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var adapter: MessageAdapter

    private lateinit var profileImageView: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var statusTextView: TextView

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val repository by lazy { ChatRepository(firestore) }
    private val viewModelFactory by lazy { ChatViewModelFactory(repository) }
    private val viewModel: ChatViewModel by viewModels { viewModelFactory }

    private val currentUserUid: String by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    private lateinit var otherUserUid: String
    private lateinit var chatId: String

    private var isUserAtBottom = true
    private var typingTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Toolbar setup
        val topAppBar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // <-- ini penting
        topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Ambil referensi UI dari Toolbar
        val toolbarContent = findViewById<LinearLayout>(R.id.toolbarContent)
        profileImageView = toolbarContent.findViewById(R.id.profileImageView)
        usernameTextView = toolbarContent.findViewById(R.id.usernameTextView)
        statusTextView = toolbarContent.findViewById(R.id.statusTextView)

        // Ambil referensi UI lainnya
        recyclerView = findViewById(R.id.messageRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        // Cek UID dan setup
        if (currentUserUid.isEmpty()) return
        otherUserUid = intent.getStringExtra("otherUserUid") ?: return
        chatId = repository.getChatId(currentUserUid, otherUserUid)

        adapter = MessageAdapter(currentUserUid, this)
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerView.adapter = adapter
        adapter.scrollToBottomListener = this

        createChatIfNeeded()
        setupAutoScroll()
        observeMessages()
        observeTypingStatus()
        setupTypingWatcher()
        observeOtherUserData()

        sendButton.setOnClickListener {
            val text = messageEditText.text.toString().trim()
            if (text.isNotEmpty()) sendMessage(text)
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            viewModel.resetUnreadCount(chatId, currentUserUid)
            markMessagesAsRead()
        }
    }

    override fun onStop() {
        super.onStop()
        firestore.collection("chats").document(chatId)
            .update("typing.$currentUserUid", false)
    }

    private fun createChatIfNeeded() {
        lifecycleScope.launch {
            repository.startOrCreateChat(currentUserUid, otherUserUid)?.let {
                chatId = it
                viewModel.resetUnreadCount(chatId, currentUserUid)
            }
        }
    }

    private fun setupAutoScroll() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as LinearLayoutManager
                isUserAtBottom = lm.findLastCompletelyVisibleItemPosition() >= adapter.itemCount - 1
            }
        })
    }

    private fun observeMessages() {
        viewModel.messages.observe(this) { messages ->
            val items = buildMessageItems(messages)
            var hasResetUnread = false

            messages.forEach { message ->
                val isIncoming = message.senderId == otherUserUid
                val isOutgoing = message.senderId == currentUserUid

                when {
                    isIncoming && message.status == "sent" -> {
                        viewModel.updateMessageStatus(chatId, message.id, "delivered")
                    }
                    isIncoming && message.status == "delivered" -> {
                        viewModel.updateMessageStatus(chatId, message.id, "read")
                        adapter.updateMessageStatus(message.id, "read")
                        hasResetUnread = true
                    }
                    isOutgoing && message.status == "sending" -> {
                        viewModel.updateMessageStatus(chatId, message.id, "sent")
                    }
                }
            }

            if (hasResetUnread) {
                lifecycleScope.launch {
                    viewModel.resetUnreadCount(chatId, currentUserUid)
                }
            }

            adapter.submitList(items) {
                if (isUserAtBottom) scrollToBottom()
            }
        }

        viewModel.loadMessages(chatId)
    }

    private fun observeTypingStatus() {
        PresenceManager.observeTypingStatus(chatId, otherUserUid) { isTyping ->
            adapter.updateTypingStatus(isTyping)
        }
    }

    private fun setupTypingWatcher() {
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val isTyping = !s.isNullOrBlank()
                PresenceManager.setTypingStatus(chatId, currentUserUid, isTyping)

                typingTimer?.cancel()
                if (isTyping) {
                    typingTimer = Timer()
                    typingTimer?.schedule(object : TimerTask() {
                        override fun run() {
                            PresenceManager.setTypingStatus(chatId, currentUserUid, false)
                        }
                    }, 3000)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun sendMessage(text: String) {
        val timestamp = System.currentTimeMillis()
        val messageId = firestore.collection("dummy").document().id
        val message = Message(
            id = messageId,
            senderId = currentUserUid,
            text = text,
            type = "text",
            timestamp = timestamp,
            status = "sending"
        )

        adapter.addTemporaryMessage(message)
        messageEditText.text.clear()

        viewModel.sendMessage(chatId, message, otherUserUid)
        viewModel.updateChatSummary(chatId, listOf(currentUserUid, otherUserUid), text, timestamp)

        if (text.contains("@chatify", ignoreCase = true)) {
            generateAIReply(text)
        }
    }

    private fun generateAIReply(userMessage: String) {
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.0-flash")

        lifecycleScope.launch {
            try {
                val prompt = userMessage.replace("@chatify", "", ignoreCase = true).trim()
                val response = model.generateContent(prompt).text ?: return@launch

                val aiMessageId = firestore.collection("dummy").document().id
                val aiMessage = Message(
                    id = aiMessageId,
                    senderId = "chatify",
                    text = response,
                    type = "text",
                    timestamp = System.currentTimeMillis(),
                    status = "sent"
                )

                viewModel.sendMessage(chatId, aiMessage, currentUserUid)
                viewModel.updateChatSummary(chatId, listOf(currentUserUid, otherUserUid), response, aiMessage.timestamp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun buildMessageItems(messages: List<Message>): List<MessageAdapter.MessageItem> {
        val items = mutableListOf<MessageAdapter.MessageItem>()
        var lastDateKey: String? = null

        for (msg in messages) {
            val cal = Calendar.getInstance().apply { timeInMillis = msg.timestamp }
            val dateKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
            if (dateKey != lastDateKey) {
                items.add(MessageAdapter.MessageItem.DateHeader(msg.timestamp))
                lastDateKey = dateKey
            }
            items.add(MessageAdapter.MessageItem.MessageData(msg))
        }
        return items
    }

    private suspend fun markMessagesAsRead() {
        val messages = viewModel.messages.value ?: return
        var shouldReset = false

        messages.filter { it.senderId == otherUserUid && it.status != "read" }.forEach {
            viewModel.updateMessageStatus(chatId, it.id, "read")
            adapter.updateMessageStatus(it.id, "read")
            shouldReset = true
        }

        if (shouldReset) {
            viewModel.resetUnreadCount(chatId, currentUserUid)
        }
    }

    override fun scrollToBottom() {
        recyclerView.post {
            recyclerView.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun observeOtherUserData() {
        val userDocRef = firestore.collection("users").document(otherUserUid)

        userDocRef.addSnapshotListener { snapshot, _ ->
            val user = snapshot?.toObject(User::class.java) ?: return@addSnapshotListener

            usernameTextView.text = user.displayName

            Glide.with(this)
                .load(user.profileImage)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .circleCrop()
                .into(profileImageView)
        }

        PresenceManager.observeUserPresence(otherUserUid) { isOnline, lastSeen ->
            statusTextView.text = if (isOnline) {
                "Online"
            } else {
                "Terakhir dilihat ${getRelativeTime(lastSeen)}"
            }

            val colorRes = if (isOnline) R.color.green_500 else R.color.colorOnPrimary
            statusTextView.setTextColor(getColor(colorRes))
        }
    }

    private fun getRelativeTime(timeMillis: Long): String {
        if (timeMillis <= 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timeMillis
        val minute = 60 * 1000
        val hour = 60 * minute
        val day = 24 * hour

        return when {
            diff < minute -> "barusan"
            diff < hour -> "${diff / minute} mnt lalu"
            diff < day -> "${diff / hour} jam lalu"
            diff < 2 * day -> "kemarin"
            diff < 7 * day -> "${diff / day} hari lalu"
            else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timeMillis))
        }
    }
}
