package com.segmentanalyzer.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.segmentanalyzer.feature.auth.garmin.GarminLoginRoute
import com.segmentanalyzer.feature.history.history.RideHistoryRoute
import com.segmentanalyzer.feature.history.records.RecordsRoute
import com.segmentanalyzer.feature.segments.SegmentsRoute
import com.segmentanalyzer.feature.settings.SettingsRoute

private const val GARMIN_LOGIN_ROUTE = "garmin_login"

@Composable
fun SegmentAnalyzerNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Rides.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.Rides.route) {
            RideHistoryRoute(
                onRideClick = { /* Ride Analysis screen isn't implemented yet. */ },
                onSearchClick = { /* Search isn't implemented yet. */ },
                onImportClick = { /* feature-import isn't implemented yet. */ },
            )
        }
        composable(TopLevelDestination.Segments.route) { SegmentsRoute() }
        composable(TopLevelDestination.Records.route) { RecordsRoute() }
        composable(TopLevelDestination.Settings.route) {
            SettingsRoute(onConnectGarminClick = { navController.navigate(GARMIN_LOGIN_ROUTE) })
        }
        composable(GARMIN_LOGIN_ROUTE) {
            GarminLoginRoute(
                onConnected = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
