package com.project.creativedetour.ai

import com.project.creativedetour.engine.NudgeContext
import com.project.creativedetour.engine.RejectReason

/** No-AI fallback: always available, instant, works on any phone. */
class TemplateNudgeWriter : NudgeWriter {
    override suspend fun write(ctx: NudgeContext, recentReasons: List<RejectReason>): String {
        val walk = if (RejectReason.TIRED in recentReasons) "a slow 5-minute stroll" else "a quick 5-minute walk"
        return when {
            ctx.minutesUntilNextEvent != null && ctx.minutesUntilNextEvent in 15..60 ->
                "${ctx.minutesUntilNextEvent} min until ${ctx.nextEventTitle ?: "your next event"}. Time for $walk first?"
            ctx.screenMinutesLastHour != null && ctx.screenMinutesLastHour >= 30 ->
                "${ctx.screenMinutesLastHour} min on your phone this past hour. How about $walk?"
            ctx.stepsLastHour != null && ctx.stepsLastHour < 100 ->
                "You've barely moved this hour. Stand up and take $walk?"
            else -> "Step outside for $walk and look at the sky?"
        }
    }
}
