package com.project.creativedetour.ai

import com.project.creativedetour.engine.NudgeContext
import com.project.creativedetour.engine.RejectReason

/** Turns a context snapshot into the sentence the user sees. Template now, Gemma later. */
interface NudgeWriter {
    suspend fun write(ctx: NudgeContext, recentReasons: List<RejectReason>): String
}
