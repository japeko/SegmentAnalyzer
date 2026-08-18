package com.segmentanalyzer.data.local.gpx

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.Instant
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class GpxTrackSummary(
    val name: String?,
    /** Free-text from `<trk><type>`, if the file happens to have one — GPX has no required field. */
    val activityTypeHint: String?,
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Double,
    val elevationGainMeters: Double,
)

/** Thrown when a GPX file can't be read as a ride: malformed XML, or no timestamped track points. */
internal class GpxParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Streaming GPX 1.0/1.1 parser using the platform's built-in `XmlPullParser` — no external
 * library needed, unlike FIT's binary format. Distance is accumulated via the haversine formula
 * between consecutive `<trkpt>` points (GPX has no native cumulative-distance field); elevation
 * gain is a naive sum of positive `<ele>` deltas (no smoothing — GPS elevation is noisy, but
 * that's the same scope cut already applied elsewhere to per-point telemetry).
 */
internal class GpxFileParser @Inject constructor() {

    fun parse(inputStream: InputStream): GpxTrackSummary {
        val parser: XmlPullParser = try {
            Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(inputStream, null)
            }
        } catch (e: Exception) {
            throw GpxParseException("couldn't parse this GPX file's XML", e)
        }

        var name: String? = null
        var activityTypeHint: String? = null
        var firstTime: Instant? = null
        var lastTime: Instant? = null
        var distanceMeters = 0.0
        var elevationGainMeters = 0.0

        var lastLat: Double? = null
        var lastLon: Double? = null
        var lastEle: Double? = null

        var currentTag = ""
        var trkptLat: Double? = null
        var trkptLon: Double? = null

        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "trkpt") {
                            trkptLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            trkptLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty()) {
                            when (currentTag) {
                                "name" -> if (name == null) name = text
                                "type" -> if (activityTypeHint == null) activityTypeHint = text
                                "time" -> runCatching { Instant.parse(text) }.getOrNull()?.let { instant ->
                                    if (firstTime == null) firstTime = instant
                                    lastTime = instant
                                }
                                "ele" -> text.toDoubleOrNull()?.let { ele ->
                                    lastEle?.let { previous -> if (ele > previous) elevationGainMeters += ele - previous }
                                    lastEle = ele
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name == "trkpt") {
                            val lat = trkptLat
                            val lon = trkptLon
                            if (lat != null && lon != null) {
                                if (lastLat != null && lastLon != null) {
                                    distanceMeters += haversineMeters(lastLat, lastLon, lat, lon)
                                }
                                lastLat = lat
                                lastLon = lon
                            }
                            trkptLat = null
                            trkptLon = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            throw GpxParseException("couldn't parse this GPX file's XML", e)
        }

        val resolvedStart = firstTime ?: throw GpxParseException("no timestamped track points found in this GPX file")
        return GpxTrackSummary(
            name = name,
            activityTypeHint = activityTypeHint,
            startTime = resolvedStart,
            endTime = lastTime ?: resolvedStart,
            distanceMeters = distanceMeters,
            elevationGainMeters = elevationGainMeters,
        )
    }
}

private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusMeters = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return earthRadiusMeters * 2 * atan2(sqrt(a), sqrt(1 - a))
}
