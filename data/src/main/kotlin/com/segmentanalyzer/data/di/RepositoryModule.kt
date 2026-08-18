package com.segmentanalyzer.data.di

import com.segmentanalyzer.data.repository.GarminAccountRepositoryImpl
import com.segmentanalyzer.data.repository.GarminImportRepositoryImpl
import com.segmentanalyzer.data.repository.RideRepositoryImpl
import com.segmentanalyzer.domain.repository.GarminAccountRepository
import com.segmentanalyzer.domain.repository.GarminImportRepository
import com.segmentanalyzer.domain.repository.RideRepository
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
    internal abstract fun bindGarminAccountRepository(impl: GarminAccountRepositoryImpl): GarminAccountRepository

    @Binds
    @Singleton
    internal abstract fun bindGarminImportRepository(impl: GarminImportRepositoryImpl): GarminImportRepository
}
