package com.student.chatify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var btnGoogleSignIn: Button
    private lateinit var btnRegister: Button
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnRegister = findViewById(R.id.registerButton)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        progressBar = findViewById(R.id.progressBar)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        btnRegister.setOnClickListener {
            registerWithEmail()
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !show
        btnGoogleSignIn.isEnabled = !show
    }

    private fun registerWithEmail() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        if (email.isEmpty()) {
            emailEditText.error = "Email harus diisi"
            emailEditText.requestFocus()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Email tidak valid"
            emailEditText.requestFocus()
            return
        }
        if (password.isEmpty()) {
            passwordEditText.error = "Password harus diisi"
            passwordEditText.requestFocus()
            return
        }
        if (password.length < 6) {
            passwordEditText.error = "Password minimal 6 karakter"
            passwordEditText.requestFocus()
            return
        }
        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Password konfirmasi tidak cocok"
            confirmPasswordEditText.requestFocus()
            return
        }

        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val username = email.substringBefore("@")
                        val userMap = hashMapOf(
                            "email" to email,
                            "username" to username,
                            "displayName" to username,
                            "profileImage" to ""
                        )
                        firestore.collection("users").document(it.uid).set(userMap)
                            .addOnSuccessListener {
                                goToDashboard()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal menyimpan data user: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Register gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Register", "createUserWithEmailAndPassword failed", task.exception)
                }
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    firebaseAuthWithGoogle(idToken)
                } ?: run {
                    Toast.makeText(this, "Token Google tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in gagal: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                Log.e("GoogleSignIn", "ApiException code: ${e.statusCode}", e)
            }
        } else {
            Toast.makeText(this, "Google sign in dibatalkan", Toast.LENGTH_SHORT).show()
            Log.w("GoogleSignIn", "Result not OK or data is null")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        checkUserInFirestore(user.uid, user.email, user.displayName, user.photoUrl?.toString())
                    } else {
                        Toast.makeText(this, "User belum terautentikasi", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Firebase auth gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("FirebaseAuth", "signInWithCredential failed", task.exception)
                }
            }
    }

    private fun checkUserInFirestore(uid: String, email: String?, displayName: String?, photoUrl: String?) {
        val userDocRef = firestore.collection("users").document(uid)
        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    goToDashboard()
                } else {
                    val username = email?.substringBefore("@") ?: "user${System.currentTimeMillis()}"
                    val userMap = hashMapOf(
                        "email" to email,
                        "username" to username,
                        "displayName" to (displayName ?: username),
                        "profileImage" to (photoUrl ?: "")
                    )
                    userDocRef.set(userMap)
                        .addOnSuccessListener {
                            goToDashboard()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal simpan data user: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e("Firestore", "Failed to save user data", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error cek data user: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "Failed to get user data", e)
            }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
