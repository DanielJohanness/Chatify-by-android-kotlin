package com.student.chatify

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(this)

        val profileImageView: ImageView = findViewById(R.id.profileImageView)
        val welcomeTextView: TextView = findViewById(R.id.welcomeTextView)
        val emailTextView: TextView = findViewById(R.id.emailTextView)
        val logoutButton: Button = findViewById(R.id.logoutButton)
        val editProfileButton: Button = findViewById(R.id.editProfileButton)  // Tombol Edit Profil

        val user = auth.currentUser

        val username = when {
            user == null -> "Pengguna"
            user.isAnonymous -> "Tamu${user.uid.takeLast(4)}"
            !user.displayName.isNullOrBlank() -> user.displayName!!
            !user.email.isNullOrBlank() -> user.email!!.substringBefore("@")
            else -> "Pengguna"
        }

        val emailText = when {
            user == null -> "Email tidak tersedia"
            user.isAnonymous -> "Masuk sebagai tamu"
            !user.email.isNullOrBlank() -> user.email!!
            else -> "Email tidak tersedia"
        }

        welcomeTextView.text = "Halo, $username"
        emailTextView.text = emailText

        val photoUrl = user?.photoUrl
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .circleCrop()
                .into(profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.ic_default_profile)
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            oneTapClient.signOut().addOnCompleteListener {
                Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        // Menangani klik tombol Edit Profil
        editProfileButton.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
    }
}
