package com.pixelnetica.easyscan.data.di

import android.content.Context
import androidx.room.Room
import com.pixelnetica.easyscan.data.EasyScanDao
import com.pixelnetica.easyscan.data.EasyScanDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    fun provideEasyScanDao(db: EasyScanDatabase): EasyScanDao = db.easyScanDao()

    @Provides
    @Singleton
    fun provideEasyScanDatabase(@ApplicationContext appContext: Context): EasyScanDatabase =
        Room
            .databaseBuilder(
                appContext,
                EasyScanDatabase::class.java,
                "easyscan.db",
                    )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
}