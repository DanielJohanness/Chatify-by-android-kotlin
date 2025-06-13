package com.student.chatify

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var profileImageView: ImageView
    private lateinit var displayNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var updateProfileButton: Button

    private var profileImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Inisialisasi Views
        profileImageView = findViewById(R.id.profileImageView)
        displayNameEditText = findViewById(R.id.displayNameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        updateProfileButton = findViewById(R.id.updateProfileButton)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            emailEditText.setText(currentUser.email)
            displayNameEditText.setText(currentUser.displayName)

            // Jika pengguna sudah punya gambar profil, tampilkan
            if (currentUser.photoUrl != null) {
                Picasso.get().load(currentUser.photoUrl).into(profileImageView)
            }
        }

        // Gambar Profil Klik
        profileImageView.setOnClickListener {
            openImageChooser()
        }

        // Tombol Update Profile
        updateProfileButton.setOnClickListener {
            updateProfile()
        }
    }

    // Fungsi untuk memilih gambar profil
    private fun openImageChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    // Menangani hasil pemilihan gambar
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            profileImageUri = data.data
            profileImageView.setImageURI(profileImageUri)
        }
    }

    // Update Profil Pengguna
    private fun updateProfile() {
        val displayName = displayNameEditText.text.toString()
        if (displayName.isEmpty()) {
            Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser

        // Memperbarui Nama Pengguna
        val userProfileChangeRequest = currentUser?.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .setPhotoUri(profileImageUri)  // Gunakan foto URI jika ada
                .build()
        )

        userProfileChangeRequest?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Hanya upload gambar profil jika ada gambar yang dipilih
                profileImageUri?.let { uploadProfileImage(it) }

                Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                finish() // Kembali ke halaman sebelumnya
            } else {
                Toast.makeText(this, "Gagal memperbarui profil", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Mengupload Gambar Profil ke Firebase Storage
    private fun uploadProfileImage(uri: Uri) {
        val storageReference: StorageReference = storage.reference
        val userProfileImageRef = storageReference.child("profile_images/${auth.currentUser?.uid}.jpg")

        val uploadTask = userProfileImageRef.putFile(uri)
        uploadTask.addOnCompleteListener(OnCompleteListener { task ->
            if (task.isSuccessful) {
                // Mendapatkan URL gambar setelah upload selesai
                userProfileImageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Update gambar profil di Firebase Authentication dan Firestore
                    updateProfileImageInAuth(downloadUri)
                    updateFirestoreProfileImage(downloadUri.toString())
                }
            } else {
                Log.e("EditProfile", "Gagal mengupload gambar: ${task.exception?.message}")
                Toast.makeText(this, "Gagal mengupload gambar profil", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Memperbarui URL gambar profil di Firebase Authentication
    private fun updateProfileImageInAuth(imageUri: Uri) {
        val currentUser = auth.currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setPhotoUri(imageUri)
            .build()

        currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("EditProfile", "Foto profil berhasil diperbarui di Firebase Authentication")
            } else {
                Log.e("EditProfile", "Gagal memperbarui foto profil di Firebase Authentication")
            }
        }
    }

    // Memperbarui URL gambar profil di Firestore
    private fun updateFirestoreProfileImage(imageUrl: String) {
        val userRef = firestore.collection("users").document(auth.currentUser!!.uid)

        userRef.update("profileImage", imageUrl)
            .addOnSuccessListener {
                Log.d("EditProfile", "URL gambar profil berhasil diperbarui di Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("EditProfile", "Gagal memperbarui URL gambar profil di Firestore: ${e.message}")
            }
    }
}
