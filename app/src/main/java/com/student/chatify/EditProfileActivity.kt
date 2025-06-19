package com.student.chatify

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.student.chatify.data.UserManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var profileImageView: ImageView
    private lateinit var editPhotoText: TextView
    private lateinit var displayNameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var statusEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null

    // Modern image picker launcher
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            profileImageView.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()

        profileImageView = findViewById(R.id.profileImageView)
        editPhotoText = findViewById(R.id.editPhotoText)
        displayNameEditText = findViewById(R.id.displayNameEditText)
        usernameEditText = findViewById(R.id.usernameEditText)
        statusEditText = findViewById(R.id.statusEditText)
        emailEditText = findViewById(R.id.emailEditText)
        saveButton = findViewById(R.id.saveButton)
        progressBar = findViewById(R.id.progressBar)

        lifecycleScope.launch {
            val userData = UserManager.getFullUser()
            if (userData != null) {
                displayNameEditText.setText(userData.displayName)
                usernameEditText.setText(userData.username)
                usernameEditText.tag = userData.username
                statusEditText.setText(userData.statusMessage)
                emailEditText.setText(userData.email)

                Glide.with(this@EditProfileActivity)
                    .load(userData.profileImage)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .circleCrop()
                    .into(profileImageView)
            }
        }

        // Buka image picker saat klik foto atau teks
        val openImagePicker = { imagePickerLauncher.launch("image/*") }
        profileImageView.setOnClickListener { openImagePicker() }
        editPhotoText.setOnClickListener { openImagePicker() }

        saveButton.setOnClickListener { saveProfile() }
    }

    private fun saveProfile() {
        val name = displayNameEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim().lowercase()
        val status = statusEditText.text.toString().trim()

        if (name.isBlank()) {
            displayNameEditText.error = "Nama tidak boleh kosong"
            return
        }
        if (username.isBlank()) {
            usernameEditText.error = "Username tidak boleh kosong"
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            val currentUser = auth.currentUser ?: return@launch

            val usernameChanged = username != usernameEditText.tag
            val usernameValid = if (usernameChanged) {
                val available = UserManager.isUsernameAvailable(username)
                if (!available) {
                    showLoading(false)
                    usernameEditText.error = "Username sudah digunakan"
                    return@launch
                }
                UserManager.updateUsername(username)
            } else true

            val nameSuccess = UserManager.updateDisplayName(name)
            val statusSuccess = UserManager.updateStatusMessage(status)

            val imageSuccess = selectedImageUri?.let {
                val url = uploadImageToStorage(it, currentUser.uid)
                if (url != null) UserManager.updateProfileImage(url) else false
            } ?: true

            showLoading(false)

            if (usernameValid && nameSuccess && statusSuccess && imageSuccess) {
                Toast.makeText(this@EditProfileActivity, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@EditProfileActivity, "Sebagian data gagal diperbarui", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) ProgressBar.VISIBLE else ProgressBar.GONE
        saveButton.isEnabled = !isLoading
    }

    private suspend fun uploadImageToStorage(uri: Uri, uid: String): Uri? {
        return try {
            val ref = FirebaseStorage.getInstance()
                .reference.child("profileImages/$uid-${UUID.randomUUID()}.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await()
        } catch (e: Exception) {
            null
        }
    }
}
