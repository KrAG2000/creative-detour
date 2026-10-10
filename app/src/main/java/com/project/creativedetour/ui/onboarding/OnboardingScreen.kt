package com.project.creativedetour.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.project.creativedetour.sensing.Access
import com.project.creativedetour.ui.ModelCard

/**
 * Play's "prominent disclosure": each card says what we read and why *before* the system prompt.
 * [refreshKey] changes whenever the activity resumes (e.g. coming back from a Settings screen).
 */
@Composable
fun OnboardingScreen(refreshKey: Int, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var localTick by remember { mutableIntStateOf(0) }
    val key = refreshKey + localTick
    val notifications = remember(key) { Access.hasNotifications(context) }
    val activity = remember(key) { Access.hasActivity(context) }
    val calendar = remember(key) { Access.hasCalendar(context) }
    val usage = remember(key) { Access.hasUsageAccess(context) }
    val battery = remember(key) { Access.ignoresBatteryOptimizations(context) }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { localTick++ }
    val packageUri = Uri.fromParts("package", context.packageName, null)

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Set up Creative Detour", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Detour notices good moments in your day to move a little. " +
                "Everything it reads is processed on this phone and never uploaded.",
            style = MaterialTheme.typography.bodyMedium,
        )

        StepCard(
            title = "Notifications", required = true, done = notifications,
            why = "Nudges arrive as notifications. At most one every 45 minutes, never at night.",
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        StepCard(
            title = "Physical activity", required = true, done = activity,
            why = "Counts steps with your phone's built-in step sensor, so Detour stays quiet when you're already moving.",
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) askPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        StepCard(
            title = "Calendar", required = false, done = calendar,
            why = "Reads the time and title of your next event, so nudges fit your free gaps and never land right before a meeting. Read-only.",
        ) {
            askPermission.launch(Manifest.permission.READ_CALENDAR)
        }
        StepCard(
            title = "Usage access", required = false, done = usage, button = "Open settings",
            why = "Lets Detour see how long your screen has been on. It only reads screen on/off times, not which apps you use. " +
                "Find Creative Detour in the list and turn it on.",
        ) {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        StepCard(
            title = "Unrestricted battery", required = false, done = battery, button = "Open app info",
            why = "Stops Android from pausing Detour in the background. It's built to use very little battery. " +
                "Tap App battery usage → Unrestricted.",
        ) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
        }

        ModelCard()

        Button(onClick = onFinish, enabled = notifications && activity, modifier = Modifier.fillMaxWidth()) {
            Text("Start Creative Detour")
        }
    }
}

@Composable
private fun StepCard(
    title: String,
    why: String,
    required: Boolean,
    done: Boolean,
    button: String = "Allow",
    onClick: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val tag = if (required) "Required" else "Optional"
            Text("${if (done) "✅" else "⬜"} $title · $tag", style = MaterialTheme.typography.titleMedium)
            Text(why, style = MaterialTheme.typography.bodyMedium)
            if (!done) OutlinedButton(onClick = onClick) { Text(button) }
        }
    }
}
