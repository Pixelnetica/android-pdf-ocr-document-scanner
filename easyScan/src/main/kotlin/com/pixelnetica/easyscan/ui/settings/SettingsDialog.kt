package com.pixelnetica.easyscan.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pixelnetica.composability.drawableResource
import com.pixelnetica.easyscan.BuildConfig
import com.pixelnetica.easyscan.EasyScanSettings
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.appSettingsDataStore
import com.pixelnetica.easyscan.ui.main.NavDialog
import com.pixelnetica.composability.handleUrlLinks
import com.pixelnetica.composability.htmlToAnnotatedString
import com.pixelnetica.composability.visibility
import com.pixelnetica.support.Spinner
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    navController: NavController,
    viewModel: SettingsDialogViewModel = hiltViewModel()
) = NavDialog(
        navController = navController,
        image = {
            Image(
                painter = drawableResource(id = R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterVertically),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        },
        title = stringResource(id = R.string.action_settings),
    ) {
    // Application theme selector
    val context = LocalContext.current

    val appSettings by context.appSettingsDataStore.data.collectAsStateWithLifecycle(
        EasyScanSettings.AppSettingsSerializer.defaultValue
    )

    val coroutineScope = rememberCoroutineScope()

    // About message

    // Build message once!
    val textColor = LocalContentColor.current
    val accentColor = MaterialTheme.colorScheme.primaryContainer
    val annotatedMessage = remember(textColor, accentColor) {
        val packageName = context.applicationContext.packageName
        val packageInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo(
                    packageName, 0
                )
            }
        }
        val versionName = packageInfo.getOrNull()?.versionName.orEmpty()

        context.getString(R.string.about_message).htmlToAnnotatedString(
            defaultStyle = SpanStyle(
                color = textColor,
            ),
            urlStyle = SpanStyle(
                color = accentColor,
                textDecoration = TextDecoration.Underline
            ),
            packageName,
            versionName,
            BuildConfig.GIT_HASH,
        )
    }

    val uriHandler = LocalUriHandler.current
    ClickableText(
        text = annotatedMessage,
        modifier = Modifier.padding(ButtonDefaults.IconSpacing),
        style = MaterialTheme.typography.bodySmall,
    ) { position ->
        annotatedMessage.handleUrlLinks(uriHandler, position)
    }

    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

    // Theme
    Spinner(
        label = stringResource(id = R.string.settings_change_app_theme),
        options = stringArrayResource(id = R.array.settings_app_themes),
        index = appSettings.appThemeValue,
    ) { index ->
        coroutineScope.launch {
            context.appSettingsDataStore.updateData {
                appSettings
                    .toBuilder()
                    .setAppThemeValue(index)
                    .build()
            }
        }
    }

    // Dynamic colors (Android 14+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = appSettings.dynamicColors,
                onCheckedChange = { dynamicColors ->
                    coroutineScope.launch {
                        context.appSettingsDataStore.updateData {
                            appSettings
                                .toBuilder()
                                .setDynamicColors(dynamicColors)
                                .build()
                        }
                    }
                },
            )
            Text(
                text = stringResource(id = R.string.settings_dynamic_colors),
                modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
            )
        }
    }

    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

    // Auto detect orientation
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = appSettings.autoDetectOrientation,
            onCheckedChange = { autoDetectOrientation ->
                coroutineScope.launch {
                    context.appSettingsDataStore.updateData {
                        appSettings
                            .toBuilder()
                            .setAutoDetectOrientation(autoDetectOrientation)
                            .build()
                    }
                }
            })
        Text(
            text = stringResource(id = R.string.settings_auto_detect_orientation),
            modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
        )
    }

    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

    // Show a PDF hidden text
    if (BuildConfig.DEBUG) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = appSettings.showPdfHiddenText,
                onCheckedChange = { showPdfHiddenText ->
                    coroutineScope.launch {
                        context.appSettingsDataStore.updateData {
                            appSettings
                                .toBuilder()
                                .setShowPdfHiddenText(showPdfHiddenText)
                                .build()
                        }
                    }
                })
            Text(
                text = stringResource(id = R.string.settings_show_pdf_hidden_text),
                modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
            )
        }

        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
    }

    // PDF images compression
    Spinner(
        label = stringResource(id = R.string.settings_pdf_image_compression),
        options = stringArrayResource(id = R.array.settings_pdf_image_compression_levels),
        index = appSettings.imageCompressionValue) {index ->
            coroutineScope.launch {
                context.appSettingsDataStore.updateData {
                    appSettings
                        .toBuilder()
                        .setImageCompressionValue(index)
                        .build()
                }
            }
    }

    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

    // Languages
    val languages by viewModel.languages.collectAsStateWithLifecycle("")
    Button(onClick = {
        navController.navigate("languagesScreen")
    }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_language),
            contentDescription = null,
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(
            text = stringResource(id = R.string.btn_read_languages) + languages.let {
                if (it.isNotEmpty()) {
                    ": $it"
                } else {
                    ""
                }
            },
        )
    }

    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
}