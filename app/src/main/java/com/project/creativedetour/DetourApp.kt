package com.project.creativedetour

import android.app.Application
import android.content.Context
import com.project.creativedetour.ai.NudgeWriter
import com.project.creativedetour.ai.TemplateNudgeWriter
import com.project.creativedetour.data.DetourDatabase
import com.project.creativedetour.data.Feedback
import com.project.creativedetour.engine.NudgeContext
import com.project.creativedetour.engine.NudgeEngine
import com.project.creativedetour.notify.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Created by Android once, before any Activity, Service or Receiver.
 * Holds the app-wide singletons so every component shares the same database and notifier.
 */
class DetourApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database by lazy { DetourDatabase.create(this) }
    val notifier by lazy { Notifier(this) }
    val engine = NudgeEngine()
    var writer: NudgeWriter = TemplateNudgeWriter()

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
    }

    /** Write the message, record it as PENDING, and show it. */
    suspend fun deliverNudge(ctx: NudgeContext) {
        val dao = database.feedbackDao()
        val now = System.currentTimeMillis()
        val recentReasons = dao.since(now - RECENT_WINDOW_MS).mapNotNull { it.reason }
        val text = writer.write(ctx, recentReasons)
        val id = dao.insert(Feedback(shownAt = now, hourOfDay = ctx.hourOfDay, message = text))
        notifier.showNudge(id, text)
    }

    companion object {
        const val RECENT_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
    }
}

/** `context.app` instead of `(context.applicationContext as DetourApp)` everywhere. */
val Context.app: DetourApp get() = applicationContext as DetourApp
