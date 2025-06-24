package com.student.chatify

import android.app.Application
import com.google.firebase.FirebaseApp
import com.student.chatify.data.AppLifecycleObserver

class ChatifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        AppLifecycleObserver.init(this)
    }
}
