package com.pixelnetica.easyscan

import android.app.Application
import com.pixelnetica.scanning.ScanningSdkLibrary

/**
 * Application base class carrying the SDK bootstrap: the scanning library must
 * be loaded before any component touches the SDK, in every application variant.
 */
open class EasyScanBaseApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Setup DocImageSDK
        ScanningSdkLibrary.load(this)
    }
}
