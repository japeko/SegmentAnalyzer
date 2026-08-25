package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.ViewedRidesRepository
import javax.inject.Inject

class MarkRideViewedUseCase @Inject constructor(
    private val viewedRidesRepository: ViewedRidesRepository,
) {
    suspend operator fun invoke(rideId: Long) = viewedRidesRepository.markRideViewed(rideId)
}
