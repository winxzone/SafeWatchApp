package com.example.safewatchapp.models

data class DeviceDataPayload(
    val childDeviceId: String,
    val timestamp: String,
    val appUsage: List<AppUsageData> = emptyList(),
    val notifications: List<NotificationData> = emptyList(),
    val screenEvent: ScreenEventData? = null
)
