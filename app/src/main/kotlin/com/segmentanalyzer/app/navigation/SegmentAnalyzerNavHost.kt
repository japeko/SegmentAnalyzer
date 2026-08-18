package com.segmentanalyzer.app.navigation

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.segmentanalyzer.feature.auth.garmin.GarminLoginRoute
import com.segmentanalyzer.feature.auth.strava.StravaCallbackRoute
import com.segmentanalyzer.feature.history.history.RideHistoryRoute
import com.segmentanalyzer.feature.history.records.RecordsRoute
import com.segmentanalyzer.feature.importer.ImportSourceRoute
import com.segmentanalyzer.feature.importer.fit.FitFileImportRoute
import com.segmentanalyzer.feature.importer.garmin.GarminImportRoute
import com.segmentanalyzer.feature.importer.gpx.GpxFileImportRoute
import com.segmentanalyzer.feature.segments.SegmentsRoute
import com.segmentanalyzer.feature.settings.SettingsRoute

private const val GARMIN_LOGIN_ROUTE = "garmin_login"
private const val GARMIN_IMPORT_ROUTE = "garmin_import"
private const val IMPORT_SOURCE_ROUTE = "import_source"
private const val FIT_FILE_IMPORT_ROUTE = "fit_file_import"
private const val GPX_FILE_IMPORT_ROUTE = "gpx_file_import"

/** Must match the intent-filter path in AndroidManifest.xml (minus the code/error query args). */
const val STRAVA_CALLBACK_ROUTE_BASE = "strava_callback"
private const val STRAVA_CALLBACK_ROUTE = "$STRAVA_CALLBACK_ROUTE_BASE?code={code}&error={error}"

/** Builds the in-app nav route for a Strava OAuth redirect's code/error, for [SegmentAnalyzerApp] to navigate to. */
fun stravaCallbackRoute(code: String?, error: String?): String {
    val builder = Uri.Builder().path(STRAVA_CALLBACK_ROUTE_BASE)
    code?.let { builder.appendQueryParameter("code", it) }
    error?.let { builder.appendQueryParameter("error", it) }
    return builder.build().toString()
}

@Composable
fun SegmentAnalyzerNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Rides.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.Rides.route) {
            RideHistoryRoute(
                onRideClick = { /* Ride Analysis screen isn't implemented yet. */ },
                onSearchClick = { /* Search isn't implemented yet. */ },
                onImportClick = { navController.navigate(IMPORT_SOURCE_ROUTE) },
            )
        }
        composable(TopLevelDestination.Segments.route) {
            SegmentsRoute(onGoToSettingsClick = { navController.navigate(TopLevelDestination.Settings.route) })
        }
        composable(TopLevelDestination.Records.route) { RecordsRoute() }
        composable(TopLevelDestination.Settings.route) {
            SettingsRoute(
                onConnectGarminClick = { navController.navigate(GARMIN_LOGIN_ROUTE) },
                onConnectStravaClick = { authorizationUrl ->
                    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(authorizationUrl))
                },
            )
        }
        composable(GARMIN_LOGIN_ROUTE) {
            GarminLoginRoute(
                onConnected = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(GARMIN_IMPORT_ROUTE) {
            GarminImportRoute(
                onGoToSettingsClick = { navController.navigate(TopLevelDestination.Settings.route) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(IMPORT_SOURCE_ROUTE) {
            ImportSourceRoute(
                onGarminClick = { navController.navigate(GARMIN_IMPORT_ROUTE) },
                onFitFileClick = { navController.navigate(FIT_FILE_IMPORT_ROUTE) },
                onGpxFileClick = { navController.navigate(GPX_FILE_IMPORT_ROUTE) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(FIT_FILE_IMPORT_ROUTE) {
            FitFileImportRoute(onBackClick = { navController.popBackStack() })
        }
        composable(GPX_FILE_IMPORT_ROUTE) {
            GpxFileImportRoute(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = STRAVA_CALLBACK_ROUTE,
            arguments = listOf(
                navArgument("code") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("error") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            val backToSettings = {
                navController.navigate(TopLevelDestination.Settings.route) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }
            }
            StravaCallbackRoute(onConnected = backToSettings, onBackToSettingsClick = backToSettings)
        }
    }
}
