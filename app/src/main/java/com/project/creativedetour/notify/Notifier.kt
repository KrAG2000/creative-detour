package com.project.creativedetour.notify

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.project.creativedetour.MainActivity
import com.project.creativedetour.R
import com.project.creativedetour.engine.RejectReason
import com.project.creativedetour.notify.NotificationIds.ACTION_ACCEPT
import com.project.creativedetour.notify.NotificationIds.ACTION_DISMISS
import com.project.creativedetour.notify.NotificationIds.ACTION_NOT_NOW
import com.project.creativedetour.notify.NotificationIds.ACTION_REASON
import com.project.creativedetour.notify.NotificationIds.CHANNEL_NUDGES
import com.project.creativedetour.notify.NotificationIds.CHANNEL_REASONS
import com.project.creativedetour.notify.NotificationIds.CHANNEL_SERVICE
import com.project.creativedetour.notify.NotificationIds.EXTRA_FEEDBACK_ID
import com.project.creativedetour.notify.NotificationIds.EXTRA_REASON
import com.project.creativedetour.notify.NotificationIds.NUDGE
import com.project.creativedetour.notify.NotificationIds.NUDGE_REASONS

/**
 * Notifications allow max 3 buttons, so the reason flow is two steps:
 *   nudge:   [Let's go] [Not now]
 *   reasons: [Busy] [Tired] [More…]  ← More… opens the app with every reason
 */
class Notifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_NUDGES, "Nudges", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Small ideas to get you moving" },
                // High importance so it pops up on screen, but without sound or vibration.
                NotificationChannel(CHANNEL_REASONS, "Quick reasons", NotificationManager.IMPORTANCE_HIGH)
                    .apply {
                        description = "The follow-up question after you tap Not now"
                        setSound(null, null)
                        enableVibration(false)
                    },
                NotificationChannel(CHANNEL_SERVICE, "Background watcher", NotificationManager.IMPORTANCE_MIN)
                    .apply { description = "Keeps Creative Detour running" },
            )
        )
    }

    fun showNudge(feedbackId: Long, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_NUDGES)
            .setSmallIcon(R.drawable.ic_nudge)
            .setContentTitle("Creative Detour")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDeleteIntent(broadcast(ACTION_DISMISS, feedbackId))
            .addAction(0, "Let's go", broadcast(ACTION_ACCEPT, feedbackId))
            .addAction(0, "Not now", broadcast(ACTION_NOT_NOW, feedbackId))
            .build()
        post(notification)
    }

    /**
     * A *new* notification (own ID, own quiet-but-high channel) instead of a silent update of the nudge:
     * a silent update can't pop up again, so it looked like it vanished. Stays until swiped or answered.
     */
    fun showReasonPicker(feedbackId: Long) {
        manager.cancel(NUDGE)
        val notification = NotificationCompat.Builder(context, CHANNEL_REASONS)
            .setSmallIcon(R.drawable.ic_nudge)
            .setContentTitle("No worries. What's stopping you?")
            .setContentText("One tap helps me pick better moments.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, RejectReason.BUSY.label, broadcast(ACTION_REASON, feedbackId, RejectReason.BUSY))
            .addAction(0, RejectReason.TIRED.label, broadcast(ACTION_REASON, feedbackId, RejectReason.TIRED))
            .addAction(0, "More…", openReasonScreen(feedbackId))
            .build()
        post(notification, NUDGE_REASONS)
    }

    /** The always-on notification Android requires for a foreground service. Minimal channel = tucked away. */
    fun serviceNotification(): Notification =
        NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_nudge)
            .setContentTitle("Watching for good moments to move")
            .setContentText("Everything stays on this phone.")
            .setOngoing(true)
            .setContentIntent(openApp())
            .build()

    fun cancelNudge() {
        manager.cancel(NUDGE)
        manager.cancel(NUDGE_REASONS)
    }

    private fun openApp(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(context, 0, intent, PENDING_FLAGS)
    }

    @SuppressLint("MissingPermission") // checked via areNotificationsEnabled()
    private fun post(notification: Notification, id: Int = NUDGE) {
        if (manager.areNotificationsEnabled()) manager.notify(id, notification)
    }

    private fun broadcast(action: String, feedbackId: Long, reason: RejectReason? = null): PendingIntent {
        val intent = Intent(context, NudgeActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_FEEDBACK_ID, feedbackId)
            reason?.let { putExtra(EXTRA_REASON, it.name) }
        }
        // Each button needs a distinct request code, or Android reuses one PendingIntent for all of them.
        val requestCode = "$action/$feedbackId/${reason?.name}".hashCode()
        return PendingIntent.getBroadcast(context, requestCode, intent, PENDING_FLAGS)
    }

    private fun openReasonScreen(feedbackId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_FEEDBACK_ID, feedbackId)
        }
        return PendingIntent.getActivity(context, "more/$feedbackId".hashCode(), intent, PENDING_FLAGS)
    }

    private companion object {
        const val PENDING_FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    }
}
