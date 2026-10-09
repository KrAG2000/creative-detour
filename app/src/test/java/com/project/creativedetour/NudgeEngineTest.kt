package com.project.creativedetour

import com.project.creativedetour.data.Feedback
import com.project.creativedetour.data.Outcome
import com.project.creativedetour.engine.NudgeContext
import com.project.creativedetour.engine.NudgeEngine
import com.project.creativedetour.engine.RejectReason
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NudgeEngineTest {
    private val engine = NudgeEngine()
    private val now = 1_000_000_000L
    private val glued = NudgeContext(
        hourOfDay = 15, stepsLastHour = 50, screenMinutesLastHour = 40, minutesSinceLastUsage = 0,
        minutesUntilNextEvent = null, nextEventTitle = null, minutesSinceLastNudge = null,
    )

    private fun rejected(reason: RejectReason, minutesAgo: Long, hour: Int = 15) = Feedback(
        shownAt = now - minutesAgo * 60_000, hourOfDay = hour, message = "",
        outcome = Outcome.REJECTED, reason = reason, respondedAt = now - minutesAgo * 60_000,
    )

    @Test fun nudgesWhenGluedToPhone() = assertTrue(engine.shouldNudge(glued, emptyList(), now))

    @Test fun quietAtNight() = assertFalse(engine.shouldNudge(glued.copy(hourOfDay = 23), emptyList(), now))

    @Test fun respectsMinimumGap() =
        assertFalse(engine.shouldNudge(glued.copy(minutesSinceLastNudge = 10), emptyList(), now))

    @Test fun tiredCoolsDownForThreeHours() {
        assertFalse(engine.shouldNudge(glued, listOf(rejected(RejectReason.TIRED, 120)), now))
        assertTrue(engine.shouldNudge(glued, listOf(rejected(RejectReason.TIRED, 200)), now))
    }

    @Test fun learnsBusyHours() {
        val history = listOf(rejected(RejectReason.BUSY, 1440 * 2), rejected(RejectReason.BUSY, 1440))
        assertFalse(engine.shouldNudge(glued, history, now))
        assertTrue(engine.shouldNudge(glued.copy(hourOfDay = 16), history, now))
    }
}
