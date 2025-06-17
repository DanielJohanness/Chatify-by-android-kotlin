package com.student.chatify

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private val tabOrder = listOf(
        R.id.homeFragment,
        R.id.searchFragment,
        R.id.addFragment,
        R.id.callLogsFragment,
        R.id.profileFragment
    )
    private var currentTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }else{
            startActivity(Intent(this, AIChatActivity::class.java))
            finish()
            return
        }

//        val uid = currentUser.uid
//        val otherUserUid = if (uid == "aoEcAo4NLwgKpDOKUjH4VHAEeQq1") {
//            "K7tK613mCkNtYidWM9OCoa91NHj1"
//        } else {
//            "aoEcAo4NLwgKpDOKUjH4VHAEeQq1"
//        }
//
//        val goToChat = true
//        if (goToChat) {
//            val intent = Intent(this, ChatActivity::class.java)
//            intent.putExtra("otherUserUid", otherUserUid) // Kirim otherUserUid, bukan chatId!
//            startActivity(intent)
//            finish()
//            return
//        }

        // Kalau tidak ke ChatActivity, baru tampilkan MainActivity UI
        setContentView(R.layout.activity_main)

        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        findViewById<BottomNavigationView>(R.id.bottom_navigation).setOnItemSelectedListener { item ->
            val targetIndex = tabOrder.indexOf(item.itemId)
            val currentIndex = tabOrder.indexOf(navController.currentDestination?.id)
            if (targetIndex == currentIndex || targetIndex == -1) return@setOnItemSelectedListener false

            val isForward = targetIndex > currentIndex

            val anim = NavOptions.Builder()
                .setEnterAnim(if (isForward) R.anim.slide_in_right else R.anim.slide_in_left)
                .setExitAnim(if (isForward) R.anim.slide_out_left else R.anim.slide_out_right)
                .setPopEnterAnim(if (isForward) R.anim.slide_in_left else R.anim.slide_in_right)
                .setPopExitAnim(if (isForward) R.anim.slide_out_right else R.anim.slide_out_left)
                .setLaunchSingleTop(true)
                .build()

            navController.navigate(item.itemId, null, anim)
            currentTabIndex = targetIndex
            true
        }
    }
}
