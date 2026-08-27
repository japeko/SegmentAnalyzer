package com.segmentanalyzer.data.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.segmentanalyzer.data.local.dao.SegmentAttemptDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * Self-heals real GPS-matched attempts left over from before Strava data existed for the same
 * (segmentId, rideId) — see [SegmentAttemptDao.deleteRedundantLocalAttempts]. Runs on every open
 * (not just [RoomDatabase.Callback.onCreate]) since the bad rows could already be on disk from
 * before this cleanup existed.
 */
class SegmentAttemptCleanupCallback @Inject constructor(
    private val segmentAttemptDaoProvider: Provider<SegmentAttemptDao>,
    private val applicationScope: CoroutineScope,
) : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        applicationScope.launch(Dispatchers.IO) {
            segmentAttemptDaoProvider.get().deleteRedundantLocalAttempts()
        }
    }
}
