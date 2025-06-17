package com.student.chatify.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.student.chatify.LoginActivity
import com.student.chatify.R

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(requireContext())

        val profileImageView = view.findViewById<ImageView>(R.id.profileImageView)
        val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
        val idTextView = view.findViewById<TextView>(R.id.idTextView)
        val aboutTextView = view.findViewById<TextView>(R.id.aboutTextView)
        val chipNameId = view.findViewById<TextView>(R.id.chipNameId)
        val logoutButton = view.findViewById<ImageView>(R.id.logoutIcon)

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
                Toast.makeText(requireContext(), "Logout berhasil", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                activity?.finish()
            }
        }
    }
}
