package com.project.creativedetour

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.project.creativedetour.data.Outcome
import com.project.creativedetour.notify.NotificationIds
import com.project.creativedetour.sensing.Access
import com.project.creativedetour.service.DetourService
import com.project.creativedetour.ui.onboarding.OnboardingScreen
import com.project.creativedetour.ui.theme.CreativeDetourTheme
import com.project.creativedetour.ui.today.ReasonPickerDialog
import com.project.creativedetour.ui.today.TodayScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Set when opened from the "More…" button: which nudge we're picking a reason for. */
    private var pickReasonFor by mutableStateOf<Long?>(null)

    /** Bumped on every resume so permission checkmarks refresh after visiting Settings. */
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readReasonRequest(intent)
        startServiceIfReady()

        val dao = app.database.feedbackDao()
        setContent {
            CreativeDetourTheme {
                var onboarded by remember { mutableStateOf(app.onboarded) }
                var showSetup by remember { mutableStateOf(false) }
                val history by dao.observeRecent().collectAsState(initial = emptyList())
                val lastCheck by app.lastCheck.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (!onboarded || showSetup) {
                        OnboardingScreen(
                            refreshKey = resumeTick,
                            onFinish = {
                                app.onboarded = true
                                onboarded = true
                                showSetup = false
                                startServiceIfReady()
                            },
                            modifier = Modifier.padding(innerPadding),
                        )
                    } else {
                        TodayScreen(
                            history = history,
                            lastCheck = lastCheck,
                            onCheckNow = { app.scope.launch { app.runCheck() } },
                            onSendTestNudge = { app.scope.launch { app.runCheck(force = true) } },
                            onOpenSetup = { showSetup = true },
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
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

    override fun onResume() {
        super.onResume()
        resumeTick++
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

    /** Safe to call repeatedly: the service ignores extra starts. */
    private fun startServiceIfReady() {
        if (app.onboarded && Access.hasActivity(this)) DetourService.start(this)
    }
}
