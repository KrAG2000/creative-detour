package com.project.creativedetour.engine

import com.project.creativedetour.data.Feedback
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** What the engine decided, and why. Shown to the user so silence is never a mystery. */
sealed interface Decision {
    data object Nudge : Decision

    /** [retryAt] = the earliest time it's worth checking again, when the engine knows it. */
    data class Quiet(val reason: String, val retryAt: Long? = null) : Decision
}

/** Decides whether *now* is a good moment. Pure logic: no Android, easy to unit test. */
class NudgeEngine(private val zone: ZoneId = ZoneId.systemDefault()) {

    fun shouldNudge(ctx: NudgeContext, recent: List<Feedback>, now: Long) = decide(ctx, recent, now) == Decision.Nudge

    /** [recent] = feedback from the last few days, newest first. First matching rule wins. */
    fun decide(ctx: NudgeContext, recent: List<Feedback>, now: Long): Decision {
        // Guards: never spam, respect what the user told us
        ctx.minutesSinceLastNudge?.let {
            if (it < MIN_GAP_MINUTES) return Decision.Quiet("Last nudge was $it min ago", now + (MIN_GAP_MINUTES - it) * MINUTE)
        }
        cooldown(recent)?.let { (reason, until) ->
            if (now < until) return Decision.Quiet("You said “${reason.label}”. Giving you some space", until)
        }
        if (busyAtThisHour(recent, ctx.hourOfDay)) {
            return Decision.Quiet("You're usually busy around ${ctx.hourOfDay}:00", startOfNextHour(now))
        }
        if (ctx.hourOfDay !in DAY_START..DAY_END) {
            return Decision.Quiet("Quiet hours until $DAY_START:00", nextDayStart(now, ctx.hourOfDay))
        }
        // Already off the phone or already moving: nothing to fix
        ctx.minutesSinceLastUsage?.let { if (it >= AWAY_MINUTES) return Decision.Quiet("You're away from your phone. Nice") }
        ctx.stepsLastHour?.let { if (it > ACTIVE_STEPS) return Decision.Quiet("Already moving: $it steps this hour") }
        ctx.minutesUntilNextEvent?.let {
            val title = ctx.nextEventTitle ?: "Your event"
            if (it < 10) return Decision.Quiet(if (it == 0) "You're in “$title” right now" else "“$title” starts in $it min")
        }
        // Triggers
        ctx.screenMinutesLastHour?.let { if (it >= SCREEN_MINUTES) return Decision.Nudge }
        ctx.stepsLastHour?.let { if (it < SEDENTARY_STEPS) return Decision.Nudge }
        return Decision.Quiet(allGood(ctx))
    }

    /** The newest rejection's reason decides how long we back off. */
    private fun cooldown(recent: List<Feedback>): Pair<RejectReason, Long>? {
        val last = recent.firstOrNull { it.reason != null } ?: return null
        val reason = last.reason ?: return null
        val since = last.respondedAt ?: last.shownAt
        return reason to since + reason.cooldownMinutes * MINUTE
    }

    /** "Busy" twice at this hour recently → this hour is probably always busy. */
    private fun busyAtThisHour(recent: List<Feedback>, hour: Int): Boolean =
        recent.count { it.hourOfDay == hour && it.reason == RejectReason.BUSY } >= 2

    private fun allGood(ctx: NudgeContext): String {
        val parts = listOfNotNull(
            ctx.screenMinutesLastHour?.let { "$it min on screen" },
            ctx.stepsLastHour?.let { "$it steps" },
        )
        return if (parts.isEmpty()) "Not enough data yet" else "All good this hour: ${parts.joinToString(", ")}"
    }

    private fun startOfNextHour(now: Long): Long =
        Instant.ofEpochMilli(now).atZone(zone).truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()

    private fun nextDayStart(now: Long, hour: Int): Long {
        val today = Instant.ofEpochMilli(now).atZone(zone).truncatedTo(ChronoUnit.DAYS).withHour(DAY_START)
        return (if (hour > DAY_END) today.plusDays(1) else today).toInstant().toEpochMilli()
    }

    companion object {
        const val MIN_GAP_MINUTES = 45
        const val SCREEN_MINUTES = 30
        /** Under this many steps in an hour counts as sitting still. High bar on purpose. */
        const val SEDENTARY_STEPS = 400
        const val ACTIVE_STEPS = 1500
        const val AWAY_MINUTES = 45
        const val DAY_START = 7
        const val DAY_END = 21 // last hour that can get a nudge (21:00–21:59)
        private const val MINUTE = 60_000L
    }
}
