package com.project.creativedetour.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.project.creativedetour.ui.ManageSpaceActivity
import com.project.creativedetour.ui.ModelCard

@Composable
fun SettingsScreen(refreshKey: Int, onSendTestNudge: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        SectionTitle("Permissions")
        PermissionCards(refreshKey)

        SectionTitle("AI model")
        ModelCard()

        SectionTitle("Storage")
        SettingCard(
            "Manage space",
            "Clear your history without losing the AI model, or delete the model to free about 2.6 GB.",
            "Open",
        ) { context.startActivity(Intent(context, ManageSpaceActivity::class.java)) }

        SectionTitle("Testing")
        SettingCard(
            "Send a test nudge",
            "Skips the rules and sends one right now. It counts as a real nudge, so automatic ones pause for 45 minutes.",
            "Send",
            onSendTestNudge,
        )

        Text(
            "Creative Detour · Gemma 4 E2B via LiteRT-LM · Everything it reads and learns stays on this phone.",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SettingCard(title: String, description: String, button: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onClick) { Text(button) }
        }
    }
}
