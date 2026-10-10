package com.project.creativedetour.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.project.creativedetour.sensing.Access
import com.project.creativedetour.ui.ModelCard
import com.project.creativedetour.ui.settings.PermissionCards

/** First run: explain, ask for permissions (each with a disclosure), offer the model, start. */
@Composable
fun OnboardingScreen(refreshKey: Int, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ready = remember(refreshKey) { Access.hasNotifications(context) && Access.hasActivity(context) }

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
        PermissionCards(refreshKey)
        ModelCard()
        Button(onClick = onFinish, enabled = ready, modifier = Modifier.fillMaxWidth()) {
            Text("Start Creative Detour")
        }
    }
}
