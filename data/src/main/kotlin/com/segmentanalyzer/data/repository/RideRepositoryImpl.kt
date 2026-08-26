package com.segmentanalyzer.data.repository

import androidx.room.withTransaction
import com.segmentanalyzer.data.local.SegmentAnalyzerDatabase
import com.segmentanalyzer.data.local.dao.RideDao
import com.segmentanalyzer.data.local.dao.RidePointDao
import com.segmentanalyzer.data.local.entity.RidePointEntity
import com.segmentanalyzer.data.mapper.toDomain
import com.segmentanalyzer.data.mapper.toEntity
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RideRepositoryImpl @Inject constructor(
    private val database: SegmentAnalyzerDatabase,
    private val rideDao: RideDao,
    private val ridePointDao: RidePointDao,
) : RideRepository {
    override fun observeRides(): Flow<List<Ride>> =
        rideDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeRide(rideId: Long): Flow<Ride?> =
        rideDao.rideById(rideId).map { entity -> entity?.toDomain() }

    override fun observeHasTrack(rideId: Long): Flow<Boolean> =
        ridePointDao.observeHasTrack(rideId)

    override suspend fun saveRides(rides: List<Ride>): Int =
        rideDao.insertIfNew(rides.map { it.toEntity() }).count { it != -1L }

    override suspend fun saveRide(ride: Ride): Long? = database.withTransaction {
        if (ride.externalId == null && rideDao.existsWithStartTime(ride.startTime.toEpochMilli())) {
            return@withTransaction null
        }
        val id = rideDao.insertIfNew(listOf(ride.toEntity())).first()
        if (id == -1L) return@withTransaction null
        if (ride.track.isNotEmpty()) {
            ridePointDao.insertAll(ride.track.mapIndexed { index, point -> point.toEntity(id, index) })
        }
        id
    }

    override suspend fun updateRide(rideId: Long, name: String, tag: String?, activityType: ActivityType) =
        rideDao.updateDetails(rideId, name, tag?.trim()?.takeIf { it.isNotEmpty() }, activityType)

    override suspend fun setTagForRides(rideIds: List<Long>, tag: String?) =
        rideDao.setTagForRides(rideIds, tag?.trim()?.takeIf { it.isNotEmpty() })

    override suspend fun setActivityTypeForRides(rideIds: List<Long>, activityType: ActivityType) =
        rideDao.setActivityTypeForRides(rideIds, activityType)

    override suspend fun deleteRide(rideId: Long) = rideDao.deleteById(rideId)

    override fun observeAllTags(): Flow<List<String>> = rideDao.observeDistinctTags()
}

private fun TrackPoint.toEntity(rideId: Long, sequence: Int): RidePointEntity = RidePointEntity(
    rideId = rideId,
    sequence = sequence,
    latitude = latitude,
    longitude = longitude,
    elevationMeters = elevationMeters,
    timestampEpochMillis = timestamp.toEpochMilli(),
    cumulativeDistanceMeters = cumulativeDistanceMeters,
    heartRateBpm = heartRateBpm,
    cadenceRpm = cadenceRpm,
    powerWatts = powerWatts,
)
