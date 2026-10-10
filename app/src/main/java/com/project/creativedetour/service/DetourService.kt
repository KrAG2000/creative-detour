package com.project.creativedetour.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.project.creativedetour.app
import com.project.creativedetour.notify.NotificationIds
import com.project.creativedetour.sensing.Access
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runs all the time (foreground service type "health": we track steps).
 * Every CHECK_INTERVAL it reads the context and lets the engine decide.
 * While the phone sleeps, `delay` sleeps too — fine, nobody is on their screen then.
 */
class DetourService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

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
            loop = scope.launch {
                delay(FIRST_CHECK_DELAY_MS) // give the step counter a moment to report
                while (isActive) {
                    try {
                        app.runCheck()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "check failed", e)
                    }
                    delay(CHECK_INTERVAL_MS)
                }
            }
        }
        return START_STICKY // if Android kills us, recreate the service when it can
    }

    override fun onDestroy() {
        app.stepSource.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun foregroundType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0

    companion object {
        private const val TAG = "DetourService"
        private const val FIRST_CHECK_DELAY_MS = 60_000L
        private const val CHECK_INTERVAL_MS = 10 * 60_000L

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, DetourService::class.java))
    }
}
