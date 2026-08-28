package com.segmentanalyzer.feature.segments.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.feature.segments.detail.GuestImportSheetState

@Composable
fun GuestImportSheet(
    state: GuestImportSheetState,
    onChooseFileClick: () -> Unit,
    onRiderNameChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = "Import a Guest Ride", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            text = "A friend's FIT file — matched against your segments, shown separately, never counted toward your own records.",
            fontSize = 12.5.sp,
            color = MaterialThemeExtras.textTertiary,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
        )

        OutlinedButton(onClick = onChooseFileClick, modifier = Modifier.fillMaxWidth(), enabled = !state.isImporting) {
            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = "Choose FIT File", modifier = Modifier.padding(start = 8.dp))
        }
        state.pickedFileName?.let { fileName ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialThemeExtras.faster,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = fileName,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        OutlinedTextField(
            value = state.riderName,
            onValueChange = onRiderNameChange,
            label = { Text("Rider's name") },
            singleLine = true,
            enabled = !state.isImporting,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        state.errorMessage?.let { message ->
            Text(
                text = message,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Button(
            onClick = onConfirmClick,
            enabled = !state.isImporting && state.pickedFileUri != null,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            if (state.isImporting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Import")
            }
        }
    }
}
