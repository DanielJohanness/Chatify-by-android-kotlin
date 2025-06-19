package com.student.chatify.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.student.chatify.R
import com.student.chatify.data.repository.ChatRepository
import com.student.chatify.ui.chat.ChatActivity
import kotlinx.coroutines.launch

class AddFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var chatRepo: ChatRepository

    private lateinit var etUsername: EditText
    private lateinit var btnAddContact: Button
    private lateinit var tvResult: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add, container, false)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        chatRepo = ChatRepository(firestore, auth)

        etUsername = view.findViewById(R.id.etUsername)
        btnAddContact = view.findViewById(R.id.btnAddContact)
        tvResult = view.findViewById(R.id.tvResult)

        btnAddContact.setOnClickListener {
            val username = etUsername.text.toString().trim()
            if (username.isEmpty()) {
                tvResult.text = "Username tidak boleh kosong"
                return@setOnClickListener
            }
            findAndStartChat(username)
        }

        return view
    }

    private fun findAndStartChat(username: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            tvResult.text = "User belum login"
            return
        }

        val currentUid = currentUser.uid
        tvResult.text = "Mencari pengguna..."

        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    tvResult.text = "Pengguna dengan username '$username' tidak ditemukan"
                    return@addOnSuccessListener
                }

                val userDoc = snapshot.documents[0]
                val targetUid = userDoc.id

                if (targetUid == currentUid) {
                    tvResult.text = "Tidak bisa mengobrol dengan diri sendiri"
                    return@addOnSuccessListener
                }

                // Simpan ke kontak (opsional)
                firestore.collection("users").document(currentUid)
                    .collection("contacts").document(targetUid)
                    .set(
                        mapOf(
                            "username" to username,
                            "displayName" to (userDoc.getString("displayName") ?: ""),
                            "profileImage" to (userDoc.getString("profileImage") ?: "")
                        )
                    )

                viewLifecycleOwner.lifecycleScope.launch {
                    tvResult.text = "Membuka percakapan..."
                    val chatId = chatRepo.startOrCreateChat(currentUid, targetUid)
                    if (!isAdded) return@launch
                    if (chatId != null) {
                        tvResult.text = ""
                        context?.let {
                            val intent = Intent(it, ChatActivity::class.java).apply {
                                putExtra("chatId", chatId)
                                putExtra("otherUserUid", targetUid)
                            }
                            startActivity(intent)
                        }
                    } else {
                        tvResult.text = "Gagal membuka percakapan"
                    }
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    tvResult.text = "Gagal mencari pengguna: ${it.message}"
                }
            }
    }
}
