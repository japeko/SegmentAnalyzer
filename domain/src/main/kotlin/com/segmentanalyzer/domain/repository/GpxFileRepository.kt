package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Ride

/** Parses a locally-picked GPX file into a ride, ready to be saved. */
interface GpxFileRepository {
    /** [uri] is the content URI string of the file the user picked. */
    suspend fun parseGpxFile(uri: String): Result<Ride>
}
