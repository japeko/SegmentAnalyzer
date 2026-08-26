package com.segmentanalyzer.domain.util

import com.segmentanalyzer.domain.model.TrackPoint

/**
 * Gradient percent for each consecutive pair of [points] (size = points.size - 1, or empty if
 * fewer than 2 points). A segment with unknown elevation on either end is treated as flat (0.0),
 * rather than dropped, so the result always lines up 1:1 with the line segments drawn on a map.
 */
fun gradientPercentSegments(points: List<TrackPoint>): List<Double> {
    if (points.size < 2) return emptyList()
    return points.zipWithNext { a, b ->
        val elevationA = a.elevationMeters
        val elevationB = b.elevationMeters
        if (elevationA == null || elevationB == null) return@zipWithNext 0.0

        val rise = (elevationB - elevationA).toDouble()
        val run = haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        if (run <= 0.0) 0.0 else (rise / run) * 100.0
    }
}

data class SlopePoint(val distanceMeters: Double, val gradePercent: Double)

/**
 * A single, resampled slope-vs-distance curve for [track] — slope is a property of the route
 * itself, not of any one ride's pace along it, so unlike a speed or time-gap comparison there's
 * only ever one line to plot regardless of how many attempts are being compared.
 */
fun slopeProfile(track: List<TrackPoint>, sampleCount: Int = 40): List<SlopePoint> {
    if (track.size < 2) return emptyList()
    val grades = gradientPercentSegments(track)

    // Each raw grade sample sits at its pair's midpoint distance; dropped (not clamped) if a GPS
    // blip makes it not strictly advance, same reasoning as the time-gap/speed curves.
    val curve = mutableListOf<Pair<Double, Double>>()
    track.zipWithNext().forEachIndexed { index, (a, b) ->
        val midDistance = (a.cumulativeDistanceMeters + b.cumulativeDistanceMeters) / 2.0
        if (curve.isNotEmpty() && midDistance <= curve.last().first) return@forEachIndexed
        curve += midDistance to grades[index]
    }
    // The resample range spans the track's actual distance, not just the last midpoint sample —
    // gradeAt clamps to that sample's value for the final stretch past it.
    val maxDistance = track.last().cumulativeDistanceMeters
    if (curve.isEmpty() || maxDistance <= 0.0) return emptyList()

    return (0 until sampleCount).map { index ->
        val distance = index * maxDistance / (sampleCount - 1)
        SlopePoint(distanceMeters = distance, gradePercent = gradeAt(curve, distance))
    }
}

/** Linear interpolation of [curve]'s grade at [distance], clamped to the curve's ends. */
private fun gradeAt(curve: List<Pair<Double, Double>>, distance: Double): Double {
    if (distance <= curve.first().first) return curve.first().second
    if (distance >= curve.last().first) return curve.last().second

    val nextIndex = curve.indexOfFirst { it.first >= distance }
    val (prevDistance, prevGrade) = curve[nextIndex - 1]
    val (nextDistance, nextGrade) = curve[nextIndex]
    if (nextDistance == prevDistance) return prevGrade
    val fraction = (distance - prevDistance) / (nextDistance - prevDistance)
    return prevGrade + fraction * (nextGrade - prevGrade)
}
