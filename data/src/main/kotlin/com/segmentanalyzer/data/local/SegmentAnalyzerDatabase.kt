package com.segmentanalyzer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.segmentanalyzer.data.local.converter.RideTypeConverters
import com.segmentanalyzer.data.local.dao.RideDao
import com.segmentanalyzer.data.local.dao.SegmentDao
import com.segmentanalyzer.data.local.entity.RideEntity
import com.segmentanalyzer.data.local.entity.SegmentEntity

@Database(entities = [RideEntity::class, SegmentEntity::class], version = 3, exportSchema = true)
@TypeConverters(RideTypeConverters::class)
abstract class SegmentAnalyzerDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao
    abstract fun segmentDao(): SegmentDao

    companion object {
        const val DATABASE_NAME = "segment_analyzer.db"
    }
}
