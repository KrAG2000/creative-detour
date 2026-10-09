package com.project.creativedetour.engine

/**
 * Preset answers to "why not now?". Stored in the database by [name],
 * so never rename an entry — change the label instead.
 */
enum class RejectReason(val label: String, val cooldownMinutes: Int) {
    BUSY("Busy", 60),
    TIRED("Tired", 180),
    WEATHER("Bad weather", 120),
    ALREADY_ACTIVE("Already moved today", 240),
}
