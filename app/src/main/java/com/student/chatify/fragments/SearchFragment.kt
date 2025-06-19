package com.student.chatify.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.student.chatify.R
import com.student.chatify.data.repository.ChatRepository
import com.student.chatify.model.User
import com.student.chatify.recyclerView.UserListAdapter
import com.student.chatify.ui.chat.ChatActivity
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserListAdapter
    private lateinit var etSearch: EditText

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatRepo = ChatRepository()

    private var fullUserList: List<User> = emptyList()
    private var isProcessingClick = false
    private var userListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        recyclerView = view.findViewById(R.id.userRecyclerView)
        etSearch = view.findViewById(R.id.etSearch)

        adapter = UserListAdapter { user -> startChatWithUser(user) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fetchUsers()
        setupSearchFilter()

        return view
    }

    private fun fetchUsers() {
        val currentUid = auth.currentUser?.uid ?: return

        userListener?.remove()
        userListener = db.collection("users").document(currentUid)
            .collection("contacts")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !isAdded) return@addSnapshotListener

                val contactUsers = snapshot.documents.mapNotNull { doc ->
                    val user = User(
                        uid = doc.id,
                        username = doc.getString("username") ?: "",
                        displayName = doc.getString("displayName") ?: "",
                        profileImage = doc.getString("profileImage") ?: "",
                        statusMessage = doc.getString("statusMessage") ?: "",
                        isOnline = false,
                        lastSeen = 0L
                    )
                    user
                }

                fullUserList = contactUsers
                filterAndDisplayUsers(etSearch.text.toString())
            }
    }

    private fun setupSearchFilter() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterAndDisplayUsers(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filterAndDisplayUsers(queryRaw: String) {
        val query = queryRaw.trim().lowercase()
        val filtered = if (query.isEmpty()) fullUserList else fullUserList.filter {
            it.displayName.lowercase().contains(query) || it.username.lowercase().contains(query)
        }
        adapter.submitList(filtered)
    }

    private fun startChatWithUser(user: User) {
        val currentUser = auth.currentUser ?: return
        if (isProcessingClick || !isAdded) return

        val currentUid = currentUser.uid
        val context = context ?: return
        isProcessingClick = true

        // Simpan ke kontak
        db.collection("users").document(currentUid)
            .collection("contacts").document(user.uid)
            .set(
                mapOf(
                    "username" to user.username,
                    "displayName" to user.displayName,
                    "profileImage" to user.profileImage,
                    "statusMessage" to user.statusMessage
                )
            )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val chatId = chatRepo.startOrCreateChat(currentUid, user.uid)
                if (!isAdded || chatId == null) return@launch

                startActivity(
                    Intent(context, ChatActivity::class.java).apply {
                        putExtra("chatId", chatId)
                        putExtra("otherUserUid", user.uid)
                    }
                )
            } finally {
                isProcessingClick = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        userListener = null
    }
}