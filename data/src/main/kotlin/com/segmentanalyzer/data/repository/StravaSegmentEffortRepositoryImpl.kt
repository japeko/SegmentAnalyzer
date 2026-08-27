package com.segmentanalyzer.data.repository

import com.segmentanalyzer.data.local.dao.StravaSegmentEffortDao
import com.segmentanalyzer.data.local.dao.StravaSegmentEffortPointDao
import com.segmentanalyzer.data.local.entity.StravaSegmentEffortEntity
import com.segmentanalyzer.data.local.entity.StravaSegmentEffortPointEntity
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.model.StravaSegmentEffortPoint
import com.segmentanalyzer.domain.repository.StravaSegmentEffortRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import javax.inject.Inject

internal class StravaSegmentEffortRepositoryImpl @Inject constructor(
    private val dao: StravaSegmentEffortDao,
    private val pointDao: StravaSegmentEffortPointDao,
) : StravaSegmentEffortRepository {

    override fun observeEffortsForRide(rideId: Long): Flow<List<StravaSegmentEffort>> =
        dao.observeForRide(rideId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun replaceEffortsForRide(rideId: Long, efforts: List<StravaSegmentEffort>) {
        dao.replaceForRide(rideId, efforts.map { it.toEntity(rideId) })
    }

    override suspend fun saveEffortDetail(effortExternalId: String, detail: StravaSegmentEffortDetail) {
        dao.updateDetail(
            effortExternalId = effortExternalId,
            avgSpeedKmh = detail.avgSpeedKmh,
            maxSpeedKmh = detail.maxSpeedKmh,
            elevationGainMeters = detail.elevationGainMeters,
            avgWatts = detail.avgWatts,
            avgHeartRateBpm = detail.avgHeartRateBpm,
            avgCadenceRpm = detail.avgCadenceRpm,
        )
    }

    override suspend fun saveEffortTrack(effortExternalId: String, points: List<StravaSegmentEffortPoint>) {
        pointDao.replaceForEffort(
            effortExternalId,
            points.mapIndexed { index, point ->
                StravaSegmentEffortPointEntity(
                    effortExternalId = effortExternalId,
                    sequence = index,
                    timeSeconds = point.timeSeconds,
                    distanceMeters = point.distanceMeters,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    elevationMeters = point.elevationMeters,
                )
            },
        )
    }

    override suspend fun trackForEffort(effortExternalId: String): List<StravaSegmentEffortPoint> =
        pointDao.forEffort(effortExternalId).map {
            StravaSegmentEffortPoint(
                timeSeconds = it.timeSeconds,
                distanceMeters = it.distanceMeters,
                latitude = it.latitude,
                longitude = it.longitude,
                elevationMeters = it.elevationMeters,
            )
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
    detail = avgSpeedKmh?.let {
        StravaSegmentEffortDetail(
            avgSpeedKmh = it,
            maxSpeedKmh = maxSpeedKmh ?: 0.0,
            elevationGainMeters = elevationGainMeters ?: 0.0,
            avgWatts = avgWatts,
            avgHeartRateBpm = avgHeartRateBpm,
            avgCadenceRpm = avgCadenceRpm,
        )
    },
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
    avgSpeedKmh = detail?.avgSpeedKmh,
    maxSpeedKmh = detail?.maxSpeedKmh,
    elevationGainMeters = detail?.elevationGainMeters,
    avgWatts = detail?.avgWatts,
    avgHeartRateBpm = detail?.avgHeartRateBpm,
    avgCadenceRpm = detail?.avgCadenceRpm,
)
