package com.student.chatify.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.student.chatify.model.User
import kotlinx.coroutines.tasks.await

object UserManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    suspend fun createUserIfNotExists(): Boolean {
        val firebaseUser = auth.currentUser ?: return false
        val userRef = firestore.collection("users").document(firebaseUser.uid)
        val snapshot = userRef.get().await()
        if (snapshot.exists()) return true

        val username = firebaseUser.email?.substringBefore("@")?.lowercase()?.replace("\\s+".toRegex(), "") ?: "user${System.currentTimeMillis()}"
        val isAvailable = isUsernameAvailable(username)
        val finalUsername = if (isAvailable) username else "$username${(1000..9999).random()}"

        val user = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            username = finalUsername,
            displayName = firebaseUser.displayName ?: finalUsername,
            profileImage = firebaseUser.photoUrl?.toString() ?: "",
            statusMessage = "",
            createdAt = System.currentTimeMillis(),
            isOnline = true
        )
        userRef.set(user).await()
        return true
    }

    suspend fun getUser(uid: String): User? {
        return firestore.collection("users").document(uid).get().await().toObject(User::class.java)
    }

    suspend fun getFullUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        val firestoreUser = getUser(firebaseUser.uid) ?: return null

        return firestoreUser.copy(
            displayName = firebaseUser.displayName ?: firestoreUser.displayName,
            profileImage = firebaseUser.photoUrl?.toString() ?: firestoreUser.profileImage,
            email = firebaseUser.email ?: firestoreUser.email
        )
    }

    suspend fun updateUsername(newUsername: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val available = isUsernameAvailable(newUsername)
        if (!available) return false
        firestore.collection("users").document(uid).update("username", newUsername).await()
        return true
    }

    suspend fun updateDisplayName(newName: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        auth.currentUser?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(newName).build())?.await()
        firestore.collection("users").document(uid).update("displayName", newName).await()
        return true
    }

    suspend fun updateStatusMessage(message: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        firestore.collection("users").document(uid).update("statusMessage", message).await()
        return true
    }

    suspend fun updateProfileImage(uri: Uri): Boolean {
        val user = auth.currentUser ?: return false
        val request = UserProfileChangeRequest.Builder().setPhotoUri(uri).build()
        user.updateProfile(request).await()
        return true // Tidak perlu update Firestore karena diambil dari auth
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        val snapshot = firestore.collection("users").whereEqualTo("username", username).get().await()
        return snapshot.isEmpty
    }

    suspend fun updateLastSeen() {
        auth.currentUser?.uid?.let {
            firestore.collection("users").document(it).update("lastSeen", System.currentTimeMillis()).await()
        }
    }

    suspend fun setOnlineStatus(isOnline: Boolean) {
        auth.currentUser?.uid?.let {
            firestore.collection("users").document(it).update("isOnline", isOnline).await()
        }
    }
}