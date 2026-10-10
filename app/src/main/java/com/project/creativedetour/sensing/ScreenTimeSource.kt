package com.project.creativedetour.sensing

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

/** Reads only screen on/off events from Usage Access, never which apps were used. */
class ScreenTimeSource(private val context: Context) {

    data class Stats(val minutesOn: Int, val minutesSinceLastUse: Int)

    fun lastHour(now: Long): Stats? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !Access.hasUsageAccess(context)) return null
        val start = now - HOUR_MS
        val events = context.getSystemService(UsageStatsManager::class.java).queryEvents(start, now)
        val event = UsageEvents.Event()

        var onSince: Long? = null
        var firstEvent = true
        var lastOff: Long? = null
        var totalOn = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> onSince = event.timeStamp
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    // If the hour starts with "screen off", the screen was on since before the window.
                    val from = onSince ?: if (firstEvent) start else null
                    if (from != null) totalOn += event.timeStamp - from
                    onSince = null
                    lastOff = event.timeStamp
                }
                else -> continue
            }
            firstEvent = false
        }

        val screenOnNow = context.getSystemService(PowerManager::class.java).isInteractive
        if (screenOnNow) totalOn += now - (onSince ?: if (firstEvent) start else now)

        val sinceLastUse = when {
            screenOnNow -> 0
            lastOff != null -> ((now - lastOff) / MINUTE_MS).toInt()
            else -> 60 // off for the whole hour
        }
        return Stats((totalOn / MINUTE_MS).toInt(), sinceLastUse)
    }

    private companion object {
        const val MINUTE_MS = 60_000L
        const val HOUR_MS = 60 * MINUTE_MS
    }
}
