package com.pixelnetica.easyscan.data.di

import com.pixelnetica.easyscan.data.DefaultEasyScanRepository
import com.pixelnetica.easyscan.data.EasyScanRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds
    @Singleton
    fun bindEasyScanRepository(easyScanRepository: DefaultEasyScanRepository):EasyScanRepository
}