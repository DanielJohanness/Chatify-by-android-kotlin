package com.student.chatify.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.student.chatify.R
import com.student.chatify.model.ChatSummary
import com.student.chatify.recyclerView.ChatListAdapter
import com.student.chatify.ui.chat.ChatActivity

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatListAdapter
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var emptyTextView: MaterialTextView

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var chatListener: ListenerRegistration? = null
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()
    private val chatSummariesMap = mutableMapOf<String, ChatSummary>()
    private val currentUid get() = auth.currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.chatListRecyclerView)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        emptyTextView = view.findViewById(R.id.emptyTextView)

        if (currentUid == null) {
            emptyTextView.text = "Pengguna belum login."
            emptyTextView.isVisible = true
            return
        }

        setupRecyclerView()
        listenToChatList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatListener?.remove()
        adapter.clearListeners()
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()
    }

    private fun setupRecyclerView() {
        adapter = ChatListAdapter(currentUid!!) { chatItem ->
            val otherUserUid = chatItem.participants.firstOrNull { it != currentUid } ?: return@ChatListAdapter
            startActivity(Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("chatId", chatItem.chatId)
                putExtra("otherUserUid", otherUserUid)
            })
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun listenToChatList() {
        progressIndicator.isVisible = true
        recyclerView.isVisible = false
        emptyTextView.isVisible = false

        chatListener?.remove()
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()
        chatSummariesMap.clear()

        try {
            chatListener = db.collection("chats")
                .whereArrayContains("participants", currentUid!!)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->

                    if (!isAdded) return@addSnapshotListener
                    Log.d("HomeFragment", "Chat snapshot: ${snapshot?.documents?.size} | Error: ${error?.message}")

                    if (error != null || snapshot == null) {
                        emptyTextView.text = "Gagal memuat daftar chat."
                        emptyTextView.isVisible = true
                        progressIndicator.isVisible = false
                        recyclerView.isVisible = false
                        return@addSnapshotListener
                    }

                    val newChatIds = snapshot.documents.map { it.id }.toSet()
                    val removedChatIds = chatSummariesMap.keys - newChatIds

                    removedChatIds.forEach { chatId ->
                        chatSummariesMap.remove(chatId)
                        messageListeners.remove(chatId)?.remove()
                    }

                    for (doc in snapshot.documents) {
                        val chatId = doc.id
                        val participants = (doc.get("participants") as? List<*>)?.filterIsInstance<String>() ?: continue
                        val unreadCounts = (doc.get("unreadCounts") as? Map<*, *>)?.mapNotNull {
                            val key = it.key as? String
                            val value = (it.value as? Number)?.toLong()
                            if (key != null && value != null) key to value else null
                        }?.toMap() ?: emptyMap()
                        val unread = unreadCounts[currentUid] ?: 0

                        messageListeners[chatId]?.remove()
                        val msgListener = db.collection("chats").document(chatId)
                            .collection("messages")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(1)
                            .addSnapshotListener inner@{ msgSnap, _ ->
                                if (!isAdded) return@inner

                                val lastDoc = msgSnap?.documents?.firstOrNull()
                                val lastMessage = lastDoc?.getString("text")
                                val timestamp = lastDoc?.getLong("timestamp")

                                if (lastMessage != null && timestamp != null) {
                                    chatSummariesMap[chatId] = ChatSummary(
                                        chatId = chatId,
                                        participants = participants,
                                        lastMessage = lastMessage,
                                        lastMessageTime = timestamp,
                                        unreadCount = unread.toInt()
                                    )

                                    val sortedList = chatSummariesMap.values
                                        .sortedByDescending { it.lastMessageTime }

                                    adapter.submitList(sortedList)
                                    recyclerView.isVisible = sortedList.isNotEmpty()
                                    emptyTextView.isVisible = sortedList.isEmpty()
                                }
                            }
                        messageListeners[chatId] = msgListener
                    }
                    progressIndicator.isVisible = false
                }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error saat memulai listener", e)
            emptyTextView.text = "Terjadi kesalahan. Coba lagi nanti."
            emptyTextView.isVisible = true
            recyclerView.isVisible = false
            progressIndicator.isVisible = false
        }
    }
}
