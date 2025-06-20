package com.example.safewatchapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.safewatchapp.localdata.DataUploadScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Только планируем задачу или сохраняем флаг
            DataUploadScheduler.schedule(context)
        }
    }
}


