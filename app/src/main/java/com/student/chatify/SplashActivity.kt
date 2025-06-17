//package com.student.chatify
//
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import androidx.appcompat.app.AppCompatActivity
//import com.google.firebase.Firebase
//import com.google.firebase.FirebaseApp
//import com.google.firebase.appcheck.appCheck
//import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
//import com.google.firebase.auth.FirebaseAuth
//
//class SplashActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        if (FirebaseApp.getApps(this).isEmpty()) {
//            FirebaseApp.initializeApp(this)
//        }
//
//        // Install App Check provider - Play Integrity untuk release
//        Firebase.appCheck.installAppCheckProviderFactory(
//            DebugAppCheckProviderFactory.getInstance(),
////            PlayIntegrityAppCheckProviderFactory()
//        )
//
//        // Dapatkan debug token dan cetak ke Logcat
//        Firebase.appCheck.getToken(false)
//            .addOnSuccessListener { tokenResponse ->
//                Log.d("AppCheckDebug", "Debug App Check Token: ${tokenResponse.token}")
//            }
//
//        val currentUser = FirebaseAuth.getInstance().currentUser
//        if (currentUser != null) {
//            val intent = Intent(this, ChatActivity::class.java)
//            val uid = currentUser.uid
//            if(uid == "aoEcAo4NLwgKpDOKUjH4VHAEeQq1"){
//                intent.putExtra("chatId", "K7tK613mCkNtYidWM9OCoa91NHj1")
//            }else{
//                intent.putExtra("chatId", "aoEcAo4NLwgKpDOKUjH4VHAEeQq1")
//            }
//            startActivity(intent)
////            startActivity(Intent(this, MainActivity::class.java))
//        } else {
//            startActivity(Intent(this, LoginActivity::class.java))
//        }
//        finish()
//    }
//}
