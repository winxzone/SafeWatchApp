package com.example.safewatchapp.manager

import android.content.Context
import androidx.core.content.edit

object AppFilterManager {
    private const val PREFS_NAME = "app_filter"
    private const val KEY_ALLOWED_APPS = "allowed_apps"

    fun getAllowedApps(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_ALLOWED_APPS, emptySet()) ?: emptySet()
    }

    fun setAllowedApps(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putStringSet(KEY_ALLOWED_APPS, packages)
            }
    }
}
