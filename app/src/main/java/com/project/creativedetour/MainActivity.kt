package com.project.creativedetour

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.project.creativedetour.data.Outcome
import com.project.creativedetour.engine.NudgeContext
import com.project.creativedetour.notify.NotificationIds
import com.project.creativedetour.ui.theme.CreativeDetourTheme
import com.project.creativedetour.ui.today.ReasonPickerDialog
import com.project.creativedetour.ui.today.TodayScreen
import kotlinx.coroutines.launch
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    /** Set when opened from the "More…" button: which nudge we're picking a reason for. */
    private var pickReasonFor by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readReasonRequest(intent)

        val dao = app.database.feedbackDao()
        setContent {
            CreativeDetourTheme {
                val askNotifications = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* v0.1: if denied, nudges just won't show */ }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val history by dao.observeRecent().collectAsState(initial = emptyList())
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TodayScreen(
                        history = history,
                        onSendTestNudge = ::sendTestNudge,
                        modifier = Modifier.padding(innerPadding),
                    )
                }

                pickReasonFor?.let { id ->
                    ReasonPickerDialog(
                        onPick = { reason ->
                            app.scope.launch {
                                dao.respond(id, Outcome.REJECTED, reason, System.currentTimeMillis())
                            }
                            app.notifier.cancelNudge()
                            pickReasonFor = null
                        },
                        onDismiss = { pickReasonFor = null },
                    )
                }
            }
        }
    }

    // The activity may already be open when "More…" is tapped; then the intent arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readReasonRequest(intent)
    }

    private fun readReasonRequest(intent: Intent) {
        val id = intent.getLongExtra(NotificationIds.EXTRA_FEEDBACK_ID, -1)
        if (id >= 0) pickReasonFor = id
    }

    /** Fake context until Block B wires up the real sensors. */
    private fun sendTestNudge() {
        val ctx = NudgeContext(
            hourOfDay = LocalTime.now().hour,
            stepsLastHour = 40,
            screenMinutesLastHour = 42,
            minutesSinceLastUsage = 0,
            minutesUntilNextEvent = null,
            nextEventTitle = null,
            minutesSinceLastNudge = null,
        )
        app.scope.launch { app.deliverNudge(ctx) }
    }
}
