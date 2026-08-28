package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.RideComparisonInsightRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Whether this phone's on-device model is ready to generate a Compare Rides AI insight — see [RideComparisonInsightRepository.observeAvailability]. */
class ObserveRideComparisonInsightAvailabilityUseCase @Inject constructor(
    private val repository: RideComparisonInsightRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeAvailability()
}
