package com.project.creativedetour.ai

/** No-AI fallback: always available, instant, works on any phone. */
class TemplateNudgeWriter : NudgeWriter {
    override suspend fun write(request: NudgeRequest): Nudge {
        val minutes = request.ctx.minutesUntilNextEvent
        val text = if (minutes != null && minutes in 15..60) {
            "$minutes min until ${request.ctx.nextEventTitle ?: "your next event"}. ${request.activity.template}"
        } else {
            request.activity.template
        }
        return Nudge(text, writtenBy = "template")
    }
}
