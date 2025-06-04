package com.student.chatify

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    private lateinit var oneTapClient: SignInClient
    private lateinit var auth: FirebaseAuth

    private lateinit var welcomeTextView: TextView
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(this)

        welcomeTextView = findViewById(R.id.welcomeTextView)
        logoutButton = findViewById(R.id.logoutButton)

        val user = auth.currentUser
        if (user != null) {
            welcomeTextView.text = "Selamat datang, ${user.email ?: user.displayName ?: "User"}"
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        // Logout Firebase
        auth.signOut()

        // Logout Google Identity Services
        oneTapClient.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Logout gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
