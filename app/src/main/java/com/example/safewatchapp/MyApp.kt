package com.example.safewatchapp

import android.app.Application
import com.example.safewatchapp.retrofit.ApiClient

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.initialize(this)
    }
}