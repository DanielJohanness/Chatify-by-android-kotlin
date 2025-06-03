package com.student.chatify.recyclerView

import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.student.chatify.R
import com.student.chatify.model.ChatMessage
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val messages: MutableList<ChatMessage>,
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
        private const val VIEW_TYPE_DATE_HEADER = 3
        private const val VIEW_TYPE_TYPING = 4
        private const val VIEW_TYPE_LOADING = 5
    }

    private val markwon = Markwon.create(context)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.dateHeader -> VIEW_TYPE_DATE_HEADER
            message.user -> VIEW_TYPE_USER
            message.typingStatus -> VIEW_TYPE_TYPING
            message.loading -> VIEW_TYPE_LOADING
            else -> VIEW_TYPE_AI
        }
    }

    // View Holder for loading status
    inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val loadingTextView: TextView = itemView.findViewById(R.id.loadingTextView)

        fun bind() {
            loadingTextView.text = "Memuat pesan..."
        }
    }

    // View Holder for User messages
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userTextView: TextView = itemView.findViewById(R.id.userTextView)
        private val userTimeTextView: TextView = itemView.findViewById(R.id.userTimeTextView)

        fun bind(message: ChatMessage) {
            markwon.setMarkdown(userTextView, message.message)
            userTimeTextView.text = timeFormatter.format(Date(message.timestamp))
        }
    }

    // View Holder for AI messages
    inner class AIViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val aiTextView: TextView = itemView.findViewById(R.id.aiTextView)
        private val aiTimeTextView: TextView = itemView.findViewById(R.id.aiTimeTextView)

        fun bind(message: ChatMessage) {
            markwon.setMarkdown(aiTextView, message.message)
            aiTimeTextView.text = timeFormatter.format(Date(message.timestamp))
        }
    }

    // View Holder for Date Header
    inner class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateHeaderTextView)

        fun bind(message: ChatMessage) {
            dateTextView.text = dateFormatter.format(Date(message.timestamp))
        }
    }

    // View Holder for Typing status
    inner class TypingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typingTextView: TextView = itemView.findViewById(R.id.typingTextView)
        private val handler = Handler()
        private var dotCount = 0

        private val typingDotsRunnable = object : Runnable {
            override fun run() {
                dotCount = (dotCount + 1) % 4
                typingTextView.text = "AI sedang mengetik${".".repeat(dotCount)}"
                handler.postDelayed(this, 500)
            }
        }

        fun bind(message: ChatMessage) {
            handler.post(typingDotsRunnable)
        }

        fun stopTypingAnimation() {
            handler.removeCallbacks(typingDotsRunnable)
        }
    }

    // Create the view holders and bind them based on the viewType
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> UserViewHolder(inflater.inflate(R.layout.item_chat_user, parent, false))
            VIEW_TYPE_AI -> AIViewHolder(inflater.inflate(R.layout.item_chat_ai, parent, false))
            VIEW_TYPE_DATE_HEADER -> DateHeaderViewHolder(inflater.inflate(R.layout.item_chat_date_header, parent, false))
            VIEW_TYPE_TYPING -> TypingViewHolder(inflater.inflate(R.layout.item_chat_typing, parent, false))
            VIEW_TYPE_LOADING -> LoadingViewHolder(inflater.inflate(R.layout.item_chat_loading, parent, false))
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    // Bind data to view holders
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserViewHolder -> holder.bind(message)
            is AIViewHolder -> holder.bind(message)
            is DateHeaderViewHolder -> holder.bind(message)
            is TypingViewHolder -> holder.bind(message)
            is LoadingViewHolder -> holder.bind()
        }
    }

    // Get item count (number of messages)
    override fun getItemCount() = messages.size

    // Add a message to the list
    fun addMessage(message: ChatMessage) {
        // Check if date header is needed
        val lastMessage = messages.lastOrNull()
        if (lastMessage == null || !isSameDay(lastMessage.timestamp, message.timestamp)) {
            messages.add(ChatMessage(message = "", user = false, timestamp = message.timestamp, dateHeader = true))
        }

        // Add message and update RecyclerView
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    // Helper to check if two messages are on the same day
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
