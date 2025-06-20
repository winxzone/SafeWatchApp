package com.example.safewatchapp.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.safewatchapp.localdata.collector.NotificationCollector
import com.example.safewatchapp.manager.AppFilterManager
import com.example.safewatchapp.manager.DeviceManager

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val context = applicationContext

        val childDeviceId = DeviceManager.getChildDeviceId(context)
        if (childDeviceId == null) {
            Log.w("NotificationListener", "No childDeviceId — skipping notification")
            return
        }

        val packageName = sbn.packageName ?: "unknown"

        // Проверяем, разрешено ли приложение для мониторинга
        val allowedApps = AppFilterManager.getAllowedApps(context)
        if (!allowedApps.contains(packageName)) {
            Log.d("NotificationListener", "Package $packageName not in allowed list — skipping notification")
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        // Дополнительная проверка на пустые уведомления
        if (title.isNullOrBlank() && text.isNullOrBlank()) {
            Log.d("NotificationListener", "Empty notification from $packageName — skipping")
            return
        }

        NotificationCollector.onNotificationReceived(
            childDeviceId = childDeviceId,
            packageName = packageName,
            title = title,
            text = text
        )

        Log.d("NotificationListener", "Collected notification from $packageName — $title")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Можно реализовать при необходимости
        // Например, для отслеживания удаления уведомлений из разрешенных приложений
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w("NotificationListener", "Notification listener disconnected")
    }
}