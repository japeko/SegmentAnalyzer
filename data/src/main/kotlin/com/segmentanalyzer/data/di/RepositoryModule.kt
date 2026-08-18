package com.segmentanalyzer.data.di

import com.segmentanalyzer.data.repository.GarminAccountRepositoryImpl
import com.segmentanalyzer.data.repository.GarminImportRepositoryImpl
import com.segmentanalyzer.data.repository.RideRepositoryImpl
import com.segmentanalyzer.data.repository.SegmentRepositoryImpl
import com.segmentanalyzer.data.repository.StravaAccountRepositoryImpl
import com.segmentanalyzer.data.repository.StravaSegmentRepositoryImpl
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import com.segmentanalyzer.domain.repository.GarminImportRepository
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.repository.SegmentRepository
import com.segmentanalyzer.domain.repository.StravaAccountRepository
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
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
}
