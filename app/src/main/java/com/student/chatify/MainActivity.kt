package com.student.chatify

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User sudah login, langsung ke Dashboard
            startActivity(Intent(this, DashboardActivity::class.java))
        } else {
            // User belum login, ke Login screen
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish() // supaya MainActivity langsung tertutup dan tidak bisa back ke sini
    }
}
