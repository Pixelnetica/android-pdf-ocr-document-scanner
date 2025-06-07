package com.pixelnetica.easyscan.ui.theme

import android.app.Activity
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.pixelnetica.scanning.ScanningSdkLibrary
import com.pixelnetica.widget.enableDynamicColors

private val DarkColorScheme = darkColorScheme(
    primary = pxPrimaryDarkColor,
    onPrimary = pxPrimaryLightColor,
    primaryContainer = pxSecondaryColor,
    onPrimaryContainer = pxSecondaryLightColor,
)

private val LightColorScheme = lightColorScheme(
    primary = pxPrimaryColor,
    onPrimary = pxPrimaryTextColor,
    primaryContainer = pxSecondaryDarkColor,
    onPrimaryContainer = pxSecondaryTextColor,

    background = pxBackground,
    onBackground = pxSurfaceTextColor,

    surface = pxSurface,
    onSurface = pxSurfaceTextColor,
    surfaceVariant = pxSurface2,
    onSurfaceVariant = pxSurfaceTextColor,

    outlineVariant = pxPrimaryDarkColor,   // divider

    //    inversePrimary = pxUndefined,
//    secondary = pxUndefined,
//    onSecondary = pxError,
//    secondaryContainer = pxUndefined,
//    onSecondaryContainer = pxError,
//    tertiary = pxUndefined,
//    onTertiary = pxError,
//    tertiaryContainer = pxUndefined,
//    onTertiaryContainer = pxError,
//    surfaceTint = pxUndefined,
//    inverseSurface = pxUndefined,
//    inverseOnSurface = pxError,
//    error = pxUndefined,
//    onError = pxError,
//    errorContainer = pxUndefined,
//    onErrorContainer = pxError,
//    outline = pxUndefined,
//    scrim = pxError,
)

@Composable
fun EasyScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme)
                dynamicDarkColorScheme(context)
            else
                dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        /* getting the current window by tapping into the Activity */
        val currentWindow = (view.context as? Activity)?.window
            ?: throw Exception("Not in an activity - unable to get Window reference")

        SideEffect {
            (view.context as Activity).window.statusBarColor = colorScheme.surface.toArgb()
            (view.context as Activity).window.navigationBarColor = colorScheme.surfaceColorAtElevation(3.dp).toArgb()
            //background.toArgb()
            WindowCompat.getInsetsController(currentWindow, view).isAppearanceLightStatusBars =
                !darkTheme
            WindowCompat.getInsetsController(currentWindow, view).isAppearanceLightNavigationBars =
                !darkTheme

            // Apply dynamic colors to ScanningSDK before changing night mode
            ScanningSdkLibrary.enableDynamicColors(dynamicColor)

            // Apply dark theme to XML layouts
            AppCompatDelegate.setDefaultNightMode(
                if (darkTheme) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}