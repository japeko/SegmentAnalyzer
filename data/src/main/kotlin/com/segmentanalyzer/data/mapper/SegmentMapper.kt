package com.segmentanalyzer.data.mapper

import com.segmentanalyzer.data.local.entity.SegmentEntity
import com.segmentanalyzer.domain.model.Segment

fun SegmentEntity.toDomain(): Segment = Segment(
    id = id,
    externalId = externalId,
    name = name,
    distanceMeters = distanceMeters,
    averageGradePercent = averageGradePercent,
    maximumGradePercent = maximumGradePercent,
    elevationGainMeters = elevationGainMeters,
    climbCategory = climbCategory,
    city = city,
    state = state,
)

fun Segment.toEntity(): SegmentEntity = SegmentEntity(
    id = 0,
    externalId = externalId,
    name = name,
    distanceMeters = distanceMeters,
    averageGradePercent = averageGradePercent,
    maximumGradePercent = maximumGradePercent,
    elevationGainMeters = elevationGainMeters,
    climbCategory = climbCategory,
    city = city,
    state = state,
)
