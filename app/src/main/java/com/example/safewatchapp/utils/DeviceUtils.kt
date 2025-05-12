package com.example.safewatchapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import java.util.*
import androidx.core.content.edit

object DeviceIdUtils {
    private const val PREFS_NAME = "DevicePrefs"
    private const val UNIQUE_ID_KEY = "UNIQUE_DEVICE_ID"
    private const val TAG = "DeviceIdUtils"

    @SuppressLint("HardwareIds")
    fun getDeviceUniqueId(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var uniqueId: String? = sharedPrefs.getString(UNIQUE_ID_KEY, null)

        if (uniqueId.isNullOrEmpty()) {
            uniqueId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    ?.takeIf { it.isNotBlank() }
                    ?: UUID.randomUUID().toString()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка получения ANDROID_ID: ${e.message}")
                UUID.randomUUID().toString()
            }

            sharedPrefs.edit() { putString(UNIQUE_ID_KEY, uniqueId) }
            Log.d(TAG, "Сохранён уникальный ID: $uniqueId")
        }

        return uniqueId ?: UUID.randomUUID().toString()
    }
}
