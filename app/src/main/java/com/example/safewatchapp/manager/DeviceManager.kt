package com.example.safewatchapp.manager

import android.content.Context
import androidx.core.content.edit

object DeviceManager {
    private const val PREFS_NAME = "app_prefs"
    private const val CHILD_DEVICE_ID_KEY = "child_device_id"
    private const val CHILD_PROFILE_ID_KEY = "child_profile_id"

    fun getChildDeviceId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(CHILD_DEVICE_ID_KEY, null)
    }

    fun saveChildDeviceId(context: Context, id: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit() { putString(CHILD_DEVICE_ID_KEY, id) }
    }

    fun clearChildDeviceId(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit() { remove(CHILD_DEVICE_ID_KEY) }
    }

    fun clearChildProfileId(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit() { remove(CHILD_PROFILE_ID_KEY) }
    }

    fun saveChildProfileId(context: Context, id: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit() { putString(CHILD_PROFILE_ID_KEY, id) }
    }

    fun getChildProfileId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(CHILD_PROFILE_ID_KEY, null)
    }
}
