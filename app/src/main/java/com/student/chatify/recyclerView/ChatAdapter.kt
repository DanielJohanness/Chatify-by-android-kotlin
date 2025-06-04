package com.student.chatify.recyclerView

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.student.chatify.R
import com.student.chatify.model.ChatMessage
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {

    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
        private const val VIEW_TYPE_DATE_HEADER = 3
        private const val VIEW_TYPE_TYPING = 4
        private const val VIEW_TYPE_LOADING = 5
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val context = parent.context // Mendapatkan konteks dari parent
        return when (viewType) {
            VIEW_TYPE_USER -> UserViewHolder(inflater.inflate(R.layout.item_chat_user, parent, false), context)
            VIEW_TYPE_AI -> AIViewHolder(inflater.inflate(R.layout.item_chat_ai, parent, false), context)
            VIEW_TYPE_DATE_HEADER -> DateHeaderViewHolder(inflater.inflate(R.layout.item_chat_date_header, parent, false))
            VIEW_TYPE_TYPING -> TypingViewHolder(inflater.inflate(R.layout.item_chat_typing, parent, false))
            VIEW_TYPE_LOADING -> LoadingViewHolder(inflater.inflate(R.layout.item_chat_loading, parent, false))
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserViewHolder -> holder.bind(message)
            is AIViewHolder -> holder.bind(message)
            is DateHeaderViewHolder -> holder.bind(message)
            is TypingViewHolder -> holder.bind()
            is LoadingViewHolder -> holder.bind()
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)

        if (message.timestamp == Long.MAX_VALUE) return VIEW_TYPE_TYPING
        if (message.timestamp == -1L) return VIEW_TYPE_LOADING

        if (position == 0 || !isSameDay(getItem(position - 1).timestamp, message.timestamp)) {
            return VIEW_TYPE_DATE_HEADER
        }

        return if (message.user) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    fun updateTypingStatus(isTyping: Boolean) {
        val typingMessage = ChatMessage("AI sedang mengetik...", false, Long.MAX_VALUE)

        val newList = if (isTyping) {
            if (!currentList.contains(typingMessage)) currentList + typingMessage else currentList
        } else {
            currentList.filterNot { it.message == "AI sedang mengetik..." }
        }

        submitList(newList)
    }

    // View Holder for User messages
    inner class UserViewHolder(itemView: View, private val context: Context) : RecyclerView.ViewHolder(itemView) {
        private val userTextView: TextView = itemView.findViewById(R.id.userTextView)
        private val userTimeTextView: TextView = itemView.findViewById(R.id.userTimeTextView)
        private val markwon = Markwon.create(context)

        fun bind(message: ChatMessage) {
            markwon.setMarkdown(userTextView, message.message)
            userTimeTextView.text = timeFormatter.format(Date(message.timestamp))
        }
    }

    // View Holder for AI messages
    inner class AIViewHolder(itemView: View, private val context: Context) : RecyclerView.ViewHolder(itemView) {
        private val aiTextView: TextView = itemView.findViewById(R.id.aiTextView)
        private val aiTimeTextView: TextView = itemView.findViewById(R.id.aiTimeTextView)
        private val markwon = Markwon.create(context)

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

        fun bind() {
            typingTextView.text = "AI sedang mengetik..."
        }
    }

    // View Holder for Loading status
    inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val loadingTextView: TextView = itemView.findViewById(R.id.loadingTextView)

        fun bind() {
            loadingTextView.text = "Memuat pesan..."
        }
    }

    // Helper to check if two messages are on the same day
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
