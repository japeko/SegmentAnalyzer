package com.segmentanalyzer.data.mapper

import com.segmentanalyzer.data.local.entity.RideEntity
import com.segmentanalyzer.domain.model.Ride
import java.time.Duration
import java.time.Instant

fun RideEntity.toDomain(): Ride = Ride(
    id = id,
    name = name,
    activityType = activityType,
    source = source,
    startTime = Instant.ofEpochMilli(startTimeEpochMillis),
    duration = Duration.ofMillis(durationMillis),
    distanceMeters = distanceMeters,
    elevationGainMeters = elevationGainMeters,
    isPersonalBest = isPersonalBest,
    elevationProfile = elevationProfilePreview,
    sourceFilePath = sourceFilePath,
)
