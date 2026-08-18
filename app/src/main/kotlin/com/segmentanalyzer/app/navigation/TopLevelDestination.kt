package com.segmentanalyzer.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(val route: String, val label: String, val icon: ImageVector) {
    Rides(route = "rides", label = "Rides", icon = Icons.AutoMirrored.Filled.List),
    Segments(route = "segments", label = "Segments", icon = Icons.Filled.Flag),
    Records(route = "records", label = "Records", icon = Icons.Filled.EmojiEvents),
    Settings(route = "settings", label = "Settings", icon = Icons.Filled.Settings),
}
