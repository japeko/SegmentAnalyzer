package com.segmentanalyzer.data.di

import com.segmentanalyzer.data.repository.FitFileRepositoryImpl
import com.segmentanalyzer.data.repository.GarminAccountRepositoryImpl
import com.segmentanalyzer.data.repository.GarminImportRepositoryImpl
import com.segmentanalyzer.data.repository.GpxFileRepositoryImpl
import com.segmentanalyzer.data.repository.RideRepositoryImpl
import com.segmentanalyzer.data.repository.SegmentAttemptRepositoryImpl
import com.segmentanalyzer.data.repository.SegmentRepositoryImpl
import com.segmentanalyzer.data.repository.SettingsRepositoryImpl
import com.segmentanalyzer.data.repository.StravaAccountRepositoryImpl
import com.segmentanalyzer.data.repository.StravaActivityRepositoryImpl
import com.segmentanalyzer.data.repository.StravaSegmentEffortRepositoryImpl
import com.segmentanalyzer.data.repository.StravaSegmentRepositoryImpl
import com.segmentanalyzer.data.repository.ViewedRidesRepositoryImpl
import com.segmentanalyzer.domain.repository.FitFileRepository
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import com.segmentanalyzer.domain.repository.GarminImportRepository
import com.segmentanalyzer.domain.repository.GpxFileRepository
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.SegmentRepository
import com.segmentanalyzer.domain.repository.SettingsRepository
import com.segmentanalyzer.domain.repository.StravaAccountRepository
import com.segmentanalyzer.domain.repository.StravaActivityRepository
import com.segmentanalyzer.domain.repository.StravaSegmentEffortRepository
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import com.segmentanalyzer.domain.repository.ViewedRidesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRideRepository(impl: RideRepositoryImpl): RideRepository

    @Binds
    @Singleton
    abstract fun bindSegmentRepository(impl: SegmentRepositoryImpl): SegmentRepository

    @Binds
    @Singleton
    internal abstract fun bindGarminAccountRepository(impl: GarminAccountRepositoryImpl): GarminAccountRepository

    @Binds
    @Singleton
    internal abstract fun bindGarminImportRepository(impl: GarminImportRepositoryImpl): GarminImportRepository

    @Binds
    @Singleton
    internal abstract fun bindStravaAccountRepository(impl: StravaAccountRepositoryImpl): StravaAccountRepository

    @Binds
    @Singleton
    internal abstract fun bindStravaSegmentRepository(impl: StravaSegmentRepositoryImpl): StravaSegmentRepository

    @Binds
    @Singleton
    internal abstract fun bindStravaActivityRepository(impl: StravaActivityRepositoryImpl): StravaActivityRepository

    @Binds
    @Singleton
    internal abstract fun bindStravaSegmentEffortRepository(
        impl: StravaSegmentEffortRepositoryImpl,
    ): StravaSegmentEffortRepository

    @Binds
    @Singleton
    internal abstract fun bindFitFileRepository(impl: FitFileRepositoryImpl): FitFileRepository

    @Binds
    @Singleton
    internal abstract fun bindGpxFileRepository(impl: GpxFileRepositoryImpl): GpxFileRepository

    @Binds
    @Singleton
    internal abstract fun bindSegmentAttemptRepository(impl: SegmentAttemptRepositoryImpl): SegmentAttemptRepository

    @Binds
    @Singleton
    internal abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    internal abstract fun bindViewedRidesRepository(impl: ViewedRidesRepositoryImpl): ViewedRidesRepository
}
