package com.student.chatify.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

object PresenceManager {

    private val db = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun initPresenceTracking() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        val userStatusRef = db.getReference("status/$uid")
        val connectedRef = db.getReference(".info/connected")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    userStatusRef.onDisconnect().setValue(
                        mapOf(
                            "state" to "offline",
                            "last_seen" to ServerValue.TIMESTAMP
                        )
                    )
                    userStatusRef.setValue(
                        mapOf(
                            "state" to "online",
                            "last_seen" to ServerValue.TIMESTAMP
                        )
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun observeUserPresence(
        uid: String,
        onUpdate: (isOnline: Boolean, lastSeen: Long) -> Unit
    ) {
        val ref = FirebaseDatabase.getInstance().getReference("status/$uid")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.child("state").getValue(String::class.java)
                val lastSeen = snapshot.child("last_seen").getValue(Long::class.java) ?: 0L
                onUpdate(state == "online", lastSeen)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun setOfflineManually() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        db.getReference("status/$uid").setValue(
            mapOf(
                "state" to "offline",
                "last_seen" to ServerValue.TIMESTAMP
            )
        )
    }
}
