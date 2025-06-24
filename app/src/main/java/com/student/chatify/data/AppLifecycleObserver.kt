package com.student.chatify.data

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

object AppLifecycleObserver : DefaultLifecycleObserver {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    fun init(application: Application) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // App ke foreground
        val user = auth.currentUser ?: return
        val ref = db.getReference("status/${user.uid}")
        ref.setValue(
            mapOf(
                "state" to "online",
                "last_seen" to ServerValue.TIMESTAMP
            )
        )
    }

    override fun onStop(owner: LifecycleOwner) {
        // App ke background
        val user = auth.currentUser ?: return
        val ref = db.getReference("status/${user.uid}")
        ref.setValue(
            mapOf(
                "state" to "offline",
                "last_seen" to ServerValue.TIMESTAMP
            )
        )
    }
}
