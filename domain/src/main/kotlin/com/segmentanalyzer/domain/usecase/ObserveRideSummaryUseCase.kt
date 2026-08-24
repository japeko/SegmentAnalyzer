package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.SummaryPeriod
import com.segmentanalyzer.domain.model.isIn
import com.segmentanalyzer.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
 *
 * [newPersonalBestCount] comes from [ObserveSegmentRecordsUseCase] — the same segment-record
 * source the Records screen uses — rather than [com.segmentanalyzer.domain.model.Ride.isPersonalBest],
 * which no repository ever sets true for a real imported ride.
 */
class ObserveRideSummaryUseCase @Inject constructor(
    private val rideRepository: RideRepository,
    private val observeSegmentRecords: ObserveSegmentRecordsUseCase,
) {
    operator fun invoke(period: SummaryPeriod): Flow<RideSummary> =
        combine(rideRepository.observeRides(), observeSegmentRecords(period)) { rides, records ->
            val inPeriod = rides.filter { it.startTime.isIn(period) }
            RideSummary(
                totalDistanceKm = inPeriod.sumOf { it.distanceMeters } / 1000.0,
                rideCount = inPeriod.size,
                elevationGainMeters = inPeriod.sumOf { it.elevationGainMeters },
                newPersonalBestCount = records.newPersonalBests.size,
            )
        }
}
