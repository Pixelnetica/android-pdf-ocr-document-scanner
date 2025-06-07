package com.pixelnetica.easyscan.data.di

import android.content.Context
import com.pixelnetica.design.lang.LanguageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class LanguageRepositoryModule {
    @Provides
    @Singleton
    fun provideLanguageRepository(@ApplicationContext appContext: Context) =
        LanguageManager.getInstance(appContext)
}