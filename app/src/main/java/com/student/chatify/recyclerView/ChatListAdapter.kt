// ✅ ChatListAdapter.kt
package com.student.chatify.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.student.chatify.R
import com.student.chatify.model.ChatSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val currentUserUid: String,
    private val onItemClick: (ChatSummary) -> Unit
) : ListAdapter<ChatSummary, ChatListAdapter.ChatViewHolder>(DiffCallback()) {

    private val userNameCache = mutableMapOf<String, String>()
    private val db = FirebaseFirestore.getInstance()

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.chatName)
        val lastMsg: TextView = view.findViewById(R.id.lastMessage)
        val time: TextView = view.findViewById(R.id.timestamp)
        val unreadBadge: TextView = view.findViewById(R.id.unreadBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val item = getItem(position)
        val otherUids = item.participants.filter { it != currentUserUid }

        holder.lastMsg.text = item.lastMessage
        holder.time.text = getRelativeTime(item.lastMessageTime)

        holder.unreadBadge.isVisible = item.unreadCount > 0
        holder.unreadBadge.text = when {
            item.unreadCount > 99 -> "99+"
            else -> item.unreadCount.toString()
        }

        if (otherUids.isEmpty()) {
            holder.name.text = "Pengguna tidak dikenal"
        } else {
            loadUserNames(otherUids) { names ->
                holder.name.text = names.joinToString(", ")
            }
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    private fun loadUserNames(uids: List<String>, callback: (List<String>) -> Unit) {
        val names = uids.mapNotNull { userNameCache[it] }.toMutableList()
        val missing = uids.filterNot { userNameCache.containsKey(it) }

        if (missing.isEmpty()) {
            callback(names)
            return
        }

        missing.forEach { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: uid
                    userNameCache[uid] = name
                    if (userNameCache.keys.containsAll(uids)) {
                        callback(uids.map { userNameCache[it] ?: it })
                    }
                }.addOnFailureListener {
                    userNameCache[uid] = uid
                    if (userNameCache.keys.containsAll(uids)) {
                        callback(uids.map { userNameCache[it] ?: it })
                    }
                }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatSummary>() {
        override fun areItemsTheSame(old: ChatSummary, new: ChatSummary) = old.chatId == new.chatId
        override fun areContentsTheSame(old: ChatSummary, new: ChatSummary) = old == new
    }

    private fun getRelativeTime(timeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timeMillis
        val minute = 60 * 1000
        val hour = 60 * minute
        val day = 24 * hour

        return when {
            diff < minute -> "Barusan"
            diff < hour -> "${diff / minute} mnt"
            diff < day -> "${diff / hour} jam"
            diff < 2 * day -> "Kemarin"
            diff < 7 * day -> "${diff / day} hari"
            else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timeMillis))
        }
    }
}
