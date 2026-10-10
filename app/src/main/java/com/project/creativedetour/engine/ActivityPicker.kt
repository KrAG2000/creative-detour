package com.project.creativedetour.engine

import com.project.creativedetour.data.Feedback
import com.project.creativedetour.data.Outcome
import kotlin.random.Random

/**
 * Chooses *what* to suggest. Hard filters from what the user told us recently, then a weighted
 * random pick: activities they accept get more likely, ones they decline less likely.
 */
class ActivityPicker(private val random: Random = Random.Default) {

    /** [history] = recent feedback, newest first. */
    fun pick(ctx: NudgeContext, history: List<Feedback>, now: Long): Activity {
        val recentReasons = history
            .filter { (it.respondedAt ?: it.shownAt) >= now - REASON_MEMORY_MS }
            .mapNotNull { it.reason }
            .toSet()
        val justUsed = history.take(AVOID_LAST).mapNotNull { it.activity }.toSet()

        var candidates = Activity.entries.filter { it.name !in justUsed }
        if (RejectReason.WEATHER in recentReasons) candidates = candidates.filterNot { it.outdoor }
        if (RejectReason.TIRED in recentReasons) candidates = candidates.filter { it.gentle }
        val shortOnTime = RejectReason.BUSY in recentReasons || (ctx.minutesUntilNextEvent ?: Int.MAX_VALUE) <= 20
        if (shortOnTime) candidates = candidates.filter { it.minutes <= 3 }
        if (candidates.isEmpty()) return Activity.MOBILITY // always doable, anywhere

        val weights = candidates.map { weight(it, history) }
        var roll = random.nextDouble() * weights.sum()
        candidates.forEachIndexed { i, activity ->
            roll -= weights[i]
            if (roll <= 0) return activity
        }
        return candidates.last()
    }

    private fun weight(activity: Activity, history: List<Feedback>): Double {
        val mine = history.filter { it.activity == activity.name }
        val accepted = mine.count { it.outcome == Outcome.ACCEPTED }
        // "Busy" or "bad weather" isn't the activity's fault; only count real disinterest.
        val declined = mine.count {
            it.outcome == Outcome.DISMISSED ||
                (it.outcome == Outcome.REJECTED && it.reason != RejectReason.BUSY && it.reason != RejectReason.WEATHER)
        }
        val outdoorBoost = if (activity.outdoor) OUTDOOR_BOOST else 1.0 // it's a touch-grass app
        return outdoorBoost * (1.0 + accepted) / (1.0 + 0.5 * declined)
    }

    private companion object {
        const val AVOID_LAST = 2
        const val OUTDOOR_BOOST = 1.5
        const val REASON_MEMORY_MS = 6 * 60 * 60 * 1000L // tiredness and weather change within the day
    }
}
