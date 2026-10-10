package com.project.creativedetour.ai

import com.project.creativedetour.data.Feedback
import com.project.creativedetour.data.Outcome
import com.project.creativedetour.engine.RejectReason

/** Builds the prompt: fixed rules (system) + the moment, the chosen activity, and recent nudges. */
object NudgePrompt {
    val SYSTEM = """
        You write one short, friendly nudge that gets the user off their phone and moving for a few minutes.
        You are told which kind of activity to suggest. Make it specific: pick one concrete idea and make it sound fun.
        Rules: one or two sentences, under 30 words. Doable right now. Warm, never preachy, no guilt.
        Never start with "Time to". Don't reuse ideas or phrases from the earlier nudges you are shown.
        No hashtags, no quotes, at most one emoji. Reply with the nudge only.
    """.trimIndent()

    fun user(request: NudgeRequest): String = buildString {
        val ctx = request.ctx
        appendLine("Facts about right now:")
        appendLine("- Time: ${ctx.hourOfDay}:00 (${partOfDay(ctx.hourOfDay)})")
        ctx.screenMinutesLastHour?.let { appendLine("- Screen on in the last hour: $it minutes") }
        ctx.stepsLastHour?.let { appendLine("- Steps in the last hour: $it") }
        if (ctx.minutesUntilNextEvent != null) {
            appendLine("- Next calendar event: \"${ctx.nextEventTitle ?: "an event"}\" in ${ctx.minutesUntilNextEvent} minutes. Fit the activity before it.")
        }

        val activity = request.activity
        appendLine()
        appendLine("Suggest ${activity.label} (about ${activity.minutes} minutes). Ideas to choose from: ${activity.ideas}.")

        val reasons = request.history.mapNotNull { it.reason }.toSet()
        if (RejectReason.TIRED in reasons) appendLine("They've said they're tired lately: keep the tone gentle.")
        if (RejectReason.BUSY in reasons) appendLine("They've been busy lately: stress how quick it is.")

        val earlier = request.history.take(EARLIER_NUDGES)
        if (earlier.isNotEmpty()) {
            appendLine()
            appendLine("Earlier nudges, newest first, and what the user did:")
            earlier.forEach { appendLine("- \"${it.message}\" → ${outcome(it)}") }
        }
        appendLine()
        append("Write the new nudge.")
    }

    private fun outcome(f: Feedback) = when (f.outcome) {
        Outcome.ACCEPTED -> "they did it"
        Outcome.REJECTED -> "declined" + (f.reason?.let { " (${it.label.lowercase()})" } ?: "")
        Outcome.DISMISSED -> "ignored it"
        Outcome.PENDING -> "no answer yet"
    }

    private fun partOfDay(hour: Int) = when (hour) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        in 17..20 -> "evening"
        else -> "night"
    }

    /** Models sometimes add quotes, preambles or a second paragraph. Keep the first clean line. */
    fun clean(raw: String): String? =
        raw.lineSequence().map { it.trim().trim('"', '“', '”', '*') }.firstOrNull { it.isNotBlank() }
            ?.take(MAX_CHARS)
            ?.takeIf { it.length >= MIN_CHARS }

    private const val EARLIER_NUDGES = 4
    private const val MIN_CHARS = 10
    private const val MAX_CHARS = 220
}
