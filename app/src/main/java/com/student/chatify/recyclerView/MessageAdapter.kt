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
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val currentUserUid: String,
    context: Context
) : ListAdapter<MessageAdapter.MessageItem, RecyclerView.ViewHolder>(MessageItemDiffCallback()) {

    interface ScrollToBottomListener {
        fun scrollToBottom()
    }

    var scrollToBottomListener: ScrollToBottomListener? = null

    companion object {
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_SELF = 1
        private const val VIEW_TYPE_OTHER = 2
        private const val VIEW_TYPE_AI = 3
        private const val VIEW_TYPE_TYPING = 4
        private const val VIEW_TYPE_LOADING = 5
    }

    private val markwon = Markwon.create(context)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is MessageItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is MessageItem.MessageData -> {
                val msg = item.message
                when {
                    msg.timestamp == Long.MAX_VALUE -> VIEW_TYPE_TYPING
                    msg.timestamp == -1L -> VIEW_TYPE_LOADING
                    msg.senderId == currentUserUid -> VIEW_TYPE_SELF
                    msg.senderId == "chatify" -> VIEW_TYPE_AI
                    else -> VIEW_TYPE_OTHER
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val layout = when (viewType) {
            VIEW_TYPE_DATE_HEADER -> R.layout.item_chat_date_header
            VIEW_TYPE_SELF -> R.layout.item_message_self
            VIEW_TYPE_OTHER -> R.layout.item_message_other
            VIEW_TYPE_AI -> R.layout.item_message_ai
            VIEW_TYPE_TYPING -> R.layout.item_chat_typing
            VIEW_TYPE_LOADING -> R.layout.item_chat_loading
            else -> throw IllegalArgumentException("Unknown view type")
        }
        val view = inflater.inflate(layout, parent, false)

        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> DateHeaderViewHolder(view)
            VIEW_TYPE_SELF -> SelfMessageViewHolder(view)
            VIEW_TYPE_OTHER -> OtherMessageViewHolder(view)
            VIEW_TYPE_AI -> AiMessageViewHolder(view)
            VIEW_TYPE_TYPING -> TypingViewHolder(view)
            VIEW_TYPE_LOADING -> LoadingViewHolder(view)
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MessageItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item.date)
            is MessageItem.MessageData -> when (holder) {
                is SelfMessageViewHolder -> holder.bind(item.message)
                is OtherMessageViewHolder -> holder.bind(item.message)
                is AiMessageViewHolder -> holder.bind(item.message)
                is TypingViewHolder -> holder.bind()
                is LoadingViewHolder -> holder.bind()
            }
        }
    }

    fun updateTypingStatus(isTyping: Boolean) {
        val list = currentList.toMutableList()
        val index = list.indexOfFirst {
            it is MessageItem.MessageData && it.message.id == "typing"
        }
        if (isTyping && index == -1) {
            list.add(
                MessageItem.MessageData(
                    Message(
                        id = "typing",
                        senderId = "chatify",
                        text = "...",
                        type = "text",
                        timestamp = Long.MAX_VALUE,
                        status = "sent"
                    )
                )
            )
            submitList(list)
        } else if (!isTyping && index != -1) {
            list.removeAt(index)
            submitList(list)
        }
    }

//    fun updateTypingStatus(isTyping: Boolean) {
//        val list = currentList.toMutableList()
//        val hasTyping = list.any { it is MessageItem.MessageData && it.message.timestamp == Long.MAX_VALUE }
//
//        if (isTyping && !hasTyping) {
//            list.add(
//                MessageItem.MessageData(
//                    Message(
//                        id = "typing",
//                        senderId = "chatify",
//                        text = "",
//                        type = "text",
//                        timestamp = Long.MAX_VALUE,
//                        status = "sent"
//                    )
//                )
//            )
//            submitList(list)
//        } else if (!isTyping && hasTyping) {
//            submitList(list.filterNot {
//                it is MessageItem.MessageData && it.message.timestamp == Long.MAX_VALUE
//            })
//        }
//    }

    fun addTemporaryMessage(message: Message) {
        val updated = currentList + MessageItem.MessageData(message)
        submitList(updated) {
            scrollToBottomListener?.scrollToBottom()
        }
    }

    fun updateMessageStatus(messageId: String, newStatus: String) {
        val updated = currentList.map {
            if (it is MessageItem.MessageData && it.message.id == messageId) {
                it.copy(message = it.message.copy(status = newStatus))
            } else it
        }
        submitList(updated)
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
                "delivered", "read" -> "✓✓"
                else -> ""
            }
        }
    }

    inner class OtherMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtMessage: TextView = view.findViewById(R.id.messageText)
        private val txtTime: TextView = view.findViewById(R.id.messageTime)
        fun bind(message: Message) {
            markwon.setMarkdown(txtMessage, message.text)
            txtTime.text = timeFormat.format(Date(message.timestamp))
        }
    }

    inner class AiMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtMessage: TextView = view.findViewById(R.id.messageText)
        private val txtTime: TextView = view.findViewById(R.id.messageTime)
        fun bind(message: Message) {
            markwon.setMarkdown(txtMessage, message.text)
            txtTime.text = timeFormat.format(Date(message.timestamp))
        }
    }

    inner class TypingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtTyping: TextView = view.findViewById(R.id.typingTextView)
        fun bind() {
            txtTyping.text = "sedang mengetik..."
        }
    }

    inner class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtLoading: TextView = view.findViewById(R.id.loadingTextView)
        fun bind() {
            txtLoading.text = "Memuat..."
        }
    }

    inner class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtDate: TextView = view.findViewById(R.id.dateHeaderTextView)
        fun bind(timestamp: Long) {
            txtDate.text = formatDateNatural(timestamp)
        }
    }

    private fun formatDateNatural(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val today = Date(now)
        val date = Date(timestamp)

        return when {
            isSameDay(today, date) -> "Hari ini"
            isYesterday(today, date) -> "Kemarin"
            else -> SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(date)
        }
    }

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(d1) == fmt.format(d2)
    }

    private fun isYesterday(today: Date, date: Date): Boolean {
        val cal = Calendar.getInstance()
        cal.time = today
        cal.add(Calendar.DATE, -1)
        return isSameDay(cal.time, date)
    }

    // MessageItem
    sealed class MessageItem {
        data class MessageData(val message: Message) : MessageItem()
        data class DateHeader(val date: Long) : MessageItem()
    }

    class MessageItemDiffCallback : DiffUtil.ItemCallback<MessageItem>() {
        override fun areItemsTheSame(old: MessageItem, new: MessageItem): Boolean =
            old is MessageItem.MessageData && new is MessageItem.MessageData && old.message.id == new.message.id ||
                    old is MessageItem.DateHeader && new is MessageItem.DateHeader && old.date == new.date

        override fun areContentsTheSame(old: MessageItem, new: MessageItem): Boolean = old == new
    }
}
