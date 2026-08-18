package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.common.format.isInCurrentMonth
import com.segmentanalyzer.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class MonthlySummary(
    val totalDistanceKm: Double,
    val rideCount: Int,
    val elevationGainMeters: Double,
    val newPersonalBestCount: Int,
)

/**
 * Rolls the current calendar month's rides up into the stats shown at the top of the
 * history screen. Computed here, over the repository's already-collected [Flow], rather than
 * as a separate SQL aggregate query — at personal-ride-log scale this keeps the aggregation
 * logic in the domain layer instead of duplicating it in SQL.
 */
class ObserveMonthlySummaryUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    operator fun invoke(): Flow<MonthlySummary> =
        rideRepository.observeRides().map { rides ->
            val thisMonth = rides.filter { it.startTime.isInCurrentMonth() }
            MonthlySummary(
                totalDistanceKm = thisMonth.sumOf { it.distanceMeters } / 1000.0,
                rideCount = thisMonth.size,
                elevationGainMeters = thisMonth.sumOf { it.elevationGainMeters },
                newPersonalBestCount = thisMonth.count { it.isPersonalBest },
            )
        }
}
