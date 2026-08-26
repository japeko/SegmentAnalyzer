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
import com.segmentanalyzer.feature.analysis.compare.RideCompareRoute
import com.segmentanalyzer.feature.auth.garmin.GarminLoginRoute
import com.segmentanalyzer.feature.auth.strava.StravaCallbackRoute
import com.segmentanalyzer.feature.history.detail.RideDetailRoute
import com.segmentanalyzer.feature.history.history.RideHistoryRoute
import com.segmentanalyzer.feature.history.records.RecordsRoute
import com.segmentanalyzer.feature.importer.garmin.GarminImportRoute
import com.segmentanalyzer.feature.segments.SegmentsRoute
import com.segmentanalyzer.feature.segments.detail.SegmentDetailRoute
import com.segmentanalyzer.feature.settings.AboutScreen
import com.segmentanalyzer.feature.settings.HowToUseScreen
import com.segmentanalyzer.feature.settings.SettingsRoute

private const val GARMIN_LOGIN_ROUTE = "garmin_login"
private const val ABOUT_ROUTE = "about"
private const val HOW_TO_USE_ROUTE = "how_to_use"
private const val GARMIN_IMPORT_ROUTE = "garmin_import"
private const val SEGMENT_DETAIL_ROUTE = "segment_detail"
private const val RIDE_COMPARE_ROUTE = "ride_compare"
private const val RIDE_DETAIL_ROUTE = "ride_detail"

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

    // Navigating to a top-level destination from a non-top-level screen (e.g. a "Go to Settings"
    // prompt on Ride/Segment Detail) must use the same saveState/restoreState options as the
    // bottom nav's own tab switches. A bare navigate() here was confirmed live to leave the
    // NavController's saved-state registry inconsistent with what the bottom nav expects,
    // silently breaking the Rides tab after connecting Strava — same root cause as the
    // STRAVA_CALLBACK_ROUTE gotcha documented below.
    val goToSettings: () -> Unit = {
        navController.navigate(TopLevelDestination.Settings.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Rides.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.Rides.route) {
            RideHistoryRoute(
                onRideClick = { rideId -> navController.navigate("$RIDE_DETAIL_ROUTE/$rideId") },
                onSearchClick = { /* Search isn't implemented yet. */ },
                // No FIT/GPX import on this branch — Garmin is the only import source, so skip
                // straight past the (now unreachable) source picker.
                onImportClick = { navController.navigate(GARMIN_IMPORT_ROUTE) },
                onNewPBsClick =  { navController.navigate(TopLevelDestination.Records.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                } },
            )
        }
        composable(TopLevelDestination.Segments.route) {
            SegmentsRoute(
                onGoToSettingsClick = goToSettings,
                onSegmentClick = { segmentId -> navController.navigate("$SEGMENT_DETAIL_ROUTE/$segmentId") },
            )
        }
        composable(TopLevelDestination.Records.route) {
            RecordsRoute(
                onSegmentClick = { segmentId -> navController.navigate("$SEGMENT_DETAIL_ROUTE/$segmentId") },
            )
        }
        composable(TopLevelDestination.Settings.route) {
            SettingsRoute(
                onConnectGarminClick = { navController.navigate(GARMIN_LOGIN_ROUTE) },
                onConnectStravaClick = { authorizationUrl ->
                    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(authorizationUrl))
                },
                onHowToUseClick = { navController.navigate(HOW_TO_USE_ROUTE) },
                onAboutClick = { navController.navigate(ABOUT_ROUTE) },
            )
        }
        composable(ABOUT_ROUTE) {
            AboutScreen(onBackClick = { navController.popBackStack() })
        }
        composable(HOW_TO_USE_ROUTE) {
            HowToUseScreen(onBackClick = { navController.popBackStack() })
        }
        composable(GARMIN_LOGIN_ROUTE) {
            GarminLoginRoute(
                onConnected = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(GARMIN_IMPORT_ROUTE) {
            GarminImportRoute(
                onGoToSettingsClick = goToSettings,
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(
            route = "$SEGMENT_DETAIL_ROUTE/{segmentId}",
            arguments = listOf(navArgument("segmentId") { type = NavType.LongType }),
        ) {
            SegmentDetailRoute(
                onBackClick = { navController.popBackStack() },
                onAttemptClick = { segmentId, attemptId ->
                    navController.navigate("$RIDE_COMPARE_ROUTE/$segmentId?anchorAttemptId=$attemptId")
                },
                onGoToSettingsClick = goToSettings,
            )
        }
        composable(
            route = "$RIDE_DETAIL_ROUTE/{rideId}",
            arguments = listOf(navArgument("rideId") { type = NavType.LongType }),
        ) {
            RideDetailRoute(
                onBackClick = { navController.popBackStack() },
                onGoToSettingsClick = goToSettings,
            )
        }
        composable(
            route = "$RIDE_COMPARE_ROUTE/{segmentId}?anchorAttemptId={anchorAttemptId}",
            arguments = listOf(
                navArgument("segmentId") { type = NavType.LongType },
                navArgument("anchorAttemptId") { type = NavType.LongType },
            ),
        ) {
            RideCompareRoute(onBackClick = { navController.popBackStack() })
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
                // This destination (a one-off deep-link landing page, never meant to be
                // revisited) must never become part of the saved/restored back-stack state the
                // bottom nav's tab switches rely on — pop it off explicitly first (default
                // saveState = false) so it isn't lying around when the popUpTo below saves state
                // for everything else. Skipping this step, or replacing the two calls below with
                // a single popUpTo(startDestinationId){saveState=true} that also happens to sweep
                // this destination up, was confirmed live to leave it saved under the Rides tab's
                // restoration bucket: the *next* bottom-nav tap on Rides silently restored this
                // screen instead of switching tabs, with no crash and no visible cause.
                navController.popBackStack(route = STRAVA_CALLBACK_ROUTE, inclusive = true)
                // Must match the bottom-nav tab switch's own navigate options exactly (see
                // SegmentAnalyzerApp) — this is also a jump to a top-level destination, just
                // triggered by the Strava OAuth deep link instead of a tab tap. Without
                // saveState/restoreState here, this leaves the NavController's saved-state
                // registry inconsistent with what the bottom nav itself expects: confirmed live,
                // this made the Rides tab silently stop responding to taps after connecting both
                // Garmin and Strava in the same session, until the app was force-restarted.
                navController.navigate(TopLevelDestination.Settings.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            StravaCallbackRoute(onConnected = backToSettings, onBackToSettingsClick = backToSettings)
        }
    }
}
