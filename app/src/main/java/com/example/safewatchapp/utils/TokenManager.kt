package com.example.safewatchapp.utils

import android.content.Context
import android.util.Log
import androidx.core.content.edit

object TokenManager {
    private const val PREFS_NAME = "AuthPrefs"
    private const val TOKEN_KEY = "jwt_token"
    private const val REMIND_ME_KEY = "remind_me"

    fun saveToken(context: Context, token: String?) {
        Log.d("TokenManager", "Saving token: $token")
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit() { putString(TOKEN_KEY, token) }
    }

    fun getToken(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = sharedPreferences.getString(TOKEN_KEY, null)
        Log.d("TokenManager", "Retrieved token: $token")
        return token
    }

    fun clearToken(context: Context) {
        Log.d("TokenManager", "Clearing token")
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit() { remove(TOKEN_KEY) }
    }

    fun clearRemindMe(context: Context) {
        Log.d("TokenManager", "Clearing remindMe")
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit() { remove(REMIND_ME_KEY) }
    }

    fun saveRemindMe(context: Context, remindMe: Boolean) {
        Log.d("TokenManager", "Saving remindMe: $remindMe")
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit() { putBoolean(REMIND_ME_KEY, remindMe) }
    }

    fun isRemindMeEnabled(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val remindMe = sharedPreferences.getBoolean(REMIND_ME_KEY, false)
        Log.d("TokenManager", "Retrieved remindMe: $remindMe")
        return remindMe
    }
}
