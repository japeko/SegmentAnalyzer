package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Ride
import kotlinx.coroutines.flow.Flow

/** Read/write access to the locally stored ride history. All rides live on-device. */
interface RideRepository {
    /** All rides, most recent first. */
    fun observeRides(): Flow<List<Ride>>
}
