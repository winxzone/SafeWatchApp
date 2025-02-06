package com.example.safewatchapp.utils

import android.content.Context
import android.content.SharedPreferences

object RoleManager {
    private const val PREF_NAME = "AppPrefs"
    private const val ROLE_KEY = "selected_role"

    // Сохранение роли
    fun saveRole(context: Context, role: String) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(ROLE_KEY, role).apply()
    }

    // Получение роли
    fun getRole(context: Context): String? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(ROLE_KEY, null)
    }

    // Проверка роли
    fun isChild(context: Context): Boolean {
        return getRole(context) == Constants.CHILD
    }
}
