package com.segmentanalyzer.data.repository

import com.segmentanalyzer.data.local.ViewedRidesStore
import com.segmentanalyzer.domain.repository.ViewedRidesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class ViewedRidesRepositoryImpl @Inject constructor(
    private val viewedRidesStore: ViewedRidesStore,
) : ViewedRidesRepository {

    override fun observeViewedRideIds(): Flow<Set<Long>> = viewedRidesStore.viewedRideIds

    override suspend fun markRideViewed(rideId: Long) = viewedRidesStore.markViewed(rideId)
}
