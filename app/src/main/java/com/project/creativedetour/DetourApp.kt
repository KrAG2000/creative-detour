package com.project.creativedetour

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import com.project.creativedetour.ai.AiCapability
import com.project.creativedetour.ai.FallbackNudgeWriter
import com.project.creativedetour.ai.GemmaNudgeWriter
import com.project.creativedetour.ai.GemmaRuntime
import com.project.creativedetour.ai.ModelDownloader
import com.project.creativedetour.ai.NudgeRequest
import com.project.creativedetour.ai.NudgeWriter
import com.project.creativedetour.ai.TemplateNudgeWriter
import com.project.creativedetour.data.DetourDatabase
import com.project.creativedetour.data.Feedback
import com.project.creativedetour.engine.CheckResult
import com.project.creativedetour.engine.NudgeContext
import com.project.creativedetour.engine.NudgeEngine
import com.project.creativedetour.engine.ActivityPicker
import com.project.creativedetour.notify.Notifier
import com.project.creativedetour.sensing.ContextReader
import com.project.creativedetour.sensing.StepSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by Android once, before any Activity, Service or Receiver.
 * Holds the app-wide singletons so every component shares the same database and notifier.
 */
class DetourApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database by lazy { DetourDatabase.create(this) }
    val notifier by lazy { Notifier(this) }
    val stepSource by lazy { StepSource(this) }
    val modelDownloader by lazy { ModelDownloader(this) }
    val contextReader by lazy { ContextReader(this, stepSource, database.feedbackDao()) }
    val engine = NudgeEngine()
    private val activityPicker = ActivityPicker()
    val gemmaRuntime by lazy { GemmaRuntime(this) }
    val aiCapability by lazy { AiCapability(this, gemmaRuntime) }

    /** Gemma when the model is downloaded and the phone can run it; otherwise the template. */
    val writer: NudgeWriter by lazy {
        FallbackNudgeWriter(GemmaNudgeWriter(this, gemmaRuntime, aiCapability), TemplateNudgeWriter())
    }

    private val prefs by lazy { getSharedPreferences("detour", MODE_PRIVATE) }
    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(value) = prefs.edit { putBoolean("onboarded", value) }

    private val _lastCheck = MutableStateFlow<CheckResult?>(null)
    val lastCheck: StateFlow<CheckResult?> = _lastCheck

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
    }

    /** Read the real context, ask the engine, nudge if it says yes. [force] skips the rules (test button). */
    suspend fun runCheck(force: Boolean = false): CheckResult {
        val now = System.currentTimeMillis()
        val ctx = contextReader.read(now)
        val recent = database.feedbackDao().since(now - RECENT_WINDOW_MS)
        val nudge = force || engine.shouldNudge(ctx, recent, now)
        if (nudge) deliverNudge(ctx, recent, now)
        return CheckResult(now, ctx, nudge).also { _lastCheck.value = it }
    }

    /** Pick an activity, write the message, record it as PENDING, and show it. [recent] is newest first. */
    private suspend fun deliverNudge(ctx: NudgeContext, recent: List<Feedback>, now: Long) {
        val activity = activityPicker.pick(ctx, recent, now)
        val nudge = writer.write(NudgeRequest(ctx, activity, recent))
        val id = database.feedbackDao().insert(
            Feedback(
                shownAt = now, hourOfDay = ctx.hourOfDay, message = nudge.text,
                writtenBy = nudge.writtenBy, activity = activity.name,
            )
        )
        notifier.showNudge(id, nudge.text)
    }

    companion object {
        const val RECENT_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
    }
}

/** `context.app` instead of `(context.applicationContext as DetourApp)` everywhere. */
val Context.app: DetourApp get() = applicationContext as DetourApp
