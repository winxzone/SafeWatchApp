package com.example.safewatchapp.models

data class ChildDevice(
    val id: String?,
    val userId: String?,
    val childId: String?,
    val name: String,
    val deviceId: String,
    val status: String = "unconfirmed",
    val createdAt: Long = System.currentTimeMillis(),
    val confirmedAt: Long?
)
