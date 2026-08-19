package com.segmentanalyzer.domain.util

import com.segmentanalyzer.domain.model.LatLng
import com.segmentanalyzer.domain.model.Segment

/** The segment's route: Strava's decoded polyline if available, else just the two endpoints. */
fun Segment.routePoints(): List<LatLng> {
    val decoded = polyline?.let { decodePolyline(it) }.orEmpty()
    if (decoded.size >= 2) return decoded

    val startLat = startLatitude
    val startLon = startLongitude
    val endLat = endLatitude
    val endLon = endLongitude
    return if (startLat != null && startLon != null && endLat != null && endLon != null) {
        listOf(LatLng(startLat, startLon), LatLng(endLat, endLon))
    } else {
        emptyList()
    }
}
