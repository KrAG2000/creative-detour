package com.project.creativedetour

import com.project.creativedetour.data.Feedback
import com.project.creativedetour.data.Outcome
import com.project.creativedetour.engine.Activity
import com.project.creativedetour.engine.ActivityPicker
import com.project.creativedetour.engine.NudgeContext
import com.project.creativedetour.engine.RejectReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ActivityPickerTest {
    private val now = 1_000_000_000L
    private val ctx = NudgeContext(
        hourOfDay = 15, stepsLastHour = 50, screenMinutesLastHour = 40, minutesSinceLastUsage = 0,
        minutesUntilNextEvent = null, nextEventTitle = null, minutesSinceLastNudge = null,
    )

    private fun fb(activity: Activity, outcome: Outcome, reason: RejectReason? = null, minutesAgo: Long = 60) = Feedback(
        shownAt = now - minutesAgo * 60_000, hourOfDay = 15, message = "", activity = activity.name,
        outcome = outcome, reason = reason, respondedAt = now - minutesAgo * 60_000,
    )

    /** Pick many times with different seeds to check what can and can't come out. */
    private fun picks(history: List<Feedback>, c: NudgeContext = ctx) =
        (0 until 300).map { ActivityPicker(Random(it)).pick(c, history, now) }

    @Test fun variesWithNoHistory() = assertTrue(picks(emptyList()).toSet().size >= 5)

    @Test fun neverRepeatsTheLastTwo() {
        val history = listOf(fb(Activity.STRETCH, Outcome.ACCEPTED, minutesAgo = 50), fb(Activity.DANCE, Outcome.ACCEPTED, minutesAgo = 100))
        val result = picks(history)
        assertFalse(Activity.STRETCH in result)
        assertFalse(Activity.DANCE in result)
    }

    @Test fun badWeatherMeansIndoors() =
        assertTrue(picks(listOf(fb(Activity.OUTDOOR_WALK, Outcome.REJECTED, RejectReason.WEATHER))).none { it.outdoor })

    @Test fun tiredMeansGentle() =
        assertTrue(picks(listOf(fb(Activity.BODYWEIGHT, Outcome.REJECTED, RejectReason.TIRED))).all { it.gentle })

    @Test fun meetingSoonMeansShort() =
        assertTrue(picks(emptyList(), ctx.copy(minutesUntilNextEvent = 15)).all { it.minutes <= 3 })

    @Test fun acceptedActivitiesGetMoreLikely() {
        // DANCE accepted 4 times long ago (outside the "avoid last two" window).
        val history = listOf(fb(Activity.STRETCH, Outcome.ACCEPTED, minutesAgo = 30), fb(Activity.MOBILITY, Outcome.ACCEPTED, minutesAgo = 40)) +
            List(4) { fb(Activity.DANCE, Outcome.ACCEPTED, minutesAgo = 600L + it) }
        val danceShare = picks(history).count { it == Activity.DANCE } / 300.0
        val baseline = picks(emptyList()).count { it == Activity.DANCE } / 300.0
        assertTrue("dance $danceShare vs baseline $baseline", danceShare > baseline * 2)
    }

    @Test fun fallsBackToMobilityWhenEverythingIsFiltered() {
        // Tired + busy → gentle & ≤3 min = TOUCH_GRASS, STRETCH, MOBILITY; mark them as just used / filtered.
        val history = listOf(
            fb(Activity.TOUCH_GRASS, Outcome.REJECTED, RejectReason.TIRED, minutesAgo = 10),
            fb(Activity.STRETCH, Outcome.REJECTED, RejectReason.BUSY, minutesAgo = 20),
        )
        assertEquals(setOf(Activity.MOBILITY), picks(history).toSet())
    }
}
