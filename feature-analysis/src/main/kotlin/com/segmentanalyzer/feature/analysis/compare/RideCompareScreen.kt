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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.segmentanalyzer.core.theme.MaterialThemeExtras
import com.segmentanalyzer.core.ui.RoutePreviewCard
import com.segmentanalyzer.feature.analysis.compare.components.AttemptChipRow
import com.segmentanalyzer.feature.analysis.compare.components.CompareStatsCard
import com.segmentanalyzer.feature.analysis.compare.components.DistanceAxisRow
import com.segmentanalyzer.feature.analysis.compare.components.SlopeChart
import com.segmentanalyzer.feature.analysis.compare.components.SpeedChart
import com.segmentanalyzer.feature.analysis.compare.components.TimeGapChart
import com.segmentanalyzer.feature.analysis.compare.picker.ComparePickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideCompareScreen(
    uiState: RideCompareUiState,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onRemoveClick: (Long) -> Unit,
    onDismissAddSheet: () -> Unit,
    onAddableAttemptSelected: (Long) -> Unit,
    onConfirmAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.segmentName.isNotBlank()) "Compare Rides: ${uiState.segmentName}" else "Compare Rides",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        var scrubFraction by remember { mutableStateOf<Float?>(null) }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                AttemptChipRow(
                    chips = uiState.chips,
                    onAddClick = onAddClick,
                    onRemoveClick = onRemoveClick,
                    modifier = Modifier.padding(start = 16.dp, top = 6.dp),
                )
            }

            if (uiState.routePoints.size >= 2) {
                item {
                    RoutePreviewCard(
                        routePoints = uiState.routePoints,
                        gradientPercents = uiState.gradientPercents,
                        highlightFraction = scrubFraction,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
            }

            val hasDistanceCharts = uiState.slopePoints.isNotEmpty() || uiState.speedSeries.isNotEmpty() || uiState.timeGapSeries.isNotEmpty()
            if (hasDistanceCharts) {
                item {
                    DistanceAxisRow(
                        segmentDistanceMeters = uiState.segmentDistanceMeters,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                }
            }

            if (uiState.slopePoints.isNotEmpty()) {
                item { ChartSectionHeader("Slope") }
                item {
                    SlopeChart(
                        points = uiState.slopePoints,
                        selectedFraction = scrubFraction,
                        onFractionSelected = { scrubFraction = it },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (uiState.speedSeries.isNotEmpty()) {
                item { ChartSectionHeader("Speed") }
                item {
                    SpeedChart(
                        series = uiState.speedSeries,
                        selectedFraction = scrubFraction,
                        onFractionSelected = { scrubFraction = it },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (uiState.timeGapSeries.isNotEmpty()) {
                item { ChartSectionHeader("Time Gap vs Current Ride") }
                item {
                    val currentColorIndex = uiState.chips.find { it.role == AttemptRole.CURRENT }?.colorIndex ?: 0
                    TimeGapChart(
                        series = uiState.timeGapSeries,
                        currentColorIndex = currentColorIndex,
                        selectedFraction = scrubFraction,
                        onFractionSelected = { scrubFraction = it },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (uiState.statRows.isNotEmpty()) {
                item { ChartSectionHeader("Stats", topPadding = 16.dp) }
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

/** Compact label above a stacked chart/card — kept small so several charts fit on one phone screen. */
@Composable
private fun ChartSectionHeader(title: String, topPadding: Dp = 12.dp) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialThemeExtras.textTertiary,
        modifier = Modifier.padding(start = 16.dp, top = topPadding, bottom = 4.dp),
    )
}
