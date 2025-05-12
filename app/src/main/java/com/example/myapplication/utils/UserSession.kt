// UserSession.kt
package com.example.myapplication.utils

import android.content.Context
import android.content.SharedPreferences

object UserSession {
    private const val PREF_NAME = "user_session"
    private const val KEY_LOGGED_IN = "logged_in"

    fun setLoggedIn(context: Context, loggedIn: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LOGGED_IN, false)
    }

    fun logout(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}