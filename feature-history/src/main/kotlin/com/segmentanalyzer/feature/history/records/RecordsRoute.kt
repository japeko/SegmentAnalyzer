package com.segmentanalyzer.feature.history.records

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.segmentanalyzer.core.ui.ComingSoonScreen

/**
 * Personal records live conceptually under History per CLAUDE.md, but aren't implemented yet —
 * this is a real bottom-nav destination with a placeholder screen.
 */
@Composable
fun RecordsRoute(modifier: Modifier = Modifier) {
    ComingSoonScreen(title = "Records", modifier = modifier
    )
}
