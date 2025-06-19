package com.student.chatify.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.student.chatify.R
import com.student.chatify.model.ChatSummary
import com.student.chatify.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val currentUserUid: String,
    private val onItemClick: (ChatSummary) -> Unit
) : ListAdapter<ChatSummary, ChatListAdapter.ChatViewHolder>(DiffCallback()) {

    private val userCache = mutableMapOf<String, User>()
    private val userListeners = mutableMapOf<String, ListenerRegistration>()
    private val db = FirebaseFirestore.getInstance()

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.profileImageView)
        val name: TextView = view.findViewById(R.id.chatName)
        val lastMsg: TextView = view.findViewById(R.id.lastMessage)
        val statusView: TextView = view.findViewById(R.id.statusView)
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
        val otherUid = item.participants.firstOrNull { it != currentUserUid }

        holder.lastMsg.text = item.lastMessage
        holder.time.text = getRelativeTime(item.lastMessageTime)
        holder.unreadBadge.isVisible = item.unreadCount > 0
        holder.unreadBadge.text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString()

        if (otherUid == null) {
            holder.name.text = "Pengguna tidak dikenal"
            holder.profileImage.setImageResource(R.drawable.ic_default_profile)
            holder.statusView.text = ""
        } else {
            listenToUserChanges(otherUid, holder, item)
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    private fun listenToUserChanges(
        uid: String,
        holder: ChatViewHolder,
        chat: ChatSummary
    ) {
        val context = holder.itemView.context

        // Register listener hanya sekali per UID
        if (!userListeners.containsKey(uid)) {
            val listener = db.collection("users").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    val user = snapshot?.toObject(User::class.java) ?: return@addSnapshotListener
                    userCache[uid] = user
                    notifyItemChanged(currentList.indexOfFirst { it.participants.contains(uid) })
                }
            userListeners[uid] = listener
        }

        // Gunakan data terakhir di-cache
        val user = userCache[uid]
        if (user != null) {
            holder.name.text = user.displayName
            holder.lastMsg.text = chat.lastMessage
            holder.statusView.text = when {
                user.isOnline -> "Online"
                user.lastSeen > 0L -> "Terakhir dilihat ${getRelativeTime(user.lastSeen)}"
                else -> ""
            }
            holder.statusView.setTextColor(
                context.getColor(
                    if (user.isOnline) R.color.green_500 else R.color.gray_600
                )
            )

            Glide.with(context)
                .load(user.profileImage)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .circleCrop()
                .into(holder.profileImage)
        }
    }

    fun clearListeners() {
        userListeners.values.forEach { it.remove() }
        userListeners.clear()
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatSummary>() {
        override fun areItemsTheSame(old: ChatSummary, new: ChatSummary) = old.chatId == new.chatId
        override fun areContentsTheSame(old: ChatSummary, new: ChatSummary) = old == new
    }

    private fun getRelativeTime(timeMillis: Long): String {
        if (timeMillis <= 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timeMillis
        val minute = 60 * 1000
        val hour = 60 * minute
        val day = 24 * hour

        return when {
            diff < minute -> "Barusan"
            diff < hour -> "${diff / minute} mnt lalu"
            diff < day -> "${diff / hour} jam lalu"
            diff < 2 * day -> "Kemarin"
            diff < 7 * day -> "${diff / day} hari lalu"
            else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timeMillis))
        }
    }
}
