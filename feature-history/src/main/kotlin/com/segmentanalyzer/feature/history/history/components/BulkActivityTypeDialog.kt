package com.segmentanalyzer.feature.history.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.ui.label
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.feature.history.history.BulkActivityTypeDialogState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkActivityTypeDialog(
    dialog: BulkActivityTypeDialogState,
    onTypeSelected: (ActivityType) -> Unit,
    onDismiss: () -> Unit,
    onSaveClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Ride Type") },
        text = {
            Column {
                Text(
                    text = "Applies to ${dialog.selectedCount} selected ride(s).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    items(ActivityType.entries) { type ->
                        FilterChip(
                            selected = dialog.selectedType == type,
                            onClick = { onTypeSelected(type) },
                            label = { Text(type.label()) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSaveClick, enabled = dialog.selectedType != null) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
