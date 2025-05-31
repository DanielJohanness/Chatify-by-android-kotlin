package com.student.chatify.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.student.chatify.R

class AddFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var etUsername: EditText
    private lateinit var btnAddContact: Button
    private lateinit var tvResult: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add, container, false)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        etUsername = view.findViewById(R.id.etUsername)
        btnAddContact = view.findViewById(R.id.btnAddContact)
        tvResult = view.findViewById(R.id.tvResult)

        btnAddContact.setOnClickListener {
            val username = etUsername.text.toString().trim()
            if (username.isEmpty()) {
                tvResult.text = "Username tidak boleh kosong"
                return@setOnClickListener
            }
            addContactByUsername(username)
        }

        return view
    }

    private fun addContactByUsername(username: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            tvResult.text = "User belum login"
            return
        }

        val currentUserId = currentUser.uid
        val currentUsername = currentUser.displayName ?: ""

        // Cek agar tidak menambahkan diri sendiri (case insensitive)
        if (username.equals(currentUsername, ignoreCase = true)) {
            tvResult.text = "Tidak bisa menambahkan diri sendiri"
            return
        }

        tvResult.text = "Mencari user..."

        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    tvResult.text = "User dengan username '$username' tidak ditemukan"
                    return@addOnSuccessListener
                }

                val userDoc = snapshot.documents[0]
                val targetUserId = userDoc.id

                if (targetUserId == currentUserId) {
                    tvResult.text = "Tidak bisa menambahkan diri sendiri"
                    return@addOnSuccessListener
                }

                // Simpan kontak di sub-collection user kita, misal "contacts"
                firestore.collection("users")
                    .document(currentUserId)
                    .collection("contacts")
                    .document(targetUserId)
                    .set(
                        mapOf(
                            "username" to username,
                            "displayName" to (userDoc.getString("displayName") ?: ""),
                            "profileImage" to (userDoc.getString("profileImage") ?: "")
                        )
                    )
                    .addOnSuccessListener {
                        tvResult.text = "Kontak '$username' berhasil ditambahkan"
                    }
                    .addOnFailureListener {
                        tvResult.text = "Gagal menambahkan kontak: ${it.message}"
                    }
            }
            .addOnFailureListener {
                tvResult.text = "Error mencari user: ${it.message}"
            }
    }
}
