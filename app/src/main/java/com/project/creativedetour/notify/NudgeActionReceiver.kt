package com.project.creativedetour.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.project.creativedetour.app
import com.project.creativedetour.data.Outcome
import com.project.creativedetour.engine.RejectReason
import kotlinx.coroutines.launch

/** Receives taps on the notification buttons and records them. */
class NudgeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(NotificationIds.EXTRA_FEEDBACK_ID, -1)
        if (id < 0) return
        val app = context.app
        val dao = app.database.feedbackDao()
        val now = System.currentTimeMillis()

        // onReceive must return fast; goAsync() lets us finish the DB write in a coroutine.
        val pending = goAsync()
        app.scope.launch {
            try {
                when (intent.action) {
                    NotificationIds.ACTION_ACCEPT -> {
                        dao.respond(id, Outcome.ACCEPTED, null, now)
                        app.notifier.cancelNudge()
                    }
                    NotificationIds.ACTION_NOT_NOW -> {
                        dao.respond(id, Outcome.REJECTED, null, now)
                        app.notifier.showReasonPicker(id)
                    }
                    NotificationIds.ACTION_REASON -> {
                        val reason = intent.getStringExtra(NotificationIds.EXTRA_REASON)?.let(RejectReason::valueOf)
                        dao.respond(id, Outcome.REJECTED, reason, now)
                        app.notifier.cancelNudge()
                    }
                    NotificationIds.ACTION_DISMISS -> dao.markDismissed(id, now)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
