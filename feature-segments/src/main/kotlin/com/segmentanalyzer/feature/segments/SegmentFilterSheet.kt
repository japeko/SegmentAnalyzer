package com.segmentanalyzer.feature.segments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DATE_LABEL_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentFilterSheet(
    availableTags: List<String>,
    selectedTag: String?,
    dateFrom: LocalDate?,
    dateTo: LocalDate?,
    onTagSelected: (String?) -> Unit,
    onDateFromSelected: (LocalDate?) -> Unit,
    onDateToSelected: (LocalDate?) -> Unit,
    onClearFiltersClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = "Filter Segments", fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Text(
            text = "Tag",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialThemeExtras.textTertiary,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 4.dp)) {
            item {
                FilterChip(
                    selected = selectedTag == null,
                    onClick = { onTagSelected(null) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
            items(availableTags) { tag ->
                FilterChip(
                    selected = selectedTag == tag,
                    onClick = { onTagSelected(tag) },
                    label = { Text(tag) },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }

        Text(
            text = "Ride Date",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialThemeExtras.textTertiary,
            modifier = Modifier.padding(top = 22.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateField(label = "From", date = dateFrom, onDateSelected = onDateFromSelected, modifier = Modifier.weight(1f))
            DateField(label = "To", date = dateTo, onDateSelected = onDateToSelected, modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onClearFiltersClick, modifier = Modifier.weight(1f)) { Text("Clear All") }
            Button(onClick = onDoneClick, modifier = Modifier.weight(1f)) { Text("Done") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, date: LocalDate?, onDateSelected: (LocalDate?) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { showPicker = true }, modifier = modifier) {
        Text(text = "$label: ${date?.let { DATE_LABEL_FORMATTER.format(it) } ?: "Any"}", maxLines = 1)
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    onDateSelected(millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate() })
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
