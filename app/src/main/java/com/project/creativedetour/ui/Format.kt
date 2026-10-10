package com.project.creativedetour.ui

import com.project.creativedetour.engine.Activity
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

fun timeOf(ms: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))

/** "Today", "Yesterday", or e.g. "Thu, 8 Oct". */
fun dayLabel(ms: Long): String {
    val day = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    }
}

val Activity.emoji: String
    get() = when (this) {
        Activity.OUTDOOR_WALK -> "🚶"
        Activity.TOUCH_GRASS -> "🌿"
        Activity.STRETCH -> "🧘"
        Activity.MOBILITY -> "🔄"
        Activity.BODYWEIGHT -> "💪"
        Activity.STAIRS -> "🪜"
        Activity.DANCE -> "💃"
        Activity.ACTIVE_CHORE -> "🪴"
        Activity.WALK_AND_TALK -> "📞"
    }
