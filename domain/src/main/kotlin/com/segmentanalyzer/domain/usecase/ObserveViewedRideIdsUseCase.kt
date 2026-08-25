package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.ViewedRidesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveViewedRideIdsUseCase @Inject constructor(
    private val viewedRidesRepository: ViewedRidesRepository,
) {
    operator fun invoke(): Flow<Set<Long>> = viewedRidesRepository.observeViewedRideIds()
}
