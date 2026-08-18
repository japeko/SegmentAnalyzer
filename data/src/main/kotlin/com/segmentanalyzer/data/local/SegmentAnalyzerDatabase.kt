package com.segmentanalyzer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.segmentanalyzer.data.local.converter.RideTypeConverters
import com.segmentanalyzer.data.local.dao.RideDao
import com.segmentanalyzer.data.local.entity.RideEntity

@Database(entities = [RideEntity::class], version = 1, exportSchema = true)
@TypeConverters(RideTypeConverters::class)
abstract class SegmentAnalyzerDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao

    companion object {
        const val DATABASE_NAME = "segment_analyzer.db"
    }
}
