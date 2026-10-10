package com.project.creativedetour.ui

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.creativedetour.ai.ModelFiles
import com.project.creativedetour.app
import com.project.creativedetour.service.DetourService
import com.project.creativedetour.ui.theme.CreativeDetourTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Declared as android:manageSpaceActivity, so Settings shows "Manage space" here instead of
 * "Clear storage". Lets people wipe their data without losing the 2.5 GB model.
 */
class ManageSpaceActivity : ComponentActivity() {

    private enum class Action(val title: String, val confirm: String) {
        HISTORY("Clear history & settings", "Deletes your nudge history and setup. The AI model stays."),
        MODEL(
            "Delete AI model",
            "Frees about 2.6 GB. The model can't be recovered: you'll have to download it again (Wi-Fi). " +
                "Until then, nudges use simple templates.",
        ),
        EVERYTHING(
            "Delete everything",
            "Deletes all app data, including the AI model. Nothing can be recovered: you'll set up the app " +
                "and download the model (about 2.6 GB) again. Same as Android's Clear storage.",
        ),
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CreativeDetourTheme {
                var refresh by remember { mutableIntStateOf(0) }
                var pending by remember { mutableStateOf<Action?>(null) }
                val modelSize = remember(refresh) { Formatter.formatFileSize(this, ModelFiles.totalBytes(this)) }

                Scaffold(Modifier.fillMaxSize()) { padding ->
                    Column(
                        Modifier.padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Manage space", style = MaterialTheme.typography.headlineMedium)
                        Text("AI model on this phone: $modelSize", style = MaterialTheme.typography.bodyMedium)
                        Action.entries.forEach { action ->
                            ActionCard(action.title, action.confirm) { pending = action }
                        }
                    }
                }

                pending?.let { action ->
                    AlertDialog(
                        onDismissRequest = { pending = null },
                        title = { Text("${action.title}?") },
                        text = { Text(action.confirm) },
                        confirmButton = {
                            TextButton(onClick = {
                                pending = null
                                perform(action) { refresh++ }
                            }) { Text("Delete") }
                        },
                        dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
                    )
                }
            }
        }
    }

    private fun perform(action: Action, onDone: () -> Unit) {
        app.scope.launch {
            withContext(Dispatchers.IO) {
                when (action) {
                    Action.HISTORY -> {
                        stopService(Intent(this@ManageSpaceActivity, DetourService::class.java))
                        app.database.clearAllTables()
                        app.onboarded = false
                    }
                    Action.MODEL -> ModelFiles.deleteAll(this@ManageSpaceActivity)
                    // Wipes everything and kills the app, exactly like Settings would.
                    Action.EVERYTHING -> getSystemService(ActivityManager::class.java).clearApplicationUserData()
                }
            }
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}

@Composable
private fun ActionCard(title: String, description: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onClick) { Text(title) }
        }
    }
}
