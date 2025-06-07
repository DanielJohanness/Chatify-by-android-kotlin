package com.student.chatify.fragments

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.student.chatify.LoginActivity
import com.student.chatify.R

class ProfileFragment : Fragment() {

    private lateinit var oneTapClient: SignInClient
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(requireContext())

        val profileImageView: ImageView = view.findViewById(R.id.profileImageView)
        val welcomeTextView: TextView = view.findViewById(R.id.welcomeTextView)
        val emailTextView: TextView = view.findViewById(R.id.emailTextView)
        val logoutButton: Button = view.findViewById(R.id.logoutButton)

        val user = auth.currentUser

        welcomeTextView.text = "Selamat datang, ${user?.displayName ?: user?.email ?: "User"}"
        emailTextView.text = user?.email ?: "Email tidak tersedia"

        val photoUrl = user?.photoUrl
        Log.d("ProfileFragment", "photoUrl = $photoUrl")

        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .circleCrop()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("Glide", "Load photo profile failed", e)
                        return false // biarkan Glide set error drawable
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("Glide", "Load photo profile success")
                        return false
                    }
                })
                .into(profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.ic_default_profile)
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            oneTapClient.signOut().addOnCompleteListener {
                Toast.makeText(requireContext(), "Logout berhasil", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireActivity(), LoginActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }

        return view
    }
}
