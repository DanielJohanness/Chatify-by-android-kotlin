package com.student.chatify

import android.content.Context
import android.content.SharedPreferences

object SavedPreference {
    private const val PREF_NAME = "user_pref"
    private const val KEY_EMAIL = "email"
    private const val KEY_USERNAME = "username"

    private fun getSharedPref(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setEmail(context: Context, email: String) {
        val editor = getSharedPref(context).edit()
        editor.putString(KEY_EMAIL, email)
        editor.apply()
    }

    fun setUsername(context: Context, username: String) {
        val editor = getSharedPref(context).edit()
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }

    fun getEmail(context: Context): String? {
        return getSharedPref(context).getString(KEY_EMAIL, null)
    }

    fun getUsername(context: Context): String? {
        return getSharedPref(context).getString(KEY_USERNAME, null)
    }
}
