package com.student.chatify.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.student.chatify.R
import com.student.chatify.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val currentUserId: String
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SELF = 0
        private const val VIEW_TYPE_OTHER = 1
    }

    inner class SelfMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val msgText: TextView = view.findViewById(R.id.messageText)
        private val timeText: TextView = view.findViewById(R.id.messageTime)

        fun bind(message: Message) {
            msgText.text = message.content
            timeText.text = formatTimestamp(message.timestamp)
        }
    }

    inner class OtherMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val msgText: TextView = view.findViewById(R.id.messageText)
        private val timeText: TextView = view.findViewById(R.id.messageTime)

        fun bind(message: Message) {
            msgText.text = message.content
            timeText.text = formatTimestamp(message.timestamp)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) VIEW_TYPE_SELF else VIEW_TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SELF) {
            val view = inflater.inflate(R.layout.item_message_self, parent, false)
            SelfMessageViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_message_other, parent, false)
            OtherMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is SelfMessageViewHolder) {
            holder.bind(message)
        } else if (holder is OtherMessageViewHolder) {
            holder.bind(message)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
}
