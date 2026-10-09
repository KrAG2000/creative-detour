package com.project.creativedetour.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.project.creativedetour.engine.RejectReason
import java.text.DateFormat
import java.util.Date

@Composable
fun TodayScreen(
    history: List<Feedback>,
    onSendTestNudge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Creative Detour", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Accepted ${history.count { it.outcome == Outcome.ACCEPTED }} of ${history.size} recent nudges",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSendTestNudge, modifier = Modifier.fillMaxWidth()) {
            Text("Send test nudge")
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history, key = { it.id }) { FeedbackRow(it) }
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
