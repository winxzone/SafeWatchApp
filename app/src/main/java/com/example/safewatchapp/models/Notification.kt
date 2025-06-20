package com.example.safewatchapp.models

data class Notification(
    val id: String? = null,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    var isRead: Boolean = false,
    val isDeleted: Boolean = false,

    @Transient
    val type: String? = null
)

