package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Ride

/** Parses a locally-picked FIT file into a ride, ready to be saved. */
interface FitFileRepository {
    /** [uri] is the content URI string of the file the user picked. */
    suspend fun parseFitFile(uri: String): Result<Ride>
}
