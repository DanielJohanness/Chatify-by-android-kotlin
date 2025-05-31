package com.student.chatify.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.student.chatify.R
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var containerLayout: LinearLayout
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        containerLayout = view.findViewById(R.id.containerLayout)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        loadLastMessages()

        return view
    }

    private fun loadLastMessages() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("chats")
            .get()
            .addOnSuccessListener { chatSnapshots ->
                if (chatSnapshots.isEmpty) {
                    showNoMessageText()
                    return@addOnSuccessListener
                }

                var hasMessage = false
                var pending = chatSnapshots.size()

                for (chatDoc in chatSnapshots) {
                    val chatId = chatDoc.id

                    firestore.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { messagesSnapshot ->
                            val messageDoc = messagesSnapshot.documents.firstOrNull()
                            pending--

                            if (messageDoc != null) {
                                hasMessage = true
                                val senderId = messageDoc.getString("senderId") ?: return@addOnSuccessListener
                                val receiverId = messageDoc.getString("receiverId") ?: return@addOnSuccessListener

                                val otherUserId = if (senderId == currentUserId) receiverId else senderId
                                val messageText = messageDoc.getString("content") ?: ""
                                val timestamp = messageDoc.getTimestamp("timestamp")?.toDate()

                                firestore.collection("users")
                                    .document(otherUserId)
                                    .get()
                                    .addOnSuccessListener { userDoc ->
                                        val name = userDoc.getString("displayName") ?: "Pengguna"
                                        val timeString = timestamp?.let { dateFormat.format(it) } ?: "Waktu tidak tersedia"

                                        val textView = TextView(requireContext()).apply {
                                            text = "$name\n\"$messageText\"\n$timeString"
                                            textSize = 16f
                                            setPadding(24, 24, 24, 24)
                                            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                                        }

                                        containerLayout.addView(textView)
                                    }
                            }

                            // Jika sudah cek semua chat dan tidak ada pesan yang valid
                            if (pending == 0 && !hasMessage) {
                                showNoMessageText()
                            }
                        }
                        .addOnFailureListener {
                            pending--
                            if (pending == 0 && !hasMessage) {
                                showNoMessageText()
                            }
                        }
                }
            }
            .addOnFailureListener {
                Log.e("HomeFragment", "Gagal ambil daftar chat: ${it.message}")
                showNoMessageText()
            }
    }
    private fun showNoMessageText() {
        val noDataView = TextView(requireContext()).apply {
            text = "Belum ada riwayat pesan"
            textSize = 18f
            setPadding(32, 64, 32, 32)
            gravity = Gravity.CENTER
        }
        containerLayout.addView(noDataView)
    }
}
