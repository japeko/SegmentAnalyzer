package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import java.time.Duration
import javax.inject.Inject

data class SpeedPoint(val distanceMeters: Double, val speedKmh: Double)
data class SpeedSeries(val attemptId: Long, val points: List<SpeedPoint>)

/**
 * Builds, for each of [attemptIds], a speed-vs-distance curve at evenly-spaced distances along
 * the segment — the same sampling shape as [BuildTimeGapSeriesUseCase], so both charts plot on an
 * identical x-axis.
 */
class BuildSpeedSeriesUseCase @Inject constructor(
    private val segmentAttemptRepository: SegmentAttemptRepository,
) {
    suspend operator fun invoke(
        attemptIds: List<Long>,
        segmentDistanceMeters: Double,
        sampleCount: Int = 40,
    ): List<SpeedSeries> {
        val distances = (0 until sampleCount).map { index -> index * segmentDistanceMeters / (sampleCount - 1) }

        return attemptIds.map { attemptId ->
            val curve = speedCurve(segmentAttemptRepository.trackPointsForAttempt(attemptId))
            val points = distances.map { distance -> SpeedPoint(distance, speedAt(curve, distance)) }
            SpeedSeries(attemptId = attemptId, points = points)
        }
    }
}

/**
 * A (distance, speedKmh) curve from consecutive track-point pairs, each speed sample placed at
 * its pair's midpoint distance so interpolation between samples stays centered on real data.
 */
private fun speedCurve(track: List<TrackPoint>): List<Pair<Double, Double>> {
    if (track.size < 2) return emptyList()
    val curve = mutableListOf<Pair<Double, Double>>()
    for (i in 1 until track.size) {
        val prev = track[i - 1]
        val curr = track[i]
        val distanceDelta = curr.cumulativeDistanceMeters - prev.cumulativeDistanceMeters
        val secondsDelta = Duration.between(prev.timestamp, curr.timestamp).toMillis() / 1000.0
        if (distanceDelta <= 0.0 || secondsDelta <= 0.0) continue
        val speedKmh = (distanceDelta / secondsDelta) * 3.6
        val midDistance = (prev.cumulativeDistanceMeters + curr.cumulativeDistanceMeters) / 2.0
        curve += midDistance to speedKmh
    }
    return curve
}

/** Linear interpolation of [curve]'s speed at [distance], clamped to the curve's ends. */
private fun speedAt(curve: List<Pair<Double, Double>>, distance: Double): Double {
    if (curve.isEmpty()) return 0.0
    if (distance <= curve.first().first) return curve.first().second
    if (distance >= curve.last().first) return curve.last().second

    val nextIndex = curve.indexOfFirst { it.first >= distance }
    val (prevDistance, prevSpeed) = curve[nextIndex - 1]
    val (nextDistance, nextSpeed) = curve[nextIndex]
    if (nextDistance == prevDistance) return prevSpeed
    val fraction = (distance - prevDistance) / (nextDistance - prevDistance)
    return prevSpeed + fraction * (nextSpeed - prevSpeed)
}
