package com.project.creativedetour.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.project.creativedetour.app
import com.project.creativedetour.engine.CheckResult
import com.project.creativedetour.engine.Decision
import com.project.creativedetour.engine.NudgeEngine
import com.project.creativedetour.notify.NotificationIds
import com.project.creativedetour.sensing.Access
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs all the time (foreground service type "health": we track steps).
 *
 * Adaptive loop: after each check it plans the next one from the engine's decision
 * (45 min after a nudge, end of a cooldown, otherwise ~10 min). Unlocking the phone wakes it
 * early, because "back on the phone" is exactly the moment worth a look. While the phone sleeps,
 * the timer sleeps too, which is fine: nobody is on their screen then.
 */
class DetourService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null
    private var unlockCheck: Job? = null

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            unlockCheck?.cancel() // several unlocks in a row → one check, 2 min after the last
            unlockCheck = scope.launch {
                delay(UNLOCK_CHECK_DELAY_MS)
                app.serviceWake.trySend(Unit)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 14+ throws if a health service starts without activity permission.
        if (!Access.hasActivity(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(this, NotificationIds.SERVICE_ONGOING, app.notifier.serviceNotification(), foregroundType())

        if (loop == null) { // onStartCommand runs again on every start() call; only one loop
            app.stepSource.start()
            ContextCompat.registerReceiver(
                this, unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            loop = scope.launch { runLoop() }
        }
        return START_STICKY // if Android kills us, recreate the service when it can
    }

    private suspend fun runLoop() {
        delay(FIRST_CHECK_DELAY_MS) // give the step counter a moment to report
        while (currentCoroutineContext().isActive) {
            val result = try {
                app.runCheck()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "check failed", e)
                null
            }
            val now = System.currentTimeMillis()
            val next = planNext(result, now)
            app.setNextCheck(next)
            app.notifier.updateServiceNotification(result?.decision, next)
            withTimeoutOrNull(next - now) { app.serviceWake.receive() } // sleep until planned time, or an early wake
        }
    }

    private fun planNext(result: CheckResult?, now: Long): Long = when (val decision = result?.decision) {
        Decision.Nudge -> now + NudgeEngine.MIN_GAP_MINUTES * 60_000L
        is Decision.Quiet -> (decision.retryAt ?: (now + DEFAULT_INTERVAL_MS))
            .coerceIn(now + MIN_INTERVAL_MS, now + MAX_INTERVAL_MS)
        null -> now + DEFAULT_INTERVAL_MS
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(unlockReceiver) }
        app.stepSource.stop()
        app.setNextCheck(null)
        scope.cancel()
        super.onDestroy()
    }

    private fun foregroundType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0

    companion object {
        private const val TAG = "DetourService"
        private const val FIRST_CHECK_DELAY_MS = 60_000L
        private const val UNLOCK_CHECK_DELAY_MS = 2 * 60_000L
        private const val MIN_INTERVAL_MS = 5 * 60_000L
        private const val DEFAULT_INTERVAL_MS = 10 * 60_000L
        private const val MAX_INTERVAL_MS = 60 * 60_000L // re-read the world at least hourly

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, DetourService::class.java))
    }
}
