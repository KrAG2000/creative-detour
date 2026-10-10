package com.project.creativedetour

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.project.creativedetour.data.Outcome
import com.project.creativedetour.notify.NotificationIds
import com.project.creativedetour.sensing.Access
import com.project.creativedetour.service.DetourService
import com.project.creativedetour.ui.ReasonPickerDialog
import com.project.creativedetour.ui.activity.ActivityScreen
import com.project.creativedetour.ui.dayLabel
import com.project.creativedetour.ui.onboarding.OnboardingScreen
import com.project.creativedetour.ui.settings.SettingsScreen
import com.project.creativedetour.ui.theme.CreativeDetourTheme
import com.project.creativedetour.ui.today.TodayScreen
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Filled.Home),
    ACTIVITY("Activity", Icons.AutoMirrored.Filled.List),
    SETTINGS("Settings", Icons.Filled.Settings),
}

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
                var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
                val history by dao.observeRecent(HISTORY_LIMIT).collectAsState(initial = emptyList())
                val lastCheck by app.lastCheck.collectAsState()
                val nextCheckAt by app.nextCheckAt.collectAsState()

                if (!onboarded) {
                    Scaffold(Modifier.fillMaxSize()) { padding ->
                        OnboardingScreen(
                            refreshKey = resumeTick,
                            onFinish = {
                                app.onboarded = true
                                onboarded = true
                                startServiceIfReady()
                            },
                            modifier = Modifier.padding(padding),
                        )
                    }
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar {
                                Tab.entries.forEach {
                                    NavigationBarItem(
                                        selected = tab == it,
                                        onClick = { tab = it },
                                        icon = { Icon(it.icon, contentDescription = null) },
                                        label = { Text(it.label) },
                                    )
                                }
                            }
                        },
                    ) { padding ->
                        val content = Modifier.padding(padding)
                        when (tab) {
                            Tab.TODAY -> TodayScreen(
                                lastCheck = lastCheck,
                                nextCheckAt = nextCheckAt,
                                today = history.filter { dayLabel(it.shownAt) == "Today" },
                                onCheckNow = app::requestCheck,
                                modifier = content,
                            )
                            Tab.ACTIVITY -> ActivityScreen(history, content)
                            Tab.SETTINGS -> SettingsScreen(
                                refreshKey = resumeTick,
                                onSendTestNudge = { app.scope.launch { app.runCheck(force = true) } },
                                modifier = content,
                            )
                        }
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

    private companion object {
        const val HISTORY_LIMIT = 300
    }
}
