package com.example.safewatchapp.localdata.collector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.safewatchapp.localdata.cache.LocalCacheManager
import com.example.safewatchapp.models.ScreenEventData
import com.example.safewatchapp.models.ScreenEventType
import java.time.LocalDate
import java.time.LocalDateTime

object ScreenEventCollector {

    private val screenEvents = mutableListOf<ScreenEventType>()
    private var receiver: BroadcastReceiver? = null

    // todo: исправить логику с childDeviceId. Как работает сохранение
    fun register(context: Context) {
        if (receiver != null) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val type = when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> ScreenEventType.SCREEN_ON
                    Intent.ACTION_SCREEN_OFF -> ScreenEventType.SCREEN_OFF
                    Intent.ACTION_USER_PRESENT -> ScreenEventType.UNLOCKED
                    else -> null
                }

                type?.let {
                    synchronized(screenEvents) {
                        screenEvents.add(it)
                    }
                    Log.d("ScreenEventCollector", "Event received: $it")
                }
            }
        }

        context.registerReceiver(receiver, filter)
        Log.d("ScreenEventCollector", "BroadcastReceiver registered")
    }

    fun collectEvents(context: Context, childDeviceId: String) {
        val eventsToAggregate = synchronized(screenEvents) {
            val copy = screenEvents.toList()
            screenEvents.clear()
            copy
        }

        if (eventsToAggregate.isEmpty()) {
            Log.d("ScreenEventCollector", "No events to aggregate")
            return
        }

        val now = LocalDate.now()

        val screenOnCount = eventsToAggregate.count { it == ScreenEventType.SCREEN_ON }
        val screenOffCount = eventsToAggregate.count { it == ScreenEventType.SCREEN_OFF }
        val unlockCount = eventsToAggregate.count { it == ScreenEventType.UNLOCKED }

        val usedAtNight = LocalDateTime.now().hour < 6 || LocalDateTime.now().hour >= 22

        val data = ScreenEventData(
            childDeviceId = childDeviceId,
            date = now.toString(),
            screenOnCount = screenOnCount,
            screenOffCount = screenOffCount,
            unlockCount = unlockCount,
            usedAtNight = usedAtNight
        )

        LocalCacheManager.saveScreenEventData(context, data)
        Log.d("ScreenEventCollector", "Aggregated data saved: $data")
    }

    fun unregister(context: Context) {
        receiver?.let {
            context.unregisterReceiver(it)
            receiver = null
            Log.d("ScreenEventCollector", "BroadcastReceiver unregistered")
        }
    }
}
