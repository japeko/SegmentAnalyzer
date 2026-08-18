package com.segmentanalyzer.common.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val RIDE_CARD_DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** Formats an [Instant] as a short localized date, e.g. "Aug 16, 2026". */
fun Instant.toRideCardDate(zoneId: ZoneId = ZoneId.systemDefault()): String =
    RIDE_CARD_DATE_FORMATTER.format(atZone(zoneId))

/** True if this instant falls within the current calendar month in [zoneId]. */
fun Instant.isInCurrentMonth(zoneId: ZoneId = ZoneId.systemDefault()): Boolean {
    val now = Instant.now().atZone(zoneId)
    val self = atZone(zoneId)
    return now.year == self.year && now.month == self.month
}
