package com.example.safewatchapp.localdata.cache

import com.google.gson.Gson
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.safewatchapp.models.AppUsageData
import com.example.safewatchapp.models.NotificationData
import com.example.safewatchapp.models.ScreenEventData
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object LocalCacheManager {

    val Context.dataStore by preferencesDataStore(name = "device_cache")

    private val gson = Gson()

    private val appUsageKey = stringPreferencesKey("cached_app_usage")
    private val notificationsKey = stringPreferencesKey("cached_notifications")
    private val screenEventDataKey = stringPreferencesKey("cached_screen_event_data")

    // -------------------- App Usage --------------------
    fun saveAppUsage(context: Context, data: List<AppUsageData>) = runBlocking {
        val json = gson.toJson(data)
        context.dataStore.edit {
            it[appUsageKey] = json
        }
    }

    fun getAppUsage(context: Context): List<AppUsageData> = runBlocking {
        val prefs = context.dataStore.data.first()
        val json = prefs[appUsageKey] ?: return@runBlocking emptyList()
        gson.fromJson(json, object : TypeToken<List<AppUsageData>>() {}.type)
    }

    // -------------------- Notifications --------------------
    fun saveNotifications(context: Context, data: List<NotificationData>) = runBlocking {
        val json = gson.toJson(data)
        context.dataStore.edit {
            it[notificationsKey] = json
        }
    }

    fun getNotifications(context: Context): List<NotificationData> = runBlocking {
        val prefs = context.dataStore.data.first()
        val json = prefs[notificationsKey] ?: return@runBlocking emptyList()
        gson.fromJson(json, object : TypeToken<List<NotificationData>>() {}.type)
    }

    // -------------------- Aggregated Screen Event Data --------------------
    fun saveScreenEventData(context: Context, data: ScreenEventData) = runBlocking {
        val json = gson.toJson(data)
        context.dataStore.edit {
            it[screenEventDataKey] = json
        }
    }

    fun getScreenEventData(context: Context): ScreenEventData? = runBlocking {
        val prefs = context.dataStore.data.first()
        val json = prefs[screenEventDataKey] ?: return@runBlocking null
        gson.fromJson(json, ScreenEventData::class.java)
    }

    // -------------------- Clear All --------------------
    fun clearAll(context: Context) = runBlocking {
        context.dataStore.edit {
            it.remove(appUsageKey)
            it.remove(notificationsKey)
            it.remove(screenEventDataKey)
        }
    }
}
