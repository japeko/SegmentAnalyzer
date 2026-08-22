package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.common.format.isInCurrentMonth
import com.segmentanalyzer.common.format.isInCurrentWeek
import com.segmentanalyzer.common.format.isInCurrentYear
import com.segmentanalyzer.domain.model.SummaryPeriod
import com.segmentanalyzer.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

data class RideSummary(
    val totalDistanceKm: Double,
    val rideCount: Int,
    val elevationGainMeters: Double,
    val newPersonalBestCount: Int,
)

/**
 * Rolls rides within a [SummaryPeriod] up into the stats shown at the top of the history screen.
 * Computed here, over the repository's already-collected [Flow], rather than as a separate SQL
 * aggregate query — at personal-ride-log scale this keeps the aggregation logic in the domain
 * layer instead of duplicating it in SQL.
 */
class ObserveRideSummaryUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    operator fun invoke(period: SummaryPeriod): Flow<RideSummary> =
        rideRepository.observeRides().map { rides ->
            val inPeriod = rides.filter { it.startTime.isIn(period) }
            RideSummary(
                totalDistanceKm = inPeriod.sumOf { it.distanceMeters } / 1000.0,
                rideCount = inPeriod.size,
                elevationGainMeters = inPeriod.sumOf { it.elevationGainMeters },
                newPersonalBestCount = inPeriod.count { it.isPersonalBest },
            )
        }

    private fun Instant.isIn(period: SummaryPeriod): Boolean = when (period) {
        SummaryPeriod.THIS_WEEK -> isInCurrentWeek()
        SummaryPeriod.THIS_MONTH -> isInCurrentMonth()
        SummaryPeriod.THIS_YEAR -> isInCurrentYear()
        SummaryPeriod.ALL_TIME -> true
    }
}
