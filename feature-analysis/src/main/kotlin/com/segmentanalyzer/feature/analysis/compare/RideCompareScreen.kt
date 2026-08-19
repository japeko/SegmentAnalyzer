package com.segmentanalyzer.feature.analysis.compare

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.core.ui.RoutePreviewCard
import com.segmentanalyzer.feature.analysis.compare.components.AttemptChipRow
import com.segmentanalyzer.feature.analysis.compare.components.CompareStatsCard
import com.segmentanalyzer.feature.analysis.compare.components.TimeGapChart
import com.segmentanalyzer.feature.analysis.compare.picker.ComparePickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideCompareScreen(
    uiState: RideCompareUiState,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onDismissAddSheet: () -> Unit,
    onAddableAttemptSelected: (Long) -> Unit,
    onConfirmAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Compare Rides") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                AttemptChipRow(
                    chips = uiState.chips,
                    onAddClick = onAddClick,
                    modifier = Modifier.padding(start = 16.dp, top = 6.dp),
                )
            }

            if (uiState.routePoints.size >= 2) {
                item {
                    RoutePreviewCard(
                        routePoints = uiState.routePoints,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
            }

            if (uiState.timeGapSeries.isNotEmpty()) {
                item {
                    Text(
                        text = "Time Gap vs Current Ride",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialThemeExtras.textTertiary,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp),
                    )
                }
                item { TimeGapChart(series = uiState.timeGapSeries, modifier = Modifier.padding(horizontal = 16.dp)) }
            }

            if (uiState.statRows.isNotEmpty()) {
                item {
                    Text(
                        text = "Stats",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialThemeExtras.textTertiary,
                        modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp),
                    )
                }
                item {
                    CompareStatsCard(
                        rows = uiState.statRows,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
            }
        }

        if (uiState.isAddSheetVisible) {
            ModalBottomSheet(onDismissRequest = onDismissAddSheet, sheetState = rememberModalBottomSheetState()) {
                ComparePickerSheet(
                    segmentName = uiState.segmentName,
                    attempts = uiState.addableAttempts,
                    selectedId = uiState.selectedAddableId,
                    onSelect = onAddableAttemptSelected,
                    onConfirm = onConfirmAdd,
                )
            }
        }
    }
}
