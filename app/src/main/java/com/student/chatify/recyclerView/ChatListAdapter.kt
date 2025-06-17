package com.student.chatify.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.student.chatify.R
import com.student.chatify.model.ChatListItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val onItemClick: (ChatListItem) -> Unit
) : ListAdapter<ChatListItem, ChatListAdapter.ChatViewHolder>(DiffCallback()) {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.chatName)
        val lastMsg = view.findViewById<TextView>(R.id.lastMessage)
        val time = view.findViewById<TextView>(R.id.timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val item = getItem(position)
        holder.name.text = item.chatName
        holder.lastMsg.text = item.lastMessage
        holder.time.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.timestamp))
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
        override fun areItemsTheSame(old: ChatListItem, new: ChatListItem) = old.chatId == new.chatId
        override fun areContentsTheSame(old: ChatListItem, new: ChatListItem) = old == new
    }
}
