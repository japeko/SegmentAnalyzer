package com.segmentanalyzer.data.repository

import com.segmentanalyzer.data.local.dao.StravaSegmentEffortDao
import com.segmentanalyzer.data.local.entity.StravaSegmentEffortEntity
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.repository.StravaSegmentEffortRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import javax.inject.Inject

internal class StravaSegmentEffortRepositoryImpl @Inject constructor(
    private val dao: StravaSegmentEffortDao,
) : StravaSegmentEffortRepository {

    override fun observeEffortsForRide(rideId: Long): Flow<List<StravaSegmentEffort>> =
        dao.observeForRide(rideId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun replaceEffortsForRide(rideId: Long, efforts: List<StravaSegmentEffort>) {
        dao.replaceForRide(rideId, efforts.map { it.toEntity(rideId) })
    }
}

internal fun StravaSegmentEffortEntity.toDomain(): StravaSegmentEffort = StravaSegmentEffort(
    effortExternalId = effortExternalId,
    segmentExternalId = segmentExternalId,
    segmentName = segmentName,
    elapsedTime = Duration.ofSeconds(elapsedTimeSeconds),
    distanceMeters = distanceMeters,
    komRank = komRank,
    prRank = prRank,
)

internal fun StravaSegmentEffort.toEntity(rideId: Long): StravaSegmentEffortEntity = StravaSegmentEffortEntity(
    rideId = rideId,
    effortExternalId = effortExternalId,
    segmentExternalId = segmentExternalId,
    segmentName = segmentName,
    elapsedTimeSeconds = elapsedTime.seconds,
    distanceMeters = distanceMeters,
    komRank = komRank,
    prRank = prRank,
)
