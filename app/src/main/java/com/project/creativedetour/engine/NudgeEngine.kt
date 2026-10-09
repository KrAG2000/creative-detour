package com.project.creativedetour.engine

import com.project.creativedetour.data.Feedback

/** Decides whether *now* is a good moment. Pure logic: no Android, easy to unit test. */
class NudgeEngine {

    /** [recent] = feedback from the last few days, newest first. */
    fun shouldNudge(ctx: NudgeContext, recent: List<Feedback>, now: Long): Boolean = when {
        // Guards: never spam, respect what the user told us
        ctx.minutesSinceLastNudge != null && ctx.minutesSinceLastNudge < MIN_GAP_MINUTES -> false
        inCooldown(recent, now) -> false
        busyAtThisHour(recent, ctx.hourOfDay) -> false
        ctx.hourOfDay !in 7..21 -> false
        // Already off the phone or already moving: nothing to fix
        ctx.minutesSinceLastUsage != null && ctx.minutesSinceLastUsage >= 45 -> false
        ctx.stepsLastHour != null && ctx.stepsLastHour > 1500 -> false
        ctx.minutesUntilNextEvent != null && ctx.minutesUntilNextEvent < 10 -> false
        // Triggers
        ctx.screenMinutesLastHour != null && ctx.screenMinutesLastHour >= 30 -> true
        ctx.stepsLastHour != null && ctx.stepsLastHour < 100 -> true
        else -> false
    }

    /** The newest rejection's reason decides how long we back off. */
    private fun inCooldown(recent: List<Feedback>, now: Long): Boolean {
        val last = recent.firstOrNull { it.reason != null } ?: return false
        val reason = last.reason ?: return false
        val since = last.respondedAt ?: last.shownAt
        return now < since + reason.cooldownMinutes * 60_000L
    }

    /** "Busy" twice at this hour recently → this hour is probably always busy. */
    private fun busyAtThisHour(recent: List<Feedback>, hour: Int): Boolean =
        recent.count { it.hourOfDay == hour && it.reason == RejectReason.BUSY } >= 2

    companion object {
        const val MIN_GAP_MINUTES = 45
    }
}
