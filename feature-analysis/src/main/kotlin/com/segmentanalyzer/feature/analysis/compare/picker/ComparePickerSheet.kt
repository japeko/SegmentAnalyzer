package com.segmentanalyzer.feature.analysis.compare.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.core.theme.NumericFontFamily
import com.segmentanalyzer.feature.analysis.compare.AddableAttemptItem

@Composable
fun ComparePickerSheet(
    segmentName: String,
    attempts: List<AddableAttemptItem>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = "Add a Ride to Compare", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            text = "$segmentName · other attempts on this route",
            fontSize = 12.5.sp,
            color = MaterialThemeExtras.textTertiary,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
        )

        // Non-functional placeholder — search isn't implemented in this version.
        OutlinedTextField(
            value = "",
            onValueChange = {},
            enabled = false,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Search rides on this route") },
            modifier = Modifier.fillMaxWidth(),
        )

        val (unavailable, selectable) = attempts.partition { it.statusLabel != null }

        Column(modifier = Modifier.padding(top = 12.dp)) {
            unavailable.forEach { item -> UnavailableRow(item) }
            if (unavailable.isNotEmpty() && selectable.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            }
            selectable.forEach { item ->
                SelectableRow(item = item, isSelected = item.id == selectedId, onSelect = { onSelect(item.id) })
            }
        }

        Button(
            onClick = onConfirm,
            enabled = selectedId != null,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text("Add Selected Ride")
        }
    }
}

@Composable
private fun UnavailableRow(item: AddableAttemptItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = item.dateLabel, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
            Text(text = item.statsLabel, fontFamily = NumericFontFamily, fontSize = 11.5.sp, color = MaterialThemeExtras.textTertiary)
        }
        Text(
            text = item.statusLabel.orEmpty(),
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SelectableRow(item: AddableAttemptItem, isSelected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val background = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .selectable(selected = isSelected, onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(text = "${item.dateLabel} · ${item.lapLabel}", fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
            Text(text = item.statsLabel, fontFamily = NumericFontFamily, fontSize = 11.5.sp, color = MaterialThemeExtras.textTertiary)
        }
    }
}
