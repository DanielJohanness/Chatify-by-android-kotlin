// file: ChatAdapter.kt
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
import com.student.chatify.model.ChatItem
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter : ListAdapter<ChatItem, RecyclerView.ViewHolder>(ChatItemDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_USER_MESSAGE = 1
        private const val VIEW_TYPE_AI_MESSAGE = 2
        private const val VIEW_TYPE_TYPING = 3
        private const val VIEW_TYPE_LOADING = 4
    }

    // Formatter untuk waktu dan tanggal
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is ChatItem.Message -> {
                // Jika timestamp = Long.MAX_VALUE, kita gunakan sebagai view typing
                if (item.timestamp == Long.MAX_VALUE) {
                    VIEW_TYPE_TYPING
                } else if (item.timestamp == -1L) {
                    VIEW_TYPE_LOADING
                } else {
                    if (item.isUser) VIEW_TYPE_USER_MESSAGE else VIEW_TYPE_AI_MESSAGE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val ctx = parent.context
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val view = inflater.inflate(R.layout.item_chat_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            VIEW_TYPE_USER_MESSAGE -> {
                val view = inflater.inflate(R.layout.item_message_self, parent, false)
                UserViewHolder(view, ctx)
            }
            VIEW_TYPE_AI_MESSAGE -> {
                val view = inflater.inflate(R.layout.item_message_other, parent, false)
                AIViewHolder(view, ctx)
            }
            VIEW_TYPE_TYPING -> {
                val view = inflater.inflate(R.layout.item_chat_typing, parent, false)
                TypingViewHolder(view)
            }
            VIEW_TYPE_LOADING -> {
                val view = inflater.inflate(R.layout.item_chat_loading, parent, false)
                LoadingViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is ChatItem.Message -> {
                when (holder) {
                    is UserViewHolder -> holder.bind(item)
                    is AIViewHolder -> holder.bind(item)
                    is TypingViewHolder -> holder.bind()
                    is LoadingViewHolder -> holder.bind()
                }
            }
        }
    }

    /** Hanya menambahkan atau menghapus “typing” indicator */
    fun updateTypingStatus(isTyping: Boolean) {
        val currentList = currentList.toMutableList()
        // Cari posisi existing tipe Typing
        val typingIndex = currentList.indexOfFirst {
            it is ChatItem.Message && it.timestamp == Long.MAX_VALUE
        }
        if (isTyping) {
            // Jika belum ada, tambahkan pesan khusus dengan timestamp = Long.MAX_VALUE
            if (typingIndex == -1) {
                currentList.add(ChatItem.Message(
                    id = "typing", // ID konstan
                    text = "",
                    isUser = false,
                    timestamp = Long.MAX_VALUE
                ))
            }
        } else {
            // Jika ada, hapus
            if (typingIndex != -1) {
                currentList.removeAt(typingIndex)
            }
        }
        submitList(currentList)
    }

    // ViewHolder untuk pesan user
    inner class UserViewHolder(itemView: View, context: Context) : RecyclerView.ViewHolder(itemView) {
        private val txtMessage: TextView = itemView.findViewById(R.id.messageText)
        private val txtTime: TextView = itemView.findViewById(R.id.messageTime)
        private val markwon = Markwon.create(context)

        fun bind(message: ChatItem.Message) {
            markwon.setMarkdown(txtMessage, message.text)
            txtTime.text = timeFormatter.format(Date(message.timestamp))

            // Jika perlu, bisa tambahkan indikator status (misalnya icon centang jika SENT)
            // Contoh (asumsi ada TextView atau ImageView untuk status):
            // when(message.status) { ... }
        }
    }

    // ViewHolder untuk pesan AI
    inner class AIViewHolder(itemView: View, context: Context) : RecyclerView.ViewHolder(itemView) {
        private val txtMessage: TextView = itemView.findViewById(R.id.messageText)
        private val txtTime: TextView = itemView.findViewById(R.id.messageTime)
        private val markwon = Markwon.create(context)

        fun bind(message: ChatItem.Message) {
            markwon.setMarkdown(txtMessage, message.text)
            txtTime.text = timeFormatter.format(Date(message.timestamp))
        }
    }

    // ViewHolder untuk header tanggal
    inner class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtDateHeader: TextView = itemView.findViewById(R.id.dateHeaderTextView)
        fun bind(header: ChatItem.DateHeader) {
            txtDateHeader.text = dateFormatter.format(Date(header.date))
        }
    }

    // ViewHolder untuk “AI sedang mengetik” (menggunakan "typing" status)
    inner class TypingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTyping: TextView = itemView.findViewById(R.id.typingTextView)
        fun bind() {
            txtTyping.text = "AI sedang mengetik..."
        }
    }

    // ViewHolder untuk “Memuat pesan…” (indicator loading)
    inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtLoading: TextView = itemView.findViewById(R.id.loadingTextView)
        fun bind() {
            txtLoading.text = "Memuat..."
            // progressBar otomatis animasi
        }
    }
}

/** DiffUtil untuk ChatItem */
private class ChatItemDiffCallback : DiffUtil.ItemCallback<ChatItem>() {
    override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        return when {
            oldItem is ChatItem.DateHeader && newItem is ChatItem.DateHeader ->
                oldItem.date == newItem.date
            oldItem is ChatItem.Message && newItem is ChatItem.Message ->
                oldItem.id == newItem.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        return oldItem == newItem
    }
}
