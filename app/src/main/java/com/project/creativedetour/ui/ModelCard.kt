package com.project.creativedetour.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.project.creativedetour.ai.AiCapability
import com.project.creativedetour.ai.ModelCatalog
import com.project.creativedetour.ai.ModelDownloader.State
import com.project.creativedetour.app
import kotlinx.coroutines.launch

/** Download → test → "runs on GPU/CPU" card for the on-device model. Used in onboarding and on the home screen. */
@Composable
fun ModelCard(modifier: Modifier = Modifier) {
    val app = LocalContext.current.app
    val spec = ModelCatalog.default
    val downloader = app.modelDownloader
    val polled by remember { downloader.states(spec) }.collectAsState(initial = State.NotDownloaded)
    val capability by app.aiCapability.status.collectAsState()
    var startError by remember { mutableStateOf<State.Failed?>(null) }
    val state = startError ?: polled
    val gb = "%.1f GB".format(spec.sizeBytes / 1_000_000_000.0)

    // Model just became available and was never tested: test it once, in the app scope so leaving the screen doesn't stop it.
    LaunchedEffect(state is State.Ready, capability) {
        if (state is State.Ready && capability == AiCapability.Status.Unknown) {
            app.scope.launch { app.aiCapability.check() }
        }
    }

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val blocked = capability as? AiCapability.Status.Unsupported
            val icon = when {
                blocked != null -> "⛔"
                state is State.Ready && capability is AiCapability.Status.Ready -> "✅"
                else -> "⬜"
            }
            Text("$icon AI model · Optional", style = MaterialTheme.typography.titleMedium)

            // A hardware blocker: never offer 2.6 GB that can't run.
            if (blocked != null && blocked.permanent) {
                Text("${blocked.reason} Detour uses simple templates instead.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            Text(
                "${spec.displayName} writes your nudges entirely on this phone, offline. " +
                    "One-time $gb download from Hugging Face, over Wi-Fi. Without it, Detour uses simple templates.",
                style = MaterialTheme.typography.bodyMedium,
            )

            when (state) {
                State.NotDownloaded -> OutlinedButton(onClick = {
                    startError = downloader.start(spec) as? State.Failed
                }) { Text("Download ($gb)") }

                is State.Downloading -> {
                    val fraction = state.bytes.toFloat() / state.total
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val label = if (state.waitingForWifi) "Waiting for Wi-Fi…" else "${(fraction * 100).toInt()}% downloaded"
                        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { downloader.cancel(spec) }) { Text("Cancel") }
                    }
                }

                State.Verifying -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Checking the file…", style = MaterialTheme.typography.bodySmall)
                }

                is State.Ready -> CapabilityLine(capability) { app.scope.launch { app.aiCapability.check() } }

                is State.Failed -> {
                    Text(state.reason, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = {
                        startError = null
                        startError = downloader.start(spec) as? State.Failed
                    }) { Text("Retry") }
                }
            }
        }
    }
}

/** What the one-time test found. */
@Composable
private fun CapabilityLine(status: AiCapability.Status, onRetest: () -> Unit) {
    when (status) {
        AiCapability.Status.Unknown, AiCapability.Status.Checking -> {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                "Testing what your phone can do. One time, up to about two minutes.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        is AiCapability.Status.Ready -> {
            Text(
                "Runs on your phone's ${status.backend} · about ${status.secondsPerNudge} s per nudge. " +
                    "Clearing the app's storage deletes the model, and it can't be recovered: you'd have to download it again.",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onRetest) { Text("Re-test") }
        }
        is AiCapability.Status.Unsupported -> {
            Text(
                "${status.reason} Detour uses simple templates instead.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onRetest) { Text("Re-test") }
        }
    }
}
