package com.project.creativedetour

import com.project.creativedetour.data.Feedback
import com.project.creativedetour.data.Outcome
import com.project.creativedetour.engine.Decision
import com.project.creativedetour.engine.NudgeContext
import com.project.creativedetour.engine.NudgeEngine
import com.project.creativedetour.engine.RejectReason
import org.junit.Assert.assertEquals
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

    @Test fun highActivityBar() {
        val calm = glued.copy(screenMinutesLastHour = 10)
        assertTrue(engine.shouldNudge(calm.copy(stepsLastHour = 300), emptyList(), now)) // 300 steps still counts as sitting
        assertFalse(engine.shouldNudge(calm.copy(stepsLastHour = 600), emptyList(), now))
    }

    @Test fun quietExplainsAndSaysWhenToRetry() {
        val decision = engine.decide(glued, listOf(rejected(RejectReason.TIRED, 60)), now)
        assertTrue(decision is Decision.Quiet)
        decision as Decision.Quiet
        assertTrue(decision.reason, decision.reason.contains("Tired"))
        assertEquals(now + 120 * 60_000L, decision.retryAt) // 180 min cooldown, 60 already passed
    }

    @Test fun minimumGapRetryTime() {
        val decision = engine.decide(glued.copy(minutesSinceLastNudge = 15), emptyList(), now) as Decision.Quiet
        assertEquals(now + 30 * 60_000L, decision.retryAt)
    }
}
