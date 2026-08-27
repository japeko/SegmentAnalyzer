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

/**
 * Well under anyone's realistic riding (or walking-the-bike) pace through even a technical
 * segment — used only to bound how long a single match may plausibly span. Without this, a rider
 * passing near a segment's end coordinate again much later in a long ride (a different pass by a
 * shared trailhead, lift queue, or access road) gets stitched to an earlier, unrelated entry into
 * one wildly-long "attempt" — confirmed live: a 1.1km segment (2:32 PB) matched a 1:02:58 "attempt"
 * because the true exit crossing was never within range, and the search kept going until the
 * track happened to pass the end coordinate again roughly an hour later.
 */
private const val MIN_PLAUSIBLE_SPEED_METERS_PER_SECOND = 1.0

/** Floor under [MIN_PLAUSIBLE_SPEED_METERS_PER_SECOND]'s bound, so a very short segment still allows a brief stop. */
private const val MIN_PLAUSIBLE_DURATION_SECONDS = 120L

private fun maxPlausibleDuration(segmentDistanceMeters: Double): Duration =
    Duration.ofSeconds(
        (segmentDistanceMeters / MIN_PLAUSIBLE_SPEED_METERS_PER_SECOND).toLong().coerceAtLeast(MIN_PLAUSIBLE_DURATION_SECONDS),
    )

/**
 * Bounds how much real GPS ground a single pass may plausibly cover, relative to the segment's
 * own mapped length — used the same way as [maxPlausibleDuration], but for the shuttle-less
 * riding pattern the time bound alone doesn't catch: descend a trail, climb back up the *same*
 * trail to lap it, then descend again. That retrace still hugs the segment's polyline in reverse
 * (passing the on-route-fraction check) and still finishes within a plausible time, but the exit
 * search's first proximity hit near the end coordinate can land on the *second* descent's finish
 * rather than the first — confirmed live: a 535m segment (best real laps 520-855m of ground
 * covered, ~1.6x) matched two ~1050m/~1470m "attempts" (~2x/~2.7x) crossing 6+ minutes, once for
 * an out-and-back-then-down pattern. 1.75x leaves headroom for a technical, indirect line while
 * still rejecting a full down-up-down cycle.
 */
private const val MAX_PLAUSIBLE_DISTANCE_RATIO = 1.75

private fun maxPlausibleDistanceMeters(segmentDistanceMeters: Double): Double =
    segmentDistanceMeters * MAX_PLAUSIBLE_DISTANCE_RATIO

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
 * Finds every pass of [track] through a segment defined by ([startLat],[startLon]) to
 * ([endLat],[endLon]) — a rider lapping the same descent repeatedly within one ride produces one
 * result per lap, not just the first. Each pass is searched for starting right after the previous
 * one's exit, so laps never overlap.
 *
 * If [polyline] has the segment's full route (from Strava's `map.polyline`), it's used both to
 * pin each entry/exit to the closest actual approach (tighter than just "first/last point in
 * range") and to reject a pass that only clips near both endpoints via an unrelated path. With
 * no polyline, falls back to matching against the two endpoint coordinates alone.
 *
 * [segmentDistanceMeters] — the segment's own known real-world length — bounds how long a single
 * pass may plausibly span, so an entry never gets stitched to an unrelated, much-later exit; see
 * [maxPlausibleDuration].
 */
fun matchAllSegmentPasses(
    track: List<TrackPoint>,
    startLat: Double,
    startLon: Double,
    endLat: Double,
    endLon: Double,
    segmentDistanceMeters: Double,
    proximityMeters: Double = SEGMENT_PROXIMITY_METERS,
    polyline: List<LatLng> = emptyList(),
): List<SegmentMatchResult> {
    val results = mutableListOf<SegmentMatchResult>()
    var searchOffset = 0
    while (searchOffset < track.size) {
        val bounds = if (polyline.size >= 2) {
            findEntryExitViaPolyline(track, polyline, proximityMeters, searchOffset, segmentDistanceMeters)
        } else {
            findEntryExitViaEndpoints(track, startLat, startLon, endLat, endLon, proximityMeters, searchOffset, segmentDistanceMeters)
        } ?: break
        val (entryIndex, exitIndex) = bounds
        results += buildMatchResult(track, entryIndex, exitIndex)
        searchOffset = exitIndex + 1
    }
    return results
}

/** Convenience for callers that only care about the first pass. */
fun matchSegment(
    track: List<TrackPoint>,
    startLat: Double,
    startLon: Double,
    endLat: Double,
    endLon: Double,
    segmentDistanceMeters: Double,
    proximityMeters: Double = SEGMENT_PROXIMITY_METERS,
    polyline: List<LatLng> = emptyList(),
): SegmentMatchResult? =
    matchAllSegmentPasses(track, startLat, startLon, endLat, endLon, segmentDistanceMeters, proximityMeters, polyline).firstOrNull()

private fun buildMatchResult(track: List<TrackPoint>, entryIndex: Int, exitIndex: Int): SegmentMatchResult {
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
    searchOffset: Int,
    segmentDistanceMeters: Double,
): Pair<Int, Int>? {
    val firstNearStart = (searchOffset until track.size).firstOrNull { index ->
        haversineMeters(track[index].latitude, track[index].longitude, startLat, startLon) <= proximityMeters
    } ?: return null

    val latestPlausibleExit = track[firstNearStart].timestamp.plus(maxPlausibleDuration(segmentDistanceMeters))
    val furthestPlausibleDistance = track[firstNearStart].cumulativeDistanceMeters + maxPlausibleDistanceMeters(segmentDistanceMeters)
    val exitIndex = ((firstNearStart + 1) until track.size)
        .asSequence()
        .takeWhile { index ->
            !track[index].timestamp.isAfter(latestPlausibleExit) && track[index].cumulativeDistanceMeters <= furthestPlausibleDistance
        }
        .firstOrNull { index ->
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
    searchOffset: Int,
    segmentDistanceMeters: Double,
): Pair<Int, Int>? {
    val start = polyline.first()
    val end = polyline.last()

    val roughEntry = (searchOffset until track.size).firstOrNull { index ->
        haversineMeters(track[index].latitude, track[index].longitude, start.latitude, start.longitude) <= proximityMeters
    } ?: return null
    val latestPlausibleExit = track[roughEntry].timestamp.plus(maxPlausibleDuration(segmentDistanceMeters))
    val furthestPlausibleDistance = track[roughEntry].cumulativeDistanceMeters + maxPlausibleDistanceMeters(segmentDistanceMeters)
    val roughExit = ((roughEntry + 1) until track.size)
        .asSequence()
        .takeWhile { index ->
            !track[index].timestamp.isAfter(latestPlausibleExit) && track[index].cumulativeDistanceMeters <= furthestPlausibleDistance
        }
        .firstOrNull { index ->
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
