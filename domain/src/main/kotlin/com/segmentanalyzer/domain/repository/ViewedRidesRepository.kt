package com.segmentanalyzer.domain.repository

import kotlinx.coroutines.flow.Flow

/** Tracks which rides the rider has already opened the detail screen for. */
interface ViewedRidesRepository {
    fun observeViewedRideIds(): Flow<Set<Long>>

    suspend fun markRideViewed(rideId: Long)
}
