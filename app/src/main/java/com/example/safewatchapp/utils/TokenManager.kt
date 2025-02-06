package com.example.safewatchapp.utils

import android.content.Context

object TokenManager {
    private const val PREFS_NAME = "AuthPrefs"
    private const val TOKEN_KEY = "jwt_token"
    private const val REMIND_ME_KEY = "remind_me"

    fun saveToken(context: Context, token: String?) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(TOKEN_KEY, null)
    }

    fun clearToken(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().remove(TOKEN_KEY).apply()
    }

    fun saveRemindMe(context: Context, remindMe: Boolean) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(REMIND_ME_KEY, remindMe).apply()
    }

    fun isRemindMeEnabled(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(REMIND_ME_KEY, false)
    }
}
