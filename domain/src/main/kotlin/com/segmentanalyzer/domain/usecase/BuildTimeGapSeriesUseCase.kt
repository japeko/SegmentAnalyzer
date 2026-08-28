package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.TrackPoint
import java.time.Duration
import javax.inject.Inject

data class TimeGapPoint(val distanceMeters: Double, val gapSeconds: Double)
data class TimeGapSeries(val attemptId: Long, val points: List<TimeGapPoint>)

/**
 * Builds, for each entry in [otherTracks], a series of how many seconds ahead (negative) or
 * behind (positive) [referenceTrack] it was at evenly-spaced distances along the segment.
 *
 * Takes already-fetched tracks rather than ids + a repository — the caller may need to resolve
 * ids from more than one source (e.g. Compare Rides mixing a rider's own [SegmentAttempt]s with
 * imported [com.segmentanalyzer.domain.model.GuestAttempt]s), which this has no business knowing
 * about; it only does the distance-sampling math.
 */
class BuildTimeGapSeriesUseCase @Inject constructor() {
    operator fun invoke(
        referenceTrack: List<TrackPoint>,
        otherTracks: Map<Long, List<TrackPoint>>,
        segmentDistanceMeters: Double,
        sampleCount: Int = 40,
    ): List<TimeGapSeries> {
        val referenceCurve = elapsedSecondsCurve(referenceTrack)
        val distances = (0 until sampleCount).map { index -> index * segmentDistanceMeters / (sampleCount - 1) }

        return otherTracks.map { (attemptId, track) ->
            val curve = elapsedSecondsCurve(track)
            val points = distances.map { distance ->
                TimeGapPoint(
                    distanceMeters = distance,
                    gapSeconds = elapsedSecondsAt(curve, distance) - elapsedSecondsAt(referenceCurve, distance),
                )
            }
            TimeGapSeries(attemptId = attemptId, points = points)
        }
    }
}

/** A monotonic (distance, elapsedSeconds) curve, dropping any point that doesn't strictly advance distance. */
private fun elapsedSecondsCurve(track: List<TrackPoint>): List<Pair<Double, Double>> {
    if (track.isEmpty()) return emptyList()
    val startTime = track.first().timestamp
    val curve = mutableListOf<Pair<Double, Double>>()
    for (point in track) {
        val distance = point.cumulativeDistanceMeters
        if (curve.isNotEmpty() && distance <= curve.last().first) continue
        curve += distance to Duration.between(startTime, point.timestamp).toMillis() / 1000.0
    }
    return curve
}

/** Linear interpolation of elapsed seconds at [distance], clamped to the curve's ends. */
private fun elapsedSecondsAt(curve: List<Pair<Double, Double>>, distance: Double): Double {
    if (curve.isEmpty()) return 0.0
    if (distance <= curve.first().first) return curve.first().second
    if (distance >= curve.last().first) return curve.last().second

    val nextIndex = curve.indexOfFirst { it.first >= distance }
    val (prevDistance, prevSeconds) = curve[nextIndex - 1]
    val (nextDistance, nextSeconds) = curve[nextIndex]
    if (nextDistance == prevDistance) return prevSeconds
    val fraction = (distance - prevDistance) / (nextDistance - prevDistance)
    return prevSeconds + fraction * (nextSeconds - prevSeconds)
}
