package com.pixelnetica.easyscan.ui.main

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelnetica.easyscan.AppSettings
import com.pixelnetica.easyscan.EasyScanSettings
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.appSettingsDataStore
import com.pixelnetica.easyscan.data.EasyScanRepository
import com.pixelnetica.easyscan.data.ShareResult
import com.pixelnetica.easyscan.ui.theme.EasyScanTheme
import com.pixelnetica.support.collectOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var repository: EasyScanRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Define application settings
            val initialSettings = remember {
                // Load initial settings in blocking to avoid screen blinking
                runBlocking {
                    appSettingsDataStore.data.catch {
                        emit(EasyScanSettings.AppSettingsSerializer.defaultValue)
                    }.first()
                }
            }
            val appSettings by appSettingsDataStore.data.collectAsStateWithLifecycle(initialSettings)
            val darkTheme = when (appSettings.appTheme) {
                AppSettings.AppTheme.LIGHT -> false
                AppSettings.AppTheme.DARK -> true
                else -> isSystemInDarkTheme()
            }
            val dynamicColors = appSettings.dynamicColors

            EasyScanTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }

        // Lookup for share
        repository.queryShareResult().collectOnLifecycle(this, action = ::shareFiles)
    }

    private fun shareFiles(share: ShareResult) {
        val (mimeType, files) = share

        // See Manifest
        val authority = "$packageName.share.fileprovider"
        val uriList = files.map { FileProvider.getUriForFile(this, authority, it) }

        // Prepare intent
        val shareIntent = when {
            uriList.size == 1 -> Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_STREAM, uriList.first())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            uriList.size > 1 -> Intent(Intent.ACTION_SEND_MULTIPLE)
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Parcelable>(uriList))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            else -> return
        }

        shareIntent.type = mimeType

        // Get selected application
        val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share_pages_title))

        // Why don't working?
        chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Workaround
        val resolveList =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                packageManager.queryIntentActivities(
                    chooserIntent,
                    PackageManager.MATCH_DEFAULT_ONLY,
                )
            else
                packageManager.queryIntentActivities(
                    chooserIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
                )

        for (resolve in resolveList) {
            val packageName = resolve.activityInfo.packageName
            for (shareUri in uriList) {
                grantUriPermission(
                    packageName,
                    shareUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        startActivity(chooserIntent)
    }
}
