package com.project.creativedetour.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.creativedetour.data.Feedback
import com.project.creativedetour.data.Outcome
import com.project.creativedetour.engine.Activity
import com.project.creativedetour.engine.CheckResult
import com.project.creativedetour.engine.Decision
import com.project.creativedetour.ui.emoji
import com.project.creativedetour.ui.timeOf

/** Home: what Detour is thinking right now, and how today is going. */
@Composable
fun TodayScreen(
    lastCheck: CheckResult?,
    nextCheckAt: Long?,
    today: List<Feedback>,
    onCheckNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Today", style = MaterialTheme.typography.headlineMedium)
        StatusCard(lastCheck, nextCheckAt)
        StatsRow(lastCheck)
        TodaySummary(today)
        OutlinedButton(onClick = onCheckNow, modifier = Modifier.fillMaxWidth()) { Text("Check now") }
    }
}

@Composable
private fun StatusCard(check: CheckResult?, nextCheckAt: Long?) {
    val (emoji, headline, detail) = when (val d = check?.decision) {
        null -> Triple("🌱", "Getting ready", "The first check runs about a minute after Detour starts.")
        Decision.Nudge -> Triple("🚶", "Sent you a nudge", "At ${timeOf(check.at)}")
        is Decision.Quiet -> Triple("🤫", "Staying quiet", d.reason)
    }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$emoji $headline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
            val next = nextCheckAt?.let { "Next check around ${timeOf(it)}" } ?: "Background watcher not running"
            Text(next, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun StatsRow(check: CheckResult?) {
    val ctx = check?.ctx
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Steps", ctx?.stepsLastHour?.toString() ?: "—", "last hour", Modifier.weight(1f))
        StatTile("Screen", ctx?.screenMinutesLastHour?.let { "$it min" } ?: "—", "last hour", Modifier.weight(1f))
        StatTile(
            "Next event",
            ctx?.minutesUntilNextEvent?.let { "in $it min" } ?: "Free",
            ctx?.nextEventTitle ?: "calendar",
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, caption: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(caption, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TodaySummary(today: List<Feedback>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val done = today.count { it.outcome == Outcome.ACCEPTED }
            val declined = today.count { it.outcome == Outcome.REJECTED }
            Text("Today's detours", style = MaterialTheme.typography.titleMedium)
            Text(
                if (today.isEmpty()) "No nudges yet today." else "${today.size} nudges · $done done · $declined declined",
                style = MaterialTheme.typography.bodyMedium,
            )
            today.firstOrNull()?.let { last ->
                val activity = Activity.fromName(last.activity)
                Text(
                    "Latest (${timeOf(last.shownAt)}): ${activity?.emoji ?: ""} ${last.message}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
