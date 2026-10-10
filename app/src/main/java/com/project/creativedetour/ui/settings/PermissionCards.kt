package com.project.creativedetour.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.project.creativedetour.sensing.Access

/**
 * Play's "prominent disclosure": each card says what we read and why *before* the system prompt.
 * Shared by onboarding and Settings. [refreshKey] changes whenever the activity resumes
 * (e.g. coming back from a Settings screen), so the checkmarks stay current.
 */
@Composable
fun PermissionCards(refreshKey: Int) {
    val context = LocalContext.current
    var localTick by remember { mutableIntStateOf(0) }
    val key = refreshKey + localTick
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { localTick++ }
    val appInfo = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))) }

    PermissionCard(
        title = "Notifications", required = true, done = remember(key) { Access.hasNotifications(context) },
        why = "Nudges arrive as notifications. At most one every 45 minutes, never at night.",
        onManage = appInfo,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    PermissionCard(
        title = "Physical activity", required = true, done = remember(key) { Access.hasActivity(context) },
        why = "Counts steps with your phone's built-in step sensor, so Detour stays quiet when you're already moving.",
        onManage = appInfo,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) askPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }
    PermissionCard(
        title = "Calendar", required = false, done = remember(key) { Access.hasCalendar(context) },
        why = "Reads the time and title of your next event, so nudges fit your free gaps and never land right before a meeting. Read-only.",
        onManage = appInfo,
    ) {
        askPermission.launch(Manifest.permission.READ_CALENDAR)
    }
    PermissionCard(
        title = "Usage access", required = false, done = remember(key) { Access.hasUsageAccess(context) },
        button = "Open settings",
        why = "Lets Detour see how long your screen has been on. It only reads screen on/off times, not which apps you use. " +
            "Find Creative Detour in the list and turn it on.",
        onManage = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
    ) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
    PermissionCard(
        title = "Unrestricted battery", required = false, done = remember(key) { Access.ignoresBatteryOptimizations(context) },
        button = "Open app info",
        why = "Stops Android from pausing Detour in the background. It's built to use very little battery. " +
            "Tap App battery usage → Unrestricted.",
        onManage = appInfo,
        onClick = appInfo,
    )
}

@Composable
private fun PermissionCard(
    title: String,
    why: String,
    required: Boolean,
    done: Boolean,
    button: String = "Allow",
    onManage: () -> Unit,
    onClick: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val tag = if (required) "Required" else "Optional"
            Text("${if (done) "✅" else "⬜"} $title · $tag", style = MaterialTheme.typography.titleMedium)
            Text(why, style = MaterialTheme.typography.bodyMedium)
            // Granted permissions can only be revoked in system settings; offer the way there.
            if (done) TextButton(onClick = onManage) { Text("Manage") } else OutlinedButton(onClick = onClick) { Text(button) }
        }
    }
}
