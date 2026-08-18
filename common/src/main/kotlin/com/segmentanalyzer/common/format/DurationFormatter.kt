package com.segmentanalyzer.common.format

import java.time.Duration

/** Formats a ride duration as "h:mm:ss" or "m:ss" for durations under an hour. */
fun Duration.toRideClock(): String {
    val totalSeconds = seconds.coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%d:%02d".format(minutes, secs)
    }
}
