package com.segmentanalyzer.domain.model

import com.segmentanalyzer.common.format.isInCurrentMonth
import com.segmentanalyzer.common.format.isInCurrentWeek
import com.segmentanalyzer.common.format.isInCurrentYear
import java.time.Instant

/** The time window the Rides page's quick-stats summary and ride list are rolled up over. */
enum class SummaryPeriod {
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME,
}

/** True if this instant falls within [period]. */
internal fun Instant.isIn(period: SummaryPeriod): Boolean = when (period) {
    SummaryPeriod.THIS_WEEK -> isInCurrentWeek()
    SummaryPeriod.THIS_MONTH -> isInCurrentMonth()
    SummaryPeriod.THIS_YEAR -> isInCurrentYear()
    SummaryPeriod.ALL_TIME -> true
}
