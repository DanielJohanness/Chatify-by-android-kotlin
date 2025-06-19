package com.student.chatify.fragments

import android.content.Intent
import android.os.Bundle
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
import com.student.chatify.ui.chat.ChatActivity
import com.student.chatify.R
import com.student.chatify.model.ChatSummary
import com.student.chatify.recyclerView.ChatListAdapter

// HomeFragment.kt
class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatListAdapter
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var emptyTextView: MaterialTextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var chatListener: ListenerRegistration? = null
    private var currentUid: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.chatListRecyclerView)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        emptyTextView = view.findViewById(R.id.emptyTextView)

        currentUid = auth.currentUser?.uid

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
    }

    private fun setupRecyclerView() {
        adapter = ChatListAdapter(currentUid!!) { chatItem ->
            val otherUserUid = chatItem.participants.firstOrNull { it != currentUid } ?: return@ChatListAdapter
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("chatId", chatItem.chatId)
                putExtra("otherUserUid", otherUserUid)
            }
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun listenToChatList() {
        progressIndicator.isVisible = true
        recyclerView.isVisible = false
        emptyTextView.isVisible = false

        chatListener = db.collection("chats")
            .whereArrayContains("participants", currentUid!!)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    emptyTextView.text = "Gagal memuat daftar chat."
                    emptyTextView.isVisible = true
                    progressIndicator.isVisible = false
                    return@addSnapshotListener
                }

                val chatList = snapshot.documents.mapNotNull { doc ->
                    val participants = doc.get("participants") as? List<String> ?: return@mapNotNull null
                    val lastMsg = doc.getString("lastMessage") ?: ""
                    val time = doc.getLong("lastMessageTime") ?: 0L

                    val unreadCounts = doc.get("unreadCounts") as? Map<String, Long>
                    val unread = unreadCounts?.get(currentUid)?.toInt() ?: 0

                    ChatSummary(
                        chatId = doc.id,
                        participants = participants,
                        lastMessage = lastMsg,
                        lastMessageTime = time,
                        unreadCount = unread
                    )
                }

                adapter.submitList(chatList)
                progressIndicator.isVisible = false
                recyclerView.isVisible = chatList.isNotEmpty()
                emptyTextView.isVisible = chatList.isEmpty()
                emptyTextView.text = "Tidak ada riwayat chat."
            }
    }
}
