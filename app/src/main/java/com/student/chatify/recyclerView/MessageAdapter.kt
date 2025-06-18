// file: MessageAdapter.kt
package com.student.chatify.recyclerView

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.student.chatify.R
import com.student.chatify.model.Message
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val currentUserUid: String,
    private val context: Context
) : ListAdapter<MessageAdapter.MessageItem, RecyclerView.ViewHolder>(MessageItemDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_SELF = 1
        private const val VIEW_TYPE_OTHER = 2
        private const val VIEW_TYPE_AI = 3
        private const val VIEW_TYPE_TYPING = 4
        private const val VIEW_TYPE_LOADING = 5
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    private val markwon = Markwon.create(context)

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is MessageItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is MessageItem.MessageData -> {
                when {
                    item.message.timestamp == Long.MAX_VALUE -> VIEW_TYPE_TYPING
                    item.message.timestamp == -1L -> VIEW_TYPE_LOADING
                    item.message.senderId == currentUserUid -> VIEW_TYPE_SELF
                    item.message.senderId == "chatify" -> VIEW_TYPE_AI
                    else -> VIEW_TYPE_OTHER
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> DateHeaderViewHolder(
                inflater.inflate(R.layout.item_chat_date_header, parent, false)
            )
            VIEW_TYPE_SELF -> SelfMessageViewHolder(
                inflater.inflate(R.layout.item_message_self, parent, false)
            )
            VIEW_TYPE_OTHER -> OtherMessageViewHolder(
                inflater.inflate(R.layout.item_message_other, parent, false)
            )
            VIEW_TYPE_AI -> AiMessageViewHolder(
                inflater.inflate(R.layout.item_message_ai, parent, false)
            )
            VIEW_TYPE_TYPING -> TypingViewHolder(
                inflater.inflate(R.layout.item_chat_typing, parent, false)
            )
            VIEW_TYPE_LOADING -> LoadingViewHolder(
                inflater.inflate(R.layout.item_chat_loading, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MessageItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item.date)
            is MessageItem.MessageData -> {
                when (holder) {
                    is SelfMessageViewHolder -> holder.bind(item.message)
                    is OtherMessageViewHolder -> holder.bind(item.message)
                    is AiMessageViewHolder -> holder.bind(item.message)
                    is TypingViewHolder -> holder.bind()
                    is LoadingViewHolder -> holder.bind()
                }
            }
        }
    }

    fun updateTypingStatus(isTyping: Boolean) {
        val current = currentList.toMutableList()
        val typingIndex = current.indexOfFirst {
            it is MessageItem.MessageData && it.message.timestamp == Long.MAX_VALUE
        }

        if (isTyping && typingIndex == -1) {
            current.add(
                MessageItem.MessageData(
                    Message(
                        id = "typing",
                        senderId = "chatify",
                        text = "",
                        type = "text",
                        timestamp = Long.MAX_VALUE,
                        status = "sent"
                    )
                )
            )
        } else if (!isTyping && typingIndex != -1) {
            current.removeAt(typingIndex)
        }
        submitList(current)
    }

    // ViewHolders
    inner class SelfMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtMessage: TextView = itemView.findViewById(R.id.messageText)
        private val txtTime: TextView = itemView.findViewById(R.id.messageTime)
        private val txtStatus: TextView = itemView.findViewById(R.id.messageStatus)

        fun bind(message: Message) {
            markwon.setMarkdown(txtMessage, message.text)
            txtTime.text = timeFormat.format(Date(message.timestamp))
            txtStatus.text = when (message.status) {
                "sent" -> "✓"
                "delivered" -> "✓✓"
                "read" -> "✓✓ (dibaca)"
                else -> ""
            }
        }
    }

    inner class OtherMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtMessage: TextView = itemView.findViewById(R.id.messageText)
        private val txtTime: TextView = itemView.findViewById(R.id.messageTime)
        fun bind(message: Message) {
            markwon.setMarkdown(txtMessage, message.text)
            txtTime.text = timeFormat.format(Date(message.timestamp))
        }
    }

    inner class AiMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtMessage: TextView = itemView.findViewById(R.id.messageText)
        private val txtTime: TextView = itemView.findViewById(R.id.messageTime)
        fun bind(message: Message) {
            markwon.setMarkdown(txtMessage, message.text)
            txtTime.text = timeFormat.format(Date(message.timestamp))
        }
    }

    inner class TypingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTyping: TextView = itemView.findViewById(R.id.typingTextView)
        fun bind() {
            txtTyping.text = "sedang mengetik..."
        }
    }

    inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtLoading: TextView = itemView.findViewById(R.id.loadingTextView)
        fun bind() {
            txtLoading.text = "Memuat..."
        }
    }

    inner class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtDate: TextView = itemView.findViewById(R.id.dateHeaderTextView)
        fun bind(dateMillis: Long) {
            txtDate.text = formatDateNatural(dateMillis)
        }
    }

    private fun formatDateNatural(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val today = Date(now)
        val date = Date(timestamp)

        val dayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

        return when {
            isSameDay(today, date) -> "Hari ini"
            isYesterday(today, date) -> "Kemarin"
            else -> dayFormat.format(date)
        }
    }

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(d1) == fmt.format(d2)
    }

    private fun isYesterday(today: Date, date: Date): Boolean {
        val cal = java.util.Calendar.getInstance()
        cal.time = today
        cal.add(java.util.Calendar.DATE, -1)
        val yesterday = cal.time
        return isSameDay(yesterday, date)
    }

    // Item sealed class
    sealed class MessageItem {
        data class MessageData(val message: Message) : MessageItem()
        data class DateHeader(val date: Long) : MessageItem()
    }

    class MessageItemDiffCallback : DiffUtil.ItemCallback<MessageItem>() {
        override fun areItemsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
            return when {
                oldItem is MessageItem.DateHeader && newItem is MessageItem.DateHeader ->
                    oldItem.date == newItem.date
                oldItem is MessageItem.MessageData && newItem is MessageItem.MessageData ->
                    oldItem.message.id == newItem.message.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
            return oldItem == newItem
        }
    }

    fun addTemporaryMessage(message: Message) {
        val newList = ArrayList(currentList)
        newList.add(MessageItem.MessageData(message))
        submitList(newList)
    }

    fun updateMessageStatus(messageId: String, newStatus: String) {
        val updated = currentList.map {
            if (it is MessageItem.MessageData && it.message.id == messageId) {
                MessageItem.MessageData(it.message.copy(status = newStatus))
            } else it
        }
        submitList(updated)
    }
}
