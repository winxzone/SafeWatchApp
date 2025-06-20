package com.example.safewatchapp

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.safewatchapp.localdata.DataUploadScheduler
import com.example.safewatchapp.manager.CollectorManager
import com.example.safewatchapp.manager.DeviceManager
import com.example.safewatchapp.retrofit.ApiClient
import com.example.safewatchapp.utils.RoleManager

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        ApiClient.initialize(this)

        val childDeviceId = DeviceManager.getChildDeviceId(this)
        if (childDeviceId != null && RoleManager.isChild(this)) {
            CollectorManager.registerReceivers(this)
            Log.d("MyApp", "ScreenEventCollector registered")
        } else {
            Log.w("MyApp", "ScreenEventCollector error not registered")
        }

        DataUploadScheduler.schedule(this)

    }
}