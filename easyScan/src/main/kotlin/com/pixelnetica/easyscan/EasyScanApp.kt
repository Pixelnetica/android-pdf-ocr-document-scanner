package com.pixelnetica.easyscan

import android.app.Application
import com.pixelnetica.scanning.ScanningSdkLibrary
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EasyScanApp: Application() {
    override fun onCreate() {
        super.onCreate()

        // Setup DocImageSDK
        ScanningSdkLibrary.load(this)
    }
}