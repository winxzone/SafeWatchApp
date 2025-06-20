package com.example.safewatchapp.models

data class ScreenEventData(
    val childDeviceId: String,
    val date: String,
    val screenOnCount: Int,
    val screenOffCount: Int,
    val unlockCount: Int,
    val usedAtNight: Boolean
)

enum class ScreenEventType {
    SCREEN_ON,
    SCREEN_OFF,
    UNLOCKED
}
