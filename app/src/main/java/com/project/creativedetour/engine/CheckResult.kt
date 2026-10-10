package com.project.creativedetour.engine

/** What the last check saw and decided. Shown on the home screen so you can see the engine think. */
data class CheckResult(val at: Long, val ctx: NudgeContext, val nudged: Boolean)
