//package com.student.chatify
//
//import android.content.Intent
//import android.graphics.Color
//import android.os.Build
//import android.os.Bundle
//import android.util.TypedValue
//import android.view.View
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import androidx.navigation.NavOptions
//import androidx.navigation.fragment.NavHostFragment
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import com.google.firebase.auth.FirebaseAuth
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var auth: FirebaseAuth
//
//    private val tabOrder = listOf(
//        R.id.homeFragment,
//        R.id.searchFragment,
//        R.id.addFragment,
//        R.id.callLogsFragment,
//        R.id.profileFragment
//    )
//    private var currentTabIndex = 0
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        auth = FirebaseAuth.getInstance()
//
//        if (auth.currentUser == null) {
//            startActivity(Intent(this, LoginActivity::class.java))
//            finish()
//            return
//        }
//
//        setContentView(R.layout.activity_main)
//        setSystemBarsColorFromTheme()
//
//        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
//
//        findViewById<BottomNavigationView>(R.id.bottom_navigation).setOnItemSelectedListener { item ->
//            val targetIndex = tabOrder.indexOf(item.itemId)
//            val currentIndex = tabOrder.indexOf(navController.currentDestination?.id)
//            if (targetIndex == currentIndex || targetIndex == -1) return@setOnItemSelectedListener false
//
//            val isForward = targetIndex > currentIndex
//
//            val anim = NavOptions.Builder()
//                .setEnterAnim(if (isForward) R.anim.slide_in_right else R.anim.slide_in_left)
//                .setExitAnim(if (isForward) R.anim.slide_out_left else R.anim.slide_out_right)
//                .setPopEnterAnim(if (isForward) R.anim.slide_in_left else R.anim.slide_in_right)
//                .setPopExitAnim(if (isForward) R.anim.slide_out_right else R.anim.slide_out_left)
//                .setLaunchSingleTop(true)
//                .build()
//
//            navController.navigate(item.itemId, null, anim)
//            currentTabIndex = targetIndex
//            true
//        }
//    }
//
//    private fun setSystemBarsColorFromTheme() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
//
//        // Ambil warna background dari tema aplikasi, bisa pakai colorSurface Material
//        val bgColor = resolveThemeColor(com.google.android.material.R.attr.colorSurface)
//            ?: resolveThemeColor(android.R.attr.windowBackground)
//            ?: ContextCompat.getColor(this, android.R.color.white)
//
//        window.statusBarColor = bgColor
//
//        // Navigation bar juga ikut warna tema jika memungkinkan
//        val navBarColor = resolveThemeColor(com.google.android.material.R.attr.colorSurface)
//            ?: ContextCompat.getColor(this, android.R.color.black)
//
//        window.navigationBarColor = navBarColor
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            val decorView = window.decorView
//            decorView.systemUiVisibility = decorView.systemUiVisibility.let {
//                if (isColorLight(bgColor))
//                    it or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//                else
//                    it and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
//            }
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val decorView = window.decorView
//            decorView.systemUiVisibility = decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
//        }
//    }
//
//    // Fungsi bantu untuk resolve warna dari atribut tema
//    private fun resolveThemeColor(attrRes: Int): Int? {
//        val typedValue = TypedValue()
//        val theme = theme
//        return if (theme.resolveAttribute(attrRes, typedValue, true)) {
//            if (typedValue.resourceId != 0) {
//                ContextCompat.getColor(this, typedValue.resourceId)
//            } else {
//                typedValue.data
//            }
//        } else null
//    }
//
//    private fun isColorLight(color: Int): Boolean {
//        val darkness = 1 - (
//                0.299 * Color.red(color) +
//                        0.587 * Color.green(color) +
//                        0.114 * Color.blue(color)
//                ) / 255
//        return darkness < 0.5
//    }
//}
