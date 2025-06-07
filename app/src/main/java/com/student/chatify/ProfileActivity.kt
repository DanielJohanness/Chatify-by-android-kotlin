package com.student.chatify

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
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
        val nameTextView: TextView = findViewById(R.id.nameTextView)
        val idTextView: TextView = findViewById(R.id.idTextView)
        val aboutTextView: TextView = findViewById(R.id.aboutTextView)
        val chipNameId: TextView = findViewById(R.id.chipNameId)
        val logoutButton: ImageView = findViewById(R.id.logoutIcon)

        val user = auth.currentUser

        val name = when {
            user == null -> "Pengguna"
            user.isAnonymous -> "Tamu${user.uid.takeLast(4)}"
            !user.displayName.isNullOrBlank() -> user.displayName!!
            !user.email.isNullOrBlank() -> user.email!!.substringBefore("@")
            else -> "Pengguna"
        }

        val id = "@${user?.email?.substringBefore("@") ?: "anonymous"}"
        val about = "Can’t talk, Chatify only"

        // Set ke TextView
        nameTextView.text = "Name:\n$name"
        idTextView.text = "ID:\n$id"
        aboutTextView.text = "About:\n$about"
        chipNameId.text = "$name | $id"

        val photoUrl = user?.photoUrl
        Glide.with(this)
            .load(photoUrl)
            .placeholder(R.drawable.ic_default_profile)
            .error(R.drawable.ic_default_profile)
            .circleCrop()
            .into(profileImageView)

        logoutButton.setOnClickListener {
            auth.signOut()
            oneTapClient.signOut().addOnCompleteListener {
                Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }
}
