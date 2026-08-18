package com.segmentanalyzer.data.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.segmentanalyzer.data.local.dao.RideDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * Populates the database with sample rides the first time it's created.
 *
 * [RoomDatabase.Callback.onCreate] fires exactly once, right when the SQLite file itself is
 * created, so there's no separate "have I seeded" flag that could drift out of sync with the
 * table's actual contents. The DAO is requested lazily via [Provider] because the callback runs
 * while the database is still being built — asking for it eagerly here would be circular.
 */
class RideSeedCallback @Inject constructor(
    private val rideDaoProvider: Provider<RideDao>,
    private val applicationScope: CoroutineScope,
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        applicationScope.launch(Dispatchers.IO) {
            rideDaoProvider.get().insertAll(SeedRides.entities)
        }
    }
}
