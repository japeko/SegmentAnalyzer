package com.segmentanalyzer.feature.analysis.compare.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.feature.analysis.compare.AiInsightState

/**
 * On-device (Gemini Nano) explanation of the comparison. The caller only renders this at all when
 * [com.segmentanalyzer.feature.analysis.compare.RideCompareUiState.isAiInsightAvailable] is true —
 * there's deliberately no "unsupported" state here, since an unsupported phone never sees it.
 */
@Composable
fun AiInsightSection(state: AiInsightState, onGenerateClick: () -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        AiInsightState.Idle -> OutlinedButton(onClick = onGenerateClick, modifier = modifier) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = "AI Insight", modifier = Modifier.padding(start = 6.dp))
        }

        AiInsightState.Loading -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(text = "Generating insight…", style = MaterialTheme.typography.bodyMedium, color = MaterialThemeExtras.textTertiary)
        }

        is AiInsightState.Loaded -> Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(text = "AI Insight", style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    text = state.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        is AiInsightState.Error -> Column(modifier = modifier) {
            Text(text = state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Button(onClick = onGenerateClick, modifier = Modifier.padding(top = 8.dp)) {
                Text("Retry")
            }
        }
    }
}
