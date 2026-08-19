package com.segmentanalyzer.domain.util

import com.segmentanalyzer.domain.model.LatLng

/** The point [fraction] (0f..1f) of the way along [points], measured by cumulative distance. */
fun pointAtFraction(points: List<LatLng>, fraction: Float): LatLng? {
    if (points.isEmpty()) return null
    if (points.size == 1 || fraction <= 0f) return points.first()
    if (fraction >= 1f) return points.last()

    val segmentLengths = points.zipWithNext { a, b -> haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude) }
    val totalLength = segmentLengths.sum()
    if (totalLength <= 0.0) return points.first()

    val targetDistance = fraction * totalLength
    var traveled = 0.0
    for (i in segmentLengths.indices) {
        val segmentLength = segmentLengths[i]
        if (traveled + segmentLength >= targetDistance) {
            val segmentFraction = if (segmentLength > 0.0) ((targetDistance - traveled) / segmentLength).toFloat() else 0f
            val a = points[i]
            val b = points[i + 1]
            return LatLng(
                latitude = a.latitude + (b.latitude - a.latitude) * segmentFraction,
                longitude = a.longitude + (b.longitude - a.longitude) * segmentFraction,
            )
        }
        traveled += segmentLength
    }
    return points.last()
}
