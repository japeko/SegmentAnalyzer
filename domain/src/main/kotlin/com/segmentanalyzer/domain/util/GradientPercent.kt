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
