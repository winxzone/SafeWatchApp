package com.example.safewatchapp.localdata.collector

import android.content.Context
import android.util.Log
import com.example.safewatchapp.localdata.cache.LocalCacheManager
import com.example.safewatchapp.models.NotificationData
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object NotificationCollector {

    private val collectedNotifications = mutableListOf<NotificationData>()

    fun onNotificationReceived(childDeviceId: String, packageName: String, title: String?, text: String?) {
        val formatter = DateTimeFormatter.ISO_DATE_TIME

        val data = NotificationData(
            childDeviceId = childDeviceId,
            packageName = packageName,
            title = title ?: "",
            text = text ?: "",
            timestamp = LocalDateTime.now().format(formatter)
        )

        collectedNotifications.add(data)
        Log.d("NotificationCollector", "Notification received: $data")
    }


    fun collectNotifications(context: Context, childDeviceId: String) {
        if (collectedNotifications.isNotEmpty()) {
            val copy = collectedNotifications.map {
                // подстраховка: если вдруг id не задан
                if (it.childDeviceId.isBlank()) it.copy(childDeviceId = childDeviceId) else it
            }
            collectedNotifications.clear()
            LocalCacheManager.saveNotifications(context, copy)
            Log.d("NotificationCollector", "Saved ${copy.size} notifications to cache")
        } else {
            Log.d("NotificationCollector", "No notifications to save")
        }
    }
}
