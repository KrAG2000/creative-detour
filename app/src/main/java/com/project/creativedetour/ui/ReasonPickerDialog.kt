package com.project.creativedetour.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.creativedetour.engine.RejectReason

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
