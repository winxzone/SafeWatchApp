package com.example.safewatchapp.models

import com.example.safewatchapp.service.SummaryAnalyzer

data class DeviceDailySummaryResponse(
    val childDeviceId: String,
    val date: String,
    val lastUpdated: String,
    val emotion: String,
    val emotionConfidence: Double,
    val totalScreenTime: Long,
    val topAppPackage: String,
    val notificationsCount: Int,
    val screenUnlockCount: Int,
    val usedAtNight: Boolean
) {
    val reasons: List<String> get() = SummaryAnalyzer.generateReasons(this)
    val advice: String get() = SummaryAnalyzer.generateAdvice(this)
}