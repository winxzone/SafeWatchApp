package com.example.safewatchapp.localdata.collector

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import com.example.safewatchapp.models.AppUsageData
import android.content.Context
import android.util.Log
import com.example.safewatchapp.localdata.cache.LocalCacheManager
import java.util.concurrent.TimeUnit
import java.time.Instant


object AppUsageCollector {

    fun collectAppUsage(context: Context, childDeviceId: String) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1)

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)

        val usageMap = mutableMapOf<String, MutableList<Pair<Long, Long>>>()
        var currentEvent: UsageEvents.Event

        val lastForegroundTime = mutableMapOf<String, Long>()

        while (usageEvents.hasNextEvent()) {
            currentEvent = UsageEvents.Event()
            usageEvents.getNextEvent(currentEvent)

            val packageName = currentEvent.packageName ?: continue

            when (currentEvent.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    lastForegroundTime[packageName] = currentEvent.timeStamp
                }

                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = lastForegroundTime[packageName] ?: continue
                    val duration = currentEvent.timeStamp - start

                    if (duration >= 60_000) {
                        usageMap.getOrPut(packageName) { mutableListOf() }
                            .add(start to currentEvent.timeStamp)
                    }

                    lastForegroundTime.remove(packageName)
                }
            }
        }

        for ((packageName, sessions) in usageMap) {
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent == null) continue  // Приложение без UI

            val totalTime = sessions.sumOf { it.second - it.first }

            val data = AppUsageData(
                childDeviceId = childDeviceId,
                packageName = packageName,
                totalTimeForeground = totalTime,
                lastTimeUsed = Instant.ofEpochMilli(sessions.maxOf { it.second }).toString(),
                timestamp = Instant.now().toString()
            )

            Log.d("AppUsageCollector", "Session collected: $data")
            LocalCacheManager.saveAppUsage(context, listOf(data))
        }
    }
}
