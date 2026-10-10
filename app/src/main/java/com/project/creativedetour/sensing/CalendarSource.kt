package com.project.creativedetour.sensing

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Instances

/** Reads the on-device calendar (whatever accounts are synced). Read-only, nothing leaves the phone. */
class CalendarSource(private val context: Context) {

    /** minutesUntil = 0 means an event is happening right now. */
    data class NextEvent(val title: String?, val minutesUntil: Int)

    fun nextEvent(now: Long): NextEvent? {
        if (!Access.hasCalendar(context)) return null
        // Instances = recurring events expanded into real occurrences within [begin, end].
        val uri = Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now - LOOKBACK_MS) // catch long events already in progress
            ContentUris.appendId(it, now + LOOKAHEAD_MS)
        }.build()
        val projection = arrayOf(Instances.TITLE, Instances.BEGIN)
        val selection = "${Instances.VISIBLE} = 1 AND ${Instances.ALL_DAY} = 0 AND " +
            "${Instances.SELF_ATTENDEE_STATUS} != ${Attendees.ATTENDEE_STATUS_DECLINED} AND ${Instances.END} > ?"

        context.contentResolver.query(uri, projection, selection, arrayOf(now.toString()), "${Instances.BEGIN} ASC")
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val minutes = ((cursor.getLong(1) - now).coerceAtLeast(0) / 60_000).toInt()
                return NextEvent(cursor.getString(0), minutes)
            }
        return null
    }

    private companion object {
        const val LOOKBACK_MS = 12 * 60 * 60 * 1000L
        const val LOOKAHEAD_MS = 6 * 60 * 60 * 1000L
    }
}
