package com.example.safewatchapp.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ScreenshotUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val childDeviceId = inputData.getString("childDeviceId") ?: return Result.failure()

        val uploader = ScreenshotUploader(applicationContext)
        uploader.uploadAllScreenshots(childDeviceId)

        return Result.success()
    }
}