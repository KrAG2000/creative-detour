package com.project.creativedetour.ai

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat
import com.project.creativedetour.engine.Activity
import com.project.creativedetour.engine.NudgeContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Can this phone run the model, and on which backend? Specs can't tell us whether a GPU driver
 * works or how fast a CPU really is, so we measure: write one sample nudge on GPU and on CPU,
 * keep the fastest backend that works. The result is remembered per model + OS build + app version,
 * so a system update (new GPU drivers) triggers a fresh test.
 */
class AiCapability(
    private val context: Context,
    private val runtime: GemmaRuntime,
    private val spec: ModelSpec = ModelCatalog.default,
) {
    sealed interface Status {
        /** Not tested yet (usually: model not downloaded). */
        data object Unknown : Status
        data object Checking : Status
        data class Ready(val backend: AiBackend, val secondsPerNudge: Int) : Status
        /** [permanent] = a hardware blocker; don't offer the download at all. */
        data class Unsupported(val reason: String, val permanent: Boolean = false) : Status
    }

    private val prefs = context.getSharedPreferences("ai_capability", Context.MODE_PRIVATE)
    private val checkLock = Mutex()
    private val _status = MutableStateFlow(load())
    val status: StateFlow<Status> = _status

    /** Returns the known result, running the test first if there isn't one. */
    suspend fun ensureChecked(): Status = when (val s = _status.value) {
        is Status.Ready, is Status.Unsupported -> s
        else -> check()
    }

    suspend fun check(): Status = checkLock.withLock {
        DeviceSpecs.blocker(context)?.let { return save(Status.Unsupported(it, permanent = true)) }
        val model = ModelFiles.find(context, spec) ?: return Status.Unknown.also { _status.value = it }
        _status.value = Status.Checking

        // If a GPU test ever crashed the whole process (native code can), this flag survives: skip GPU.
        val gpuCrashedBefore = prefs.getBoolean(KEY_TRYING_GPU, false)
        val timings = mutableMapOf<AiBackend, Long>()
        var timedOut = false
        for (backend in AiBackend.entries) {
            if (backend == AiBackend.GPU && gpuCrashedBefore) continue
            if (backend == AiBackend.GPU) prefs.edit(commit = true) { putBoolean(KEY_TRYING_GPU, true) }
            try {
                val out = runtime.generate(model, backend, NudgePrompt.SYSTEM, SAMPLE_PROMPT, CHECK_TIMEOUT_MS)
                if (NudgePrompt.clean(out.text) != null) timings[backend] = out.totalMs
                Log.i(TAG, "$backend: load ${out.loadMs} ms + generate ${out.generateMs} ms → '${out.text.trim()}'")
            } catch (e: CancellationException) {
                throw e
            } catch (e: GenerationTimeoutException) {
                timedOut = true
                Log.w(TAG, "$backend too slow", e)
            } catch (e: Exception) {
                Log.w(TAG, "$backend failed", e)
            } finally {
                if (backend == AiBackend.GPU) prefs.edit(commit = true) { putBoolean(KEY_TRYING_GPU, false) }
            }
        }

        val best = timings.minByOrNull { it.value }
        save(
            when {
                best == null && timedOut -> Status.Unsupported("Too slow on this phone: a nudge took over a minute.")
                best == null -> Status.Unsupported("The AI model couldn't run on this phone.")
                best.value > MAX_ACCEPTABLE_MS ->
                    Status.Unsupported("Too slow on this phone: about ${best.value / 1000} s per nudge.")
                else -> Status.Ready(best.key, (best.value / 1000).toInt().coerceAtLeast(1))
            }
        )
    }

    /** The chosen backend failed during real use: forget the result so the next nudge re-tests. */
    fun invalidate() {
        prefs.edit { remove(KEY_FINGERPRINT) }
        _status.value = Status.Unknown
    }

    private fun save(status: Status): Status {
        prefs.edit {
            putString(KEY_FINGERPRINT, fingerprint())
            when (status) {
                is Status.Ready -> putString(KEY_BACKEND, status.backend.name).putInt(KEY_SECONDS, status.secondsPerNudge)
                is Status.Unsupported -> putString(KEY_BACKEND, NONE).putString(KEY_REASON, status.reason)
                else -> Unit
            }
        }
        _status.value = status
        return status
    }

    private fun load(): Status {
        DeviceSpecs.blocker(context)?.let { return Status.Unsupported(it, permanent = true) }
        if (prefs.getString(KEY_FINGERPRINT, null) != fingerprint()) {
            prefs.edit { putBoolean(KEY_TRYING_GPU, false) } // new OS/app/model: give the GPU another chance
            return Status.Unknown
        }
        return when (val backend = prefs.getString(KEY_BACKEND, null)) {
            null -> Status.Unknown
            NONE -> Status.Unsupported(prefs.getString(KEY_REASON, null) ?: "The AI model couldn't run on this phone.")
            else -> Status.Ready(AiBackend.valueOf(backend), prefs.getInt(KEY_SECONDS, 0))
        }
    }

    private fun fingerprint(): String {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return "${spec.id}|${Build.FINGERPRINT}|${PackageInfoCompat.getLongVersionCode(info)}"
    }

    private companion object {
        const val TAG = "AiCapability"
        const val KEY_FINGERPRINT = "fingerprint"
        const val KEY_BACKEND = "backend"
        const val KEY_SECONDS = "seconds"
        const val KEY_REASON = "reason"
        const val KEY_TRYING_GPU = "trying_gpu"
        const val NONE = "none"
        const val CHECK_TIMEOUT_MS = 60_000L
        const val MAX_ACCEPTABLE_MS = 45_000L

        /** A realistic nudge request, so the test measures what real use costs. */
        val SAMPLE_PROMPT = NudgePrompt.user(
            NudgeRequest(
                NudgeContext(
                    hourOfDay = 15, stepsLastHour = 120, screenMinutesLastHour = 40, minutesSinceLastUsage = 0,
                    minutesUntilNextEvent = null, nextEventTitle = null, minutesSinceLastNudge = null,
                ),
                activity = Activity.STRETCH,
                history = emptyList(),
            )
        )
    }
}
