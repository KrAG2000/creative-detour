package com.project.creativedetour.ai

import com.project.creativedetour.data.Feedback
import com.project.creativedetour.engine.Activity
import com.project.creativedetour.engine.NudgeContext

/** Everything a writer needs: the moment, the activity the app picked, and recent nudges (newest first). */
data class NudgeRequest(val ctx: NudgeContext, val activity: Activity, val history: List<Feedback>)

/** The sentence the user sees, plus who wrote it ("gemma-4-e2b·cpu", "template", ...). */
data class Nudge(val text: String, val writtenBy: String)

/** Turns a request into a nudge. Template, Gemma, or a fallback chain of both. */
interface NudgeWriter {
    suspend fun write(request: NudgeRequest): Nudge
}

/** Try [primary]; on any failure (no model, timeout, crash) use [fallback]. The user always gets a nudge. */
class FallbackNudgeWriter(private val primary: NudgeWriter, private val fallback: NudgeWriter) : NudgeWriter {
    override suspend fun write(request: NudgeRequest): Nudge =
        try {
            primary.write(request)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("NudgeWriter", "primary writer failed, using fallback", e)
            fallback.write(request)
        }
}
