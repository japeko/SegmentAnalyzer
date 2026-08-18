package com.segmentanalyzer.data.di

import android.content.Context
import androidx.room.Room
import com.segmentanalyzer.data.local.SegmentAnalyzerDatabase
import com.segmentanalyzer.data.local.dao.RideDao
import com.segmentanalyzer.data.seed.RideSeedCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        rideDaoProvider: Provider<RideDao>,
        seedCallbackFactory: Provider<RideSeedCallback>,
    ): SegmentAnalyzerDatabase =
        Room.databaseBuilder(
            context,
            SegmentAnalyzerDatabase::class.java,
            SegmentAnalyzerDatabase.DATABASE_NAME,
        )
            .addCallback(seedCallbackFactory.get())
            .build()

    @Provides
    fun provideRideDao(database: SegmentAnalyzerDatabase): RideDao = database.rideDao()
}
