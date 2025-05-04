package com.example.safewatchapp.utils

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.example.safewatchapp.service.ScreenshotUploadWorker
import java.util.concurrent.TimeUnit
import android.content.Context
import androidx.work.*

object ScreenshotUtils {

    fun scheduleScreenshotUpload(context: Context, childDeviceId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Только Wi-Fi
            .build()

        val inputData = workDataOf("childDeviceId" to childDeviceId)

        val workRequest = PeriodicWorkRequestBuilder<ScreenshotUploadWorker>(
            6, TimeUnit.HOURS
        )
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "screenshot_upload_worker", // Название уникальной задачи
                ExistingPeriodicWorkPolicy.KEEP, // Не перезапускать, если уже запланировано
                workRequest
            )
    }
}