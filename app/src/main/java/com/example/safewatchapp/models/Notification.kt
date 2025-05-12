package com.example.safewatchapp.models

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    var isRead: Boolean,
    val isDeleted: Boolean,
    val type: String
)

