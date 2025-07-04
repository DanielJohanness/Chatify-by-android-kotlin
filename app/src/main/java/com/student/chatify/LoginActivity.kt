// 📁 LoginActivity.kt (update dengan UserManager)
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
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.student.chatify.data.UserManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var googleSignInButton: Button
    private lateinit var anonymousLoginButton: Button
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
        anonymousLoginButton = findViewById(R.id.anonymousLoginButton)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        errorTextView = findViewById(R.id.errorTextView)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInButton.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener(this) {
                showLoading(true)
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        anonymousLoginButton.setOnClickListener {
            showLoading(true)
            firebaseAuthWithAnonymous()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    lifecycleScope.launch {
                        UserManager.createUserIfNotExists()
                        showLoading(false)
                        Toast.makeText(this@LoginActivity, "Login Google berhasil", Toast.LENGTH_SHORT).show()
                        startDashboard()
                    }
                } else {
                    showLoading(false)
                    showError("Login Google gagal: ${task.exception?.message}")
                }
            }
    }

    private fun firebaseAuthWithAnonymous() {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    lifecycleScope.launch {
                        UserManager.createUserIfNotExists()
                        showLoading(false)
                        Toast.makeText(this@LoginActivity, "Login anonim berhasil", Toast.LENGTH_SHORT).show()
                        startDashboard()
                    }
                } else {
                    showLoading(false)
                    showError("Login anonim gagal: ${task.exception?.message}")
                }
            }
    }

    private fun showLoading(loading: Boolean) {
        loadingProgressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        googleSignInButton.isEnabled = !loading
        anonymousLoginButton.isEnabled = !loading
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