package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.GuestAttempt
import com.segmentanalyzer.domain.model.TrackPoint
import kotlinx.coroutines.flow.Flow

interface GuestAttemptRepository {
    /**
     * Parses [uri]'s FIT file and matches its track against every known segment, saving one
     * [GuestAttempt] per pass found — a whole-ride file can match several segments, or the same
     * one more than once for a repeated lap. Attributed to [riderName]. Fails if the file has no
     * GPS track, or matches no segment at all.
     */
    suspend fun importFitFile(uri: String, riderName: String): Result<List<GuestAttempt>>

    fun observeForSegment(segmentId: Long): Flow<List<GuestAttempt>>

    /** The matched entry..exit sub-track only — not the friend's whole ride. */
    suspend fun trackPointsForGuestAttempt(guestAttemptId: Long): List<TrackPoint>

    suspend fun deleteGuestAttempt(id: Long)
}
