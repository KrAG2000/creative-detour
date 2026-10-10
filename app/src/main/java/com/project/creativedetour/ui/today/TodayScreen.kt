package com.project.creativedetour.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.creativedetour.data.Feedback
import com.project.creativedetour.data.Outcome
import com.project.creativedetour.engine.CheckResult
import com.project.creativedetour.engine.RejectReason
import java.text.DateFormat
import java.util.Date

@Composable
fun TodayScreen(
    history: List<Feedback>,
    lastCheck: CheckResult?,
    onCheckNow: () -> Unit,
    onSendTestNudge: () -> Unit,
    onOpenSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Creative Detour", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Accepted ${history.count { it.outcome == Outcome.ACCEPTED }} of ${history.size} recent nudges",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        RightNowCard(lastCheck)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCheckNow, modifier = Modifier.weight(1f)) { Text("Check now") }
            OutlinedButton(onClick = onSendTestNudge, modifier = Modifier.weight(1f)) { Text("Force nudge") }
        }
        TextButton(onClick = onOpenSetup) { Text("Permissions & setup") }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history, key = { it.id }) { FeedbackRow(it) }
        }
    }
}

/** What the engine saw on its last check, and what it decided. "—" = source unavailable. */
@Composable
private fun RightNowCard(check: CheckResult?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            if (check == null) {
                Text("No check yet. The first one runs a minute after start.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            val ctx = check.ctx
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(check.at))
            fun Int?.show(unit: String) = this?.let { "$it $unit" } ?: "—"
            Text("Last check $time · ${if (check.nudged) "nudged 🚶" else "stayed quiet 🤫"}", style = MaterialTheme.typography.labelMedium)
            Text("Steps (last hour): ${ctx.stepsLastHour.show("")}")
            Text("Screen on (last hour): ${ctx.screenMinutesLastHour.show("min")}")
            Text("Since last phone use: ${ctx.minutesSinceLastUsage.show("min")}")
            Text("Next event: ${ctx.minutesUntilNextEvent?.let { "${ctx.nextEventTitle ?: "untitled"} in $it min" } ?: "—"}")
            Text("Since last nudge: ${ctx.minutesSinceLastNudge.show("min")}")
        }
    }
}

@Composable
private fun FeedbackRow(feedback: Feedback) {
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(feedback.shownAt))
    val outcome = when (feedback.outcome) {
        Outcome.PENDING -> "⏳ waiting"
        Outcome.ACCEPTED -> "✅ accepted"
        Outcome.REJECTED -> "❌ ${feedback.reason?.label ?: "no reason"}"
        Outcome.DISMISSED -> "👋 swiped away"
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("$time · $outcome", style = MaterialTheme.typography.labelMedium)
            Text(feedback.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Opened from the "More…" notification button: every reason, one tap each. */
@Composable
fun ReasonPickerDialog(onPick: (RejectReason) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What's stopping you?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RejectReason.entries.forEach { reason ->
                    OutlinedButton(onClick = { onPick(reason) }, modifier = Modifier.fillMaxWidth()) {
                        Text(reason.label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Skip") } },
    )
}
