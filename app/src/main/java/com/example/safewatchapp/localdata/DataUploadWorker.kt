package com.example.safewatchapp.localdata

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.safewatchapp.localdata.cache.LocalCacheManager
import com.example.safewatchapp.manager.CollectorManager
import com.example.safewatchapp.manager.DeviceManager
import com.example.safewatchapp.models.DeviceDataPayload
import com.example.safewatchapp.retrofit.ApiClient
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DataUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val childDeviceId = DeviceManager.getChildDeviceId(context) ?: return Result.failure()

        try {
            Log.d("DataUploadWorker", "Starting collectors...")
            CollectorManager.collectOnce(context, childDeviceId)

            val appUsage = LocalCacheManager.getAppUsage(context)
            val notifications = LocalCacheManager.getNotifications(context)
            val screenEvent = LocalCacheManager.getScreenEventData(context)

            Log.d("DataUploadWorker", "AppUsage: ${appUsage.size}, Notifications: ${notifications.size}, ScreenEvent: ${screenEvent != null}")

            // Нет данных — не загружаем
            if (appUsage.isEmpty() && notifications.isEmpty() && screenEvent == null) {
                Log.d("DataUploadWorker", "No data to upload")
                return Result.success()
            }

            val payload = DeviceDataPayload(
                childDeviceId = childDeviceId,
                appUsage = appUsage,
                notifications = notifications,
                screenEvent = screenEvent,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            )

            Log.d("DataUploadWorker", "Uploading payload: $payload")

            ApiClient.deviceDataApiService.uploadDeviceData(payload)

            LocalCacheManager.clearAll(context)
            Log.d("DataUploadWorker", "Upload success, cache cleared")
            return Result.success()

        } catch (e: Exception) {
            Log.e("DataUploadWorker", "Upload failed: ${e.message}", e)
            return Result.retry()
        }
    }
}


