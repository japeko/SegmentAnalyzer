package com.segmentanalyzer.domain.util

import com.segmentanalyzer.domain.model.LatLng

/** Decodes Google's "encoded polyline algorithm format" (used by Strava's segment `map.polyline`). */
fun decodePolyline(encoded: String, precision: Int = 5): List<LatLng> {
    val factor = Math.pow(10.0, precision.toDouble())
    val points = mutableListOf<LatLng>()
    var index = 0
    var lat = 0
    var lng = 0

    while (index < encoded.length) {
        var shift = 0
        var result = 0
        var b: Int
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        lat += if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        lng += if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)

        points.add(LatLng(lat / factor, lng / factor))
    }
    return points
}
