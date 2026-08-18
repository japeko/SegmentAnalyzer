package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Segment
import kotlinx.coroutines.flow.Flow

/** Read/write access to the locally stored segments. All segments live on-device. */
interface SegmentRepository {
    /** All segments, most recently synced first. */
    fun observeSegments(): Flow<List<Segment>>

    /** Saves synced segments, skipping ones already present. Returns the ids of newly inserted segments. */
    suspend fun saveSegments(segments: List<Segment>): List<Long>
}
