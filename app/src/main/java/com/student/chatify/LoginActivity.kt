package com.student.chatify

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var googleSignInButton: Button
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var errorTextView: TextView

    companion object {
        private const val TAG = "LoginActivity"
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                account?.idToken?.let { idToken ->
                    firebaseAuthWithGoogle(idToken)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google sign in failed", e)
                showError("Google sign-in gagal: ${e.message}")
            }
        } else {
            showError("Google sign-in dibatalkan")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        googleSignInButton = findViewById(R.id.googleSignInButton)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        errorTextView = findViewById(R.id.errorTextView)

        // Setup Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Pastikan pengguna memilih akun Google saat login
        googleSignInButton.setOnClickListener {
            // Menampilkan progress loading
            showLoading(true)
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Google berhasil", Toast.LENGTH_SHORT).show()
                    startDashboard()
                } else {
                    showError("Login Google gagal: ${task.exception?.message}")
                }
            }
    }

    private fun showLoading(loading: Boolean) {
        loadingProgressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        googleSignInButton.isEnabled = !loading
    }

    private fun showError(message: String) {
        errorTextView.text = message
        errorTextView.visibility = android.view.View.VISIBLE
    }

    private fun startDashboard() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
