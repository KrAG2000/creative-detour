package com.project.creativedetour.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.creativedetour.data.Feedback
import com.project.creativedetour.data.Outcome
import com.project.creativedetour.engine.Activity
import com.project.creativedetour.engine.RejectReason
import com.project.creativedetour.ui.dayLabel
import com.project.creativedetour.ui.emoji
import com.project.creativedetour.ui.timeOf

/** The full log, grouped by day, with what Detour has learned from it. */
@Composable
fun ActivityScreen(history: List<Feedback>, modifier: Modifier = Modifier) {
    val byDay = history.groupBy { dayLabel(it.shownAt) } // history is newest first, so days come out in order
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("Activity", style = MaterialTheme.typography.headlineMedium) }
        item { InsightsCard(history) }
        if (history.isEmpty()) {
            item { Text("Nothing here yet. Your nudges and answers will show up here.", style = MaterialTheme.typography.bodyMedium) }
        }
        byDay.forEach { (day, rows) ->
            item(key = "day-$day") {
                Text(day, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            }
            items(rows, key = { it.id }) { FeedbackRow(it) }
        }
    }
}

/** What Detour learned, in plain words. Mirrors the rules in ActivityPicker and NudgeEngine. */
@Composable
private fun InsightsCard(history: List<Feedback>) {
    val answered = history.filter { it.outcome != Outcome.PENDING }
    val done = answered.count { it.outcome == Outcome.ACCEPTED }
    val favourites = history.filter { it.outcome == Outcome.ACCEPTED }
        .groupingBy { it.activity }.eachCount()
        .entries.sortedByDescending { it.value }.take(2)
        .mapNotNull { (name, count) -> Activity.fromName(name)?.let { "${it.emoji} ${it.label} ($count×)" } }
    val busyHours = history.filter { it.reason == RejectReason.BUSY }
        .groupingBy { it.hourOfDay }.eachCount()
        .filterValues { it >= 2 }.keys.sorted()
    val topReason = history.mapNotNull { it.reason }.groupingBy { it }.eachCount().maxByOrNull { it.value }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("What Detour has learned", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (answered.isEmpty()) {
                Text("Answer a few nudges and patterns will show up here.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            Text("You did $done of ${answered.size} detours (${done * 100 / answered.size}%).", style = MaterialTheme.typography.bodyMedium)
            if (favourites.isNotEmpty()) {
                Text("Suggests more of: ${favourites.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
            }
            if (busyHours.isNotEmpty()) {
                Text("Skips ${busyHours.joinToString(", ") { "$it:00" }}: you're usually busy then.", style = MaterialTheme.typography.bodyMedium)
            }
            topReason?.let { Text("Most common “not now”: ${it.key.label} (${it.value}×)", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun FeedbackRow(feedback: Feedback) {
    val activity = Activity.fromName(feedback.activity)
    val outcome = when (feedback.outcome) {
        Outcome.PENDING -> "⏳ Waiting"
        Outcome.ACCEPTED -> "✅ Done"
        Outcome.REJECTED -> "❌ ${feedback.reason?.label ?: "Not now"}"
        Outcome.DISMISSED -> "👋 Swiped away"
    }
    val author = if (feedback.writtenBy == "template") "📝 template" else "🧠 ${feedback.writtenBy}"
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${timeOf(feedback.shownAt)} · ${activity?.let { "${it.emoji} ${it.label}" } ?: "nudge"}",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(feedback.message, style = MaterialTheme.typography.bodyMedium)
            Text("$outcome · $author", style = MaterialTheme.typography.labelSmall)
        }
    }
}
