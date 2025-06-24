package com.student.chatify

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.student.chatify.data.PresenceManager
import com.student.chatify.data.UserManager
import kotlinx.coroutines.launch

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

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

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
        PresenceManager.initPresenceTracking()
    }
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            UserManager.setOnlineStatus(true)
        }
    }

    override fun onPause() {
        super.onPause()

        // Update ke Firestore
        lifecycleScope.launch {
            UserManager.setOnlineStatus(false)
            UserManager.updateLastSeen()
        }

        // Update manual ke Realtime Database
        PresenceManager.setOfflineManually()
    }

}