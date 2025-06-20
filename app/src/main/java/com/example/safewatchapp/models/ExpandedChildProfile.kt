package com.example.safewatchapp.models

data class ExpandedChildProfile(
    val id: String,
    val name: String,
    val photoId: String?,
    val summary: DeviceDailySummaryResponse?
)
