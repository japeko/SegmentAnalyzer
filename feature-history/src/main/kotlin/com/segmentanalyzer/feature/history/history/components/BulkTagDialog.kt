package com.segmentanalyzer.feature.history.history.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.feature.history.history.BulkTagDialogState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkTagDialog(
    dialog: BulkTagDialogState,
    onDismiss: () -> Unit,
    onTagChange: (String) -> Unit,
    onTagSuggestionClick: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Tag") },
        text = {
            Column {
                Text(
                    text = "Applies to ${dialog.selectedCount} selected ride(s). Leave blank to clear their tag.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = dialog.tag,
                    onValueChange = onTagChange,
                    label = { Text("Tag") },
                    placeholder = { Text("e.g. Race, Training") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                if (dialog.tagSuggestions.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        dialog.tagSuggestions.forEach { suggestion ->
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTagSuggestionClick(suggestion) }
                                    .padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSaveClick) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
