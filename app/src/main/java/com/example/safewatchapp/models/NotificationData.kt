package com.example.safewatchapp.models

data class NotificationData(
    val childDeviceId: String,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: String
)
