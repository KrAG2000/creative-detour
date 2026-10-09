package com.project.creativedetour.engine

/**
 * A snapshot of "what's going on right now", built by ContextReader every few minutes.
 * Everything except the clock can be missing (no sensor, permission denied, empty calendar),
 * so those fields are nullable and the engine has to cope with null.
 */
data class NudgeContext(
    val hourOfDay: Int,
    val stepsLastHour: Int?,
    val screenMinutesLastHour: Int?,
    val minutesSinceLastUsage: Int?,
    val minutesUntilNextEvent: Int?,
    val nextEventTitle: String?,
    val minutesSinceLastNudge: Int?,
)
