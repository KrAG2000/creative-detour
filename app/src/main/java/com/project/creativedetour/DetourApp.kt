package com.project.creativedetour

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import com.project.creativedetour.ai.ModelDownloader
import com.project.creativedetour.ai.NudgeWriter
import com.project.creativedetour.ai.TemplateNudgeWriter
import com.project.creativedetour.data.DetourDatabase
import com.project.creativedetour.data.Feedback
import com.project.creativedetour.engine.CheckResult
import com.project.creativedetour.engine.NudgeContext
import com.project.creativedetour.engine.NudgeEngine
import com.project.creativedetour.engine.RejectReason
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
    var writer: NudgeWriter = TemplateNudgeWriter()

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
        if (nudge) deliverNudge(ctx, recent.mapNotNull { it.reason }, now)
        return CheckResult(now, ctx, nudge).also { _lastCheck.value = it }
    }

    /** Write the message, record it as PENDING, and show it. */
    private suspend fun deliverNudge(ctx: NudgeContext, recentReasons: List<RejectReason>, now: Long) {
        val text = writer.write(ctx, recentReasons)
        val id = database.feedbackDao().insert(Feedback(shownAt = now, hourOfDay = ctx.hourOfDay, message = text))
        notifier.showNudge(id, text)
    }

    companion object {
        const val RECENT_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
    }
}

/** `context.app` instead of `(context.applicationContext as DetourApp)` everywhere. */
val Context.app: DetourApp get() = applicationContext as DetourApp
