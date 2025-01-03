package com.example.safewatchapp.models

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean,
    val isDeleted: Boolean,
)

