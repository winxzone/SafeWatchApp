package com.example.safewatchapp.utils

import android.content.Context
import androidx.core.content.edit

object RoleManager {
    const val ROLE_CHILD = "Child"
    const val ROLE_PARENT = "Parent"

    private const val PREF_NAME = "AppPrefs"
    private const val ROLE_KEY = "selected_role"

    fun saveRole(context: Context, role: String) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit() { putString(ROLE_KEY, role) }
    }

    fun getRole(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(ROLE_KEY, null)
    }

    fun isChild(context: Context): Boolean {
        return getRole(context) == ROLE_CHILD
    }

    fun clearRole(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { remove(ROLE_KEY) }
    }
}
