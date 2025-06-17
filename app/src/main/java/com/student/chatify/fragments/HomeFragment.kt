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
import com.google.firebase.firestore.Query
import com.student.chatify.ChatActivity
import com.student.chatify.R
import com.student.chatify.model.ChatListItem
import com.student.chatify.recyclerView.ChatListAdapter

class HomeFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatListAdapter
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var emptyTextView: MaterialTextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.chatListRecyclerView)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        emptyTextView = view.findViewById(R.id.emptyTextView)

        adapter = ChatListAdapter { chatItem ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("chatId", chatItem.chatId)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadChatList()
    }

    private fun loadChatList() {
        val uid = auth.currentUser?.uid ?: return

        progressIndicator.isVisible = true
        recyclerView.isVisible = false
        emptyTextView.isVisible = false

        db.collection("chats")
            .whereArrayContains("participants", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                progressIndicator.isVisible = false

                if (error != null || snapshot == null) {
                    emptyTextView.text = "Gagal memuat daftar chat."
                    emptyTextView.isVisible = true
                    return@addSnapshotListener
                }

                val chatList = snapshot.documents.mapNotNull { doc ->
                    val lastMsg = doc.getString("lastMessage") ?: ""
                    val name = doc.getString("chatName") ?: "Chat"
                    val time = doc.getLong("timestamp") ?: 0L

                    ChatListItem(
                        chatId = doc.id,
                        chatName = name,
                        lastMessage = lastMsg,
                        timestamp = time
                    )
                }

                recyclerView.isVisible = chatList.isNotEmpty()
                emptyTextView.isVisible = chatList.isEmpty()
                emptyTextView.text = "Tidak ada riwayat chat."

                adapter.submitList(chatList)
            }
    }
}
