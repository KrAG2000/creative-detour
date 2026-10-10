package com.project.creativedetour.ai

import android.content.Context
import kotlinx.coroutines.CancellationException

/**
 * Writes nudges with Gemma 4 E2B on-device, on whichever backend AiCapability measured as best.
 * Any failure throws, and FallbackNudgeWriter switches to the template for this nudge.
 */
class GemmaNudgeWriter(
    private val context: Context,
    private val runtime: GemmaRuntime,
    private val capability: AiCapability,
    private val spec: ModelSpec = ModelCatalog.default,
) : NudgeWriter {

    override suspend fun write(request: NudgeRequest): Nudge {
        val model = ModelFiles.find(context, spec) ?: error("model not downloaded")
        val status = capability.ensureChecked() // first nudge after download runs the one-time test
        if (status !is AiCapability.Status.Ready) error("AI unavailable: $status")
        check(DeviceSpecs.hasHeadroomNow(context)) { "not enough free memory right now" }

        val out = try {
            runtime.generate(model, status.backend, NudgePrompt.SYSTEM, NudgePrompt.user(request), GENERATE_TIMEOUT_MS)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            capability.invalidate() // something changed since the test (driver, memory): re-test next time
            throw e
        }
        val text = NudgePrompt.clean(out.text) ?: error("model returned nothing usable: '${out.text}'")
        return Nudge(text, writtenBy = "${spec.id}·${status.backend.name.lowercase()}")
    }

    private companion object {
        const val GENERATE_TIMEOUT_MS = 60_000L
    }
}
