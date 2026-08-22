package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Segment
import kotlinx.coroutines.flow.Flow

/** Read/write access to the locally stored segments. All segments live on-device. */
interface SegmentRepository {
    /** All segments, most recently synced first. */
    fun observeSegments(): Flow<List<Segment>>

    /** Saves synced segments, skipping ones already present. Returns the ids of newly inserted segments. */
    suspend fun saveSegments(segments: List<Segment>): List<Long>

    /**
     * Segments with at least one attempt whose ride matches [tag] and whose attempt's own start
     * time falls within [afterEpochMillis]..[beforeEpochMillis) — any null bound is unfiltered on
     * that axis. A segment with no matching attempt (including one with none at all) is excluded.
     */
    fun observeFilteredSegments(tag: String?, afterEpochMillis: Long?, beforeEpochMillis: Long?): Flow<List<Segment>>
}
