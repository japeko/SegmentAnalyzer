package com.segmentanalyzer.data.repository

import com.segmentanalyzer.data.local.dao.SegmentDao
import com.segmentanalyzer.data.mapper.toDomain
import com.segmentanalyzer.data.mapper.toEntity
import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.repository.SegmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SegmentRepositoryImpl @Inject constructor(
    private val segmentDao: SegmentDao,
) : SegmentRepository {
    override fun observeSegments(): Flow<List<Segment>> =
        segmentDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveSegments(segments: List<Segment>): List<Long> =
        segmentDao.insertIfNew(segments.map { it.toEntity() }).filter { it != -1L }
}
