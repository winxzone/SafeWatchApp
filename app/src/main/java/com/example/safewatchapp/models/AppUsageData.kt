package com.example.safewatchapp.models

data class AppUsageData(
    val childDeviceId: String,
    val packageName: String,
    val totalTimeForeground: Long,
    val lastTimeUsed: String,
    val timestamp: String
)