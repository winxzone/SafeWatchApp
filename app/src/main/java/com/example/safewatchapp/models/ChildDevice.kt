package com.example.safewatchapp.models

data class ChildDevice(
    val id: String?,
    val accountId: String?,
    val childId: String?,
    val name: String,
    val status: String = "unconfirmed",
    val createdAt: Long = System.currentTimeMillis(),
    val confirmedAt: Long?
)
