package com.segmentanalyzer.data.local.fit

import com.garmin.fit.Activity
import com.garmin.fit.ActivityMesg
import com.garmin.fit.DateTime
import com.garmin.fit.Event
import com.garmin.fit.EventType
import com.garmin.fit.FileEncoder
import com.garmin.fit.FileIdMesg
import com.garmin.fit.LapMesg
import com.garmin.fit.Manufacturer
import com.garmin.fit.RecordMesg
import com.garmin.fit.SessionMesg
import com.garmin.fit.Sport
import com.garmin.fit.File as FitFileType
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.inject.Inject

internal data class FitExportPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Float?,
    val timestamp: Instant,
    val cumulativeDistanceMeters: Double,
    val heartRateBpm: Int?,
    val cadenceRpm: Int?,
    val powerWatts: Int?,
)

/** Degrees to FIT's semicircle position encoding — the inverse of FitFileParser's SEMICIRCLES_TO_DEGREES. */
private const val DEGREES_TO_SEMICIRCLES = 2147483648.0 / 180.0

/**
 * Thin wrapper around the official Garmin FIT Java SDK's encoder (the write-side counterpart to
 * [FitFileParser]), writing one segment attempt's recorded track as a standalone .fit activity
 * file — FileId, one Record message per point, then Lap/Session/Activity summaries. This is the
 * minimal structure FIT-consuming tools expect from a valid activity file.
 */
internal class FitFileEncoder @Inject constructor() {

    fun encode(destination: File, points: List<FitExportPoint>) {
        require(points.isNotEmpty()) { "can't encode a FIT file with no track points" }
        val startTime = points.first().timestamp
        val endTime = points.last().timestamp
        val elapsedSeconds = Duration.between(startTime, endTime).toMillis() / 1000f
        val totalDistance = (points.last().cumulativeDistanceMeters - points.first().cumulativeDistanceMeters).toFloat()
        val avgSpeed = if (elapsedSeconds > 0) totalDistance / elapsedSeconds else 0f
        val avgHeartRate = points.averageIntOrNull { it.heartRateBpm }
        val avgCadence = points.averageIntOrNull { it.cadenceRpm }
        val avgPower = points.averageIntOrNull { it.powerWatts }

        val encoder = FileEncoder(destination)
        try {
            encoder.write(fileIdMesg(startTime))
            encoder.write(points.map { it.toRecordMesg() })
            encoder.write(lapMesg(startTime, endTime, elapsedSeconds, totalDistance, avgSpeed, avgHeartRate, avgCadence, avgPower))
            encoder.write(sessionMesg(startTime, endTime, elapsedSeconds, totalDistance, avgSpeed, avgHeartRate, avgCadence, avgPower))
            encoder.write(activityMesg(endTime, elapsedSeconds))
        } finally {
            encoder.close()
        }
    }

    private fun fileIdMesg(startTime: Instant): FileIdMesg {
        val mesg = FileIdMesg()
        mesg.setType(FitFileType.ACTIVITY)
        mesg.setManufacturer(Manufacturer.GARMIN)
        mesg.setTimeCreated(startTime.toFitDateTime())
        return mesg
    }

    private fun FitExportPoint.toRecordMesg(): RecordMesg {
        val mesg = RecordMesg()
        mesg.setTimestamp(timestamp.toFitDateTime())
        mesg.setPositionLat((latitude * DEGREES_TO_SEMICIRCLES).toInt())
        mesg.setPositionLong((longitude * DEGREES_TO_SEMICIRCLES).toInt())
        elevationMeters?.let { mesg.setAltitude(it) }
        mesg.setDistance(cumulativeDistanceMeters.toFloat())
        heartRateBpm?.let { mesg.setHeartRate(it.toShort()) }
        cadenceRpm?.let { mesg.setCadence(it.toShort()) }
        powerWatts?.let { mesg.setPower(it) }
        return mesg
    }

    private fun lapMesg(
        startTime: Instant,
        endTime: Instant,
        elapsedSeconds: Float,
        totalDistance: Float,
        avgSpeed: Float,
        avgHeartRate: Int?,
        avgCadence: Int?,
        avgPower: Int?,
    ): LapMesg {
        val mesg = LapMesg()
        mesg.setTimestamp(endTime.toFitDateTime())
        mesg.setStartTime(startTime.toFitDateTime())
        mesg.setTotalElapsedTime(elapsedSeconds)
        mesg.setTotalTimerTime(elapsedSeconds)
        mesg.setTotalDistance(totalDistance)
        mesg.setAvgSpeed(avgSpeed)
        mesg.setEvent(Event.TIMER)
        mesg.setEventType(EventType.STOP)
        avgHeartRate?.let { mesg.setAvgHeartRate(it.toShort()) }
        avgCadence?.let { mesg.setAvgCadence(it.toShort()) }
        avgPower?.let { mesg.setAvgPower(it) }
        return mesg
    }

    private fun sessionMesg(
        startTime: Instant,
        endTime: Instant,
        elapsedSeconds: Float,
        totalDistance: Float,
        avgSpeed: Float,
        avgHeartRate: Int?,
        avgCadence: Int?,
        avgPower: Int?,
    ): SessionMesg {
        val mesg = SessionMesg()
        mesg.setTimestamp(endTime.toFitDateTime())
        mesg.setStartTime(startTime.toFitDateTime())
        mesg.setTotalElapsedTime(elapsedSeconds)
        mesg.setTotalTimerTime(elapsedSeconds)
        mesg.setTotalDistance(totalDistance)
        mesg.setAvgSpeed(avgSpeed)
        mesg.setSport(Sport.CYCLING)
        mesg.setEvent(Event.TIMER)
        mesg.setEventType(EventType.STOP)
        avgHeartRate?.let { mesg.setAvgHeartRate(it.toShort()) }
        avgCadence?.let { mesg.setAvgCadence(it.toShort()) }
        avgPower?.let { mesg.setAvgPower(it) }
        return mesg
    }

    private fun activityMesg(endTime: Instant, elapsedSeconds: Float): ActivityMesg {
        val mesg = ActivityMesg()
        mesg.setTimestamp(endTime.toFitDateTime())
        mesg.setTotalTimerTime(elapsedSeconds)
        mesg.setNumSessions(1)
        mesg.setType(Activity.MANUAL)
        mesg.setEvent(Event.TIMER)
        mesg.setEventType(EventType.STOP)
        return mesg
    }
}

private fun Instant.toFitDateTime(): DateTime = DateTime(Date.from(this))

private fun List<FitExportPoint>.averageIntOrNull(selector: (FitExportPoint) -> Int?): Int? {
    val values = mapNotNull(selector)
    return if (values.isEmpty()) null else values.average().toInt()
}
