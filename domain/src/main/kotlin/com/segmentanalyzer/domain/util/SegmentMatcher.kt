package com.segmentanalyzer.domain.util

import com.segmentanalyzer.domain.model.LatLng
import com.segmentanalyzer.domain.model.TrackPoint
import java.time.Duration
import java.time.Instant

/**
 * Consumer GPS commonly degrades to 20-30m under tree canopy (this app's own target terrain —
 * MTB/gravel riders), and a segment's own start/end coordinate is only as precise as however its
 * creator drew it on Strava. 50m is roughly two GPS samples of slack at typical riding speed and
 * sampling rate — loose enough to tolerate real-world noise, tight enough not to conflate two
 * nearby trailheads.
 */
const val SEGMENT_PROXIMITY_METERS = 50.0

/** How many extra track points past the rough end-of-segment crossing to search for the true closest one. */
private const val EXIT_SEARCH_SLACK_POINTS = 4

/** Fraction of the entry..exit sub-track that must stay near the polyline for a match to be accepted. */
private const val MIN_ON_ROUTE_FRACTION = 0.5

data class SegmentMatchResult(
    val entryIndex: Int,
    val exitIndex: Int,
    val startTime: Instant,
    val duration: Duration,
    val avgSpeedKmh: Double,
    val elevationGainMeters: Double,
    val avgPowerWatts: Double?,
)

/**
 * Finds the first pass of [track] through a segment defined by ([startLat],[startLon]) to
 * ([endLat],[endLon]), or null if the track never comes within [proximityMeters] of both in
 * order. Only the first entry/exit pair is matched — a lap or out-and-back within one ride
 * produces a single attempt, not multiple.
 *
 * If [polyline] has the segment's full route (from Strava's `map.polyline`), it's used both to
 * pin the entry/exit to the closest actual approach (tighter than just "first/last point in
 * range") and to reject a ride that only clips near both endpoints via an unrelated path. With
 * no polyline, falls back to matching against the two endpoint coordinates alone.
 */
fun matchSegment(
    track: List<TrackPoint>,
    startLat: Double,
    startLon: Double,
    endLat: Double,
    endLon: Double,
    proximityMeters: Double = SEGMENT_PROXIMITY_METERS,
    polyline: List<LatLng> = emptyList(),
): SegmentMatchResult? {
    val bounds = if (polyline.size >= 2) {
        findEntryExitViaPolyline(track, polyline, proximityMeters)
    } else {
        findEntryExitViaEndpoints(track, startLat, startLon, endLat, endLon, proximityMeters)
    } ?: return null
    val (entryIndex, exitIndex) = bounds

    val entry = track[entryIndex]
    val exit = track[exitIndex]
    val duration = Duration.between(entry.timestamp, exit.timestamp)
    val distanceMeters = exit.cumulativeDistanceMeters - entry.cumulativeDistanceMeters
    val avgSpeedKmh = if (duration.seconds > 0) (distanceMeters / 1000.0) / (duration.seconds / 3600.0) else 0.0

    var elevationGainMeters = 0.0
    var previousElevation: Float? = null
    val powerReadings = mutableListOf<Int>()
    for (index in entryIndex..exitIndex) {
        val point = track[index]
        point.elevationMeters?.let { ele ->
            previousElevation?.let { previous -> if (ele > previous) elevationGainMeters += ele - previous }
            previousElevation = ele
        }
        point.powerWatts?.let { powerReadings += it }
    }

    return SegmentMatchResult(
        entryIndex = entryIndex,
        exitIndex = exitIndex,
        startTime = entry.timestamp,
        duration = duration,
        avgSpeedKmh = avgSpeedKmh,
        elevationGainMeters = elevationGainMeters,
        avgPowerWatts = if (powerReadings.isEmpty()) null else powerReadings.average(),
    )
}

private fun findEntryExitViaEndpoints(
    track: List<TrackPoint>,
    startLat: Double,
    startLon: Double,
    endLat: Double,
    endLon: Double,
    proximityMeters: Double,
): Pair<Int, Int>? {
    val firstNearStart = track.indexOfFirst { haversineMeters(it.latitude, it.longitude, startLat, startLon) <= proximityMeters }
    if (firstNearStart == -1) return null

    val exitIndex = ((firstNearStart + 1) until track.size).firstOrNull { index ->
        haversineMeters(track[index].latitude, track[index].longitude, endLat, endLon) <= proximityMeters
    } ?: return null

    // Riders often linger near the trailhead before actually starting (prepping, regrouping) —
    // that produces a run of consecutive points within range of the start. Use the *last* of
    // those, right before they actually departed, rather than the first, or the attempt's
    // duration is inflated by however long they stood around.
    val entryIndex = (firstNearStart until exitIndex).last { index ->
        haversineMeters(track[index].latitude, track[index].longitude, startLat, startLon) <= proximityMeters
    }

    return entryIndex to exitIndex
}

private fun findEntryExitViaPolyline(
    track: List<TrackPoint>,
    polyline: List<LatLng>,
    proximityMeters: Double,
): Pair<Int, Int>? {
    val start = polyline.first()
    val end = polyline.last()

    val roughEntry = track.indexOfFirst { haversineMeters(it.latitude, it.longitude, start.latitude, start.longitude) <= proximityMeters }
    if (roughEntry == -1) return null
    val roughExit = ((roughEntry + 1) until track.size).firstOrNull { index ->
        haversineMeters(track[index].latitude, track[index].longitude, end.latitude, end.longitude) <= proximityMeters
    } ?: return null

    // Refine to the point of *closest* approach rather than first-or-last in range — this both
    // resolves lingering near the trailhead and lands on the true crossing moment even when the
    // approach/exit path curves near the endpoint from a slightly different angle each time.
    val entryIndex = (roughEntry..roughExit).minByOrNull { index ->
        haversineMeters(track[index].latitude, track[index].longitude, start.latitude, start.longitude)
    } ?: roughEntry

    // The true finish crossing can fall between two recorded samples, slightly past roughExit —
    // search a short window past it too, not just up to it.
    val exitSearchEnd = minOf(track.size - 1, roughExit + EXIT_SEARCH_SLACK_POINTS)
    val exitIndex = (entryIndex..exitSearchEnd).minByOrNull { index ->
        haversineMeters(track[index].latitude, track[index].longitude, end.latitude, end.longitude)
    } ?: roughExit
    if (exitIndex <= entryIndex) return null

    // Reject a "match" that only clips near both endpoints via an unrelated path — the ride
    // needs to actually track the segment's route in between, not just touch its ends.
    val onRouteCount = (entryIndex..exitIndex).count { index ->
        val point = track[index]
        polyline.any { p -> haversineMeters(point.latitude, point.longitude, p.latitude, p.longitude) <= proximityMeters * 2 }
    }
    val onRouteFraction = onRouteCount.toDouble() / (exitIndex - entryIndex + 1)
    if (onRouteFraction < MIN_ON_ROUTE_FRACTION) return null

    return entryIndex to exitIndex
}
