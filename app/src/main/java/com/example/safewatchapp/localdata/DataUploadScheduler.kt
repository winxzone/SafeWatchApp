package com.example.safewatchapp.localdata

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object DataUploadScheduler {
    fun schedule(context: Context) {
        val uploadRequest = PeriodicWorkRequestBuilder<DataUploadWorker>(
            1, TimeUnit.HOURS// или другой интервал
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DataUploadWork",
            ExistingPeriodicWorkPolicy.KEEP,
            uploadRequest
        )
    }
}
