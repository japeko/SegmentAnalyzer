package com.segmentanalyzer.data.repository

import com.segmentanalyzer.data.local.dao.RideDao
import com.segmentanalyzer.data.mapper.toDomain
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RideRepositoryImpl @Inject constructor(
    private val rideDao: RideDao,
) : RideRepository {
    override fun observeRides(): Flow<List<Ride>> =
        rideDao.observeAll().map { entities -> entities.map { it.toDomain() } }
}
