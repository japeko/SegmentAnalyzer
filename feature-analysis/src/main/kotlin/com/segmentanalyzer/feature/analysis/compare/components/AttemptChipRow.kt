package com.segmentanalyzer.feature.analysis.compare.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.feature.analysis.compare.AttemptChip
import com.segmentanalyzer.feature.analysis.compare.AttemptRole

@Composable
fun AttemptChipRow(chips: List<AttemptChip>, onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip -> AttemptChipItem(chip) }
        AddChip(onClick = onAddClick)
    }
}

@Composable
private fun AttemptChipItem(chip: AttemptChip, modifier: Modifier = Modifier) {
    val color = MaterialThemeExtras.compareColor(chip.colorIndex, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Column(modifier = Modifier.size(9.dp).clip(CircleShape).background(color)) {}
            Column {
                Text(text = chip.role.label(), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                Text(text = chip.dateLabel, fontSize = 9.5.sp, color = MaterialThemeExtras.textTertiary)
            }
        }
    }
}

@Composable
private fun AddChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text = "Add", fontWeight = FontWeight.Medium, fontSize = 11.5.sp)
        }
    }
}

private fun AttemptRole.label(): String = when (this) {
    AttemptRole.CURRENT -> "Current"
    AttemptRole.PERSONAL_BEST -> "Personal Best"
    AttemptRole.PREVIOUS -> "Previous"
    AttemptRole.SELECTED -> "Selected"
}
