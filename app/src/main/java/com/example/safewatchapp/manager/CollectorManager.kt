package com.example.safewatchapp.manager

import android.content.Context
import com.example.safewatchapp.localdata.collector.AppUsageCollector
import com.example.safewatchapp.localdata.collector.NotificationCollector
import com.example.safewatchapp.localdata.collector.ScreenEventCollector

object CollectorManager {

    // Разовый сбор данных (вызывается в DataUploadWorker)
    fun collectOnce(context: Context, childDeviceId: String) {
        AppUsageCollector.collectAppUsage(context, childDeviceId)
        NotificationCollector.collectNotifications(context, childDeviceId)
        ScreenEventCollector.collectEvents(context, childDeviceId)
    }

    // Регистрация ресиверов (долгосрочная логика, вызывается в Application)
    fun registerReceivers(context: Context) {
        ScreenEventCollector.register(context)
    }
}
