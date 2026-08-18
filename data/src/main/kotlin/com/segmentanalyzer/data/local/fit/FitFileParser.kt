package com.segmentanalyzer.data.local.fit

import com.garmin.fit.Decode
import com.garmin.fit.FitRuntimeException
import com.garmin.fit.MesgBroadcaster
import com.garmin.fit.SessionMesg
import com.garmin.fit.SessionMesgListener
import com.garmin.fit.Sport
import com.garmin.fit.SubSport
import java.io.InputStream
import java.time.Instant
import javax.inject.Inject

internal data class FitSessionSummary(
    val sport: Sport?,
    val subSport: SubSport?,
    val startTime: Instant?,
    val durationSeconds: Float?,
    val distanceMeters: Float?,
    val elevationGainMeters: Int?,
)

/** Thrown when a FIT file can't be read as a ride: corrupt data, or no session message at all. */
internal class FitParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thin wrapper around the official Garmin FIT Java SDK (`com.garmin:fit`), extracting just the
 * session summary fields needed for a ride — no per-point telemetry (see plan for the scope cut).
 */
internal class FitFileParser @Inject constructor() {

    fun parse(inputStream: InputStream): FitSessionSummary {
        var session: SessionMesg? = null
        val broadcaster = MesgBroadcaster(Decode())
        broadcaster.addListener(SessionMesgListener { mesg -> session = mesg })

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
        )
    }
}
