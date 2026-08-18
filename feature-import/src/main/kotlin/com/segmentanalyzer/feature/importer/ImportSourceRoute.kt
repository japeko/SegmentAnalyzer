package com.segmentanalyzer.feature.importer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ImportSourceRoute(
    onGarminClick: () -> Unit,
    onFitFileClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ImportSourceScreen(
        onGarminClick = onGarminClick,
        onFitFileClick = onFitFileClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
