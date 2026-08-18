package com.segmentanalyzer.app

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.segmentanalyzer.app.navigation.SegmentAnalyzerNavHost
import com.segmentanalyzer.app.navigation.TopLevelDestination

/**
 * [NavigationSuiteScaffold] switches between a bottom nav bar (compact width — phone portrait)
 * and a nav rail (medium/expanded width — tablet, landscape) from this one destination list,
 * satisfying CLAUDE.md's landscape/tablet support without separate layout branches.
 */
@Composable
fun SegmentAnalyzerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = currentRoute == destination.route,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(imageVector = destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                )
            }
        },
    ) {
        SegmentAnalyzerNavHost(navController = navController)
    }
}
