package com.student.chatify.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.student.chatify.EditProfileActivity
import com.student.chatify.LoginActivity
import com.student.chatify.R
import com.student.chatify.data.UserManager
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient

    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var idTextView: TextView
    private lateinit var aboutTextView: TextView
    private lateinit var chipNameId: TextView
    private lateinit var editProfileText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(requireContext())

        profileImageView = view.findViewById(R.id.profileImageView)
        nameTextView = view.findViewById(R.id.nameTextView)
        idTextView = view.findViewById(R.id.idTextView)
        aboutTextView = view.findViewById(R.id.aboutTextView)
        chipNameId = view.findViewById(R.id.chipNameId)
        editProfileText = view.findViewById(R.id.editPhotoText)

        val logoutButton = view.findViewById<ImageView>(R.id.logoutIcon)
        val nameRow = view.findViewById<LinearLayout>(R.id.nameRow)
        val idRow = view.findViewById<LinearLayout>(R.id.idRow)
        val aboutRow = view.findViewById<LinearLayout>(R.id.aboutRow)

        val uid = auth.currentUser?.uid ?: return

        loadUserData(uid)

        val intent = Intent(requireContext(), EditProfileActivity::class.java)
        nameRow.setOnClickListener { startActivity(intent) }
        idRow.setOnClickListener { startActivity(intent) }
        aboutRow.setOnClickListener { startActivity(intent) }
        editProfileText.setOnClickListener { startActivity(intent) }

        logoutButton.setOnClickListener {
            auth.signOut()
            oneTapClient.signOut().addOnCompleteListener {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        auth.currentUser?.uid?.let { loadUserData(it) }
    }

    private fun loadUserData(uid: String) {
        lifecycleScope.launch {
            val user = UserManager.getFullUser()
            if (user != null) {
                nameTextView.text = "Name:\n${user.displayName}"
                idTextView.text = "ID:\n@${user.username}"
                aboutTextView.text = "About:\n${user.statusMessage.ifBlank { "Can’t talk, Chatify only" }}"
                chipNameId.text = "${user.displayName} | @${user.username}"

                Glide.with(this@ProfileFragment)
                    .load(user.profileImage)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .circleCrop()
                    .into(profileImageView)
            } else {
                Toast.makeText(requireContext(), "Gagal memuat profil", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
