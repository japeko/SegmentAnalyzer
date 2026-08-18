package com.segmentanalyzer.data.local.fit

import com.garmin.fit.Decode
import com.garmin.fit.FitRuntimeException
import com.garmin.fit.MesgBroadcaster
import com.garmin.fit.RecordMesgListener
import com.garmin.fit.SessionMesg
import com.garmin.fit.SessionMesgListener
import com.garmin.fit.Sport
import com.garmin.fit.SubSport
import java.io.InputStream
import java.time.Instant
import javax.inject.Inject

internal data class FitTrackPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Float?,
    val timestamp: Instant,
    val cumulativeDistanceMeters: Double,
    val heartRateBpm: Int?,
    val cadenceRpm: Int?,
    val powerWatts: Int?,
)

internal data class FitSessionSummary(
    val sport: Sport?,
    val subSport: SubSport?,
    val startTime: Instant?,
    val durationSeconds: Float?,
    val distanceMeters: Float?,
    val elevationGainMeters: Int?,
    val trackPoints: List<FitTrackPoint>,
)

/** Thrown when a FIT file can't be read as a ride: corrupt data, or no session message at all. */
internal class FitParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** FIT stores lat/lon as semicircles; this converts to degrees. */
private const val SEMICIRCLES_TO_DEGREES = 180.0 / 2147483648.0

/**
 * Thin wrapper around the official Garmin FIT Java SDK (`com.garmin:fit`), extracting the session
 * summary fields plus the per-point GPS track (for segment-attempt matching).
 */
internal class FitFileParser @Inject constructor() {

    fun parse(inputStream: InputStream): FitSessionSummary {
        var session: SessionMesg? = null
        val trackPoints = mutableListOf<FitTrackPoint>()

        val broadcaster = MesgBroadcaster(Decode())
        broadcaster.addListener(SessionMesgListener { mesg -> session = mesg })
        broadcaster.addListener(
            RecordMesgListener { mesg ->
                val lat = mesg.positionLat
                val lon = mesg.positionLong
                val timestamp = mesg.timestamp?.date?.toInstant()
                if (lat != null && lon != null && timestamp != null) {
                    trackPoints += FitTrackPoint(
                        latitude = lat * SEMICIRCLES_TO_DEGREES,
                        longitude = lon * SEMICIRCLES_TO_DEGREES,
                        elevationMeters = mesg.altitude,
                        timestamp = timestamp,
                        cumulativeDistanceMeters = (mesg.distance ?: 0f).toDouble(),
                        heartRateBpm = mesg.heartRate?.toInt(),
                        cadenceRpm = mesg.cadence?.toInt(),
                        powerWatts = mesg.power,
                    )
                }
            },
        )

        try {
            broadcaster.run(inputStream)
        } catch (e: FitRuntimeException) {
            throw FitParseException(e.message ?: "couldn't parse the FIT file", e)
        }

        val result = session ?: throw FitParseException("no session data found in this FIT file")
        return FitSessionSummary(
            sport = result.sport,
            subSport = result.subSport,
            startTime = result.startTime?.date?.toInstant(),
            durationSeconds = result.totalElapsedTime,
            distanceMeters = result.totalDistance,
            elevationGainMeters = result.totalAscent,
            trackPoints = trackPoints,
        )
    }
}
