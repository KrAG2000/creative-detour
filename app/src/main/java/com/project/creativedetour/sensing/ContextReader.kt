package com.project.creativedetour.sensing

import android.content.Context
import com.project.creativedetour.data.FeedbackDao
import com.project.creativedetour.engine.NudgeContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime

/** Combines every source into one NudgeContext. A missing source just becomes null. */
class ContextReader(context: Context, private val steps: StepSource, private val dao: FeedbackDao) {
    private val screen = ScreenTimeSource(context)
    private val calendar = CalendarSource(context)

    suspend fun read(now: Long): NudgeContext = withContext(Dispatchers.IO) {
        val screenStats = screen.lastHour(now)
        val next = calendar.nextEvent(now)
        val lastNudgeAt = dao.latest()?.shownAt
        NudgeContext(
            hourOfDay = LocalTime.now().hour,
            stepsLastHour = steps.stepsLastHour(),
            screenMinutesLastHour = screenStats?.minutesOn,
            minutesSinceLastUsage = screenStats?.minutesSinceLastUse,
            minutesUntilNextEvent = next?.minutesUntil,
            nextEventTitle = next?.title,
            minutesSinceLastNudge = lastNudgeAt?.let { ((now - it) / 60_000).toInt() },
        )
    }
}
