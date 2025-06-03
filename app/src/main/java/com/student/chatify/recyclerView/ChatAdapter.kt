package com.student.chatify.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.student.chatify.R
import com.student.chatify.model.ChatMessage
import io.noties.markwon.Markwon
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date


class ChatAdapter(private val messages: List<ChatMessage>, context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
        private const val VIEW_TYPE_DATE_HEADER = 3
    }

    private val markwon = Markwon.create(context)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return when {
            msg.isDateHeader -> VIEW_TYPE_DATE_HEADER
            msg.isUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = inflater.inflate(R.layout.item_chat_user, parent, false)
                UserViewHolder(view)
            }
            VIEW_TYPE_AI -> {
                val view = inflater.inflate(R.layout.item_chat_ai, parent, false)
                AIViewHolder(view)
            }
            VIEW_TYPE_DATE_HEADER -> {
                val view = inflater.inflate(R.layout.item_chat_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is UserViewHolder -> holder.bind(msg)
            is AIViewHolder -> holder.bind(msg)
            is DateHeaderViewHolder -> holder.bind(msg)
        }
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userTextView: TextView = itemView.findViewById(R.id.userTextView)
        private val userTimeTextView: TextView = itemView.findViewById(R.id.userTimeTextView)

        fun bind(message: ChatMessage) {
            markwon.setMarkdown(userTextView, message.message)
            userTimeTextView.text = timeFormatter.format(Date(message.timestamp))
        }
    }

    inner class AIViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val aiTextView: TextView = itemView.findViewById(R.id.aiTextView)
        private val aiTimeTextView: TextView = itemView.findViewById(R.id.aiTimeTextView)

        fun bind(message: ChatMessage) {
            markwon.setMarkdown(aiTextView, message.message)
            aiTimeTextView.text = timeFormatter.format(Date(message.timestamp))
        }
    }

    inner class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateHeaderTextView)

        fun bind(message: ChatMessage) {
            dateTextView.text = dateFormatter.format(Date(message.timestamp))
        }
    }
}
