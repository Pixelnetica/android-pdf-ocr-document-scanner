package com.pixelnetica.easyscan.ui.textscreen

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pixelnetica.design.read.ConfirmRestoreScope
import com.pixelnetica.design.read.RecognizedStatus
import com.pixelnetica.design.read.RecognizedText
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.ui.TestTags
import com.pixelnetica.scanning.ScanReader
import com.pixelnetica.scanning.ScanText
import com.pixelnetica.support.Tag
import com.pixelnetica.support.WaitingOverlay

private fun ScanText?.display() =
    this?.toString()?.lines()?.firstOrNull() ?: "<EMPTY>"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextScreen(
    navController: NavController,
    viewModel: TextScreenViewModel = hiltViewModel(),
    ) {

    val languagesTitle by viewModel.languagesTitle.collectAsStateWithLifecycle("")
    val languagesInvoke: () -> Unit = {
        navController.navigate("languagesScreen")
    }

    val hasText by viewModel.hasText.collectAsStateWithLifecycle(false)

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(id = R.string.title_activity_read))
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag(TestTags.NAV_BACK),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    // Show languages or button
                    if (languagesTitle.isEmpty()) {
                        IconButton(onClick = languagesInvoke) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_language),
                                contentDescription = stringResource(id = R.string.btn_read_languages),
                            )
                        }
                    } else {
                        TextButton(
                            onClick = languagesInvoke,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = LocalContentColor.current
                            )
                        ) {
                            Text(languagesTitle)
                        }
                    }

                    // Refresh
                    if (languagesTitle.isNotEmpty()) {
                        IconButton(onClick = { viewModel.refresh(true) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_refresh),
                                contentDescription = stringResource(id = R.string.btn_read_update),
                            )
                        }
                    }

                    // Delete recognition results
                    if (hasText) {
                        IconButton(onClick = { viewModel.delete() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_trash_bin),
                                contentDescription = stringResource(id = R.string.btn_read_delete)
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }

    ) { contentPadding ->

        // Show a confirmation when the language set was changed
        val inconsistentLanguages by viewModel.inconsistentLanguages.collectAsStateWithLifecycle(null)
        val differentLanguages = inconsistentLanguages
        if (differentLanguages != null) {
            var differentLanguagesHandled by remember(differentLanguages) {
                mutableStateOf(false)
            }

            if (!differentLanguagesHandled) {
                // Build a language string
                val prompt = stringResource(
                    id = R.string.msg_read_different_languages,
                    ScanReader.parseLanguages(differentLanguages.textLanguages).split("+").joinToString(),
                )
                val action = stringResource(id = R.string.action_read_update)

                LaunchedEffect(differentLanguages) {
                    val result = snackbarHostState.showSnackbar(
                        message = prompt,
                        actionLabel = action,
                        withDismissAction = true,
                        duration = SnackbarDuration.Indefinite,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.refresh(true)
                    }

                    // Cleanup current request
                    differentLanguagesHandled = true
                }
            }
        }

        // Show snackbar on text restore
        var confirmation by remember {
            mutableStateOf<Pair<ConfirmRestoreScope, List<CharSequence>>?>(null)
        }

        confirmation?.let {

            // Show a confirmation on restore word(s)
            val action = stringResource(id = android.R.string.ok)
            val prompt = stringResource(
                id = R.string.msg_read_undo_text,
                it.second.joinToString(" ", limit = 2)
            )
            LaunchedEffect(it) {
                Logger.log.d("Try to show shackbar!")
                val result = snackbarHostState.showSnackbar(
                    message = prompt,
                    actionLabel = action,
                    withDismissAction = false,
                    duration = SnackbarDuration.Long
                )
                Logger.log.d("Snackbar returns $result.")
                if (result == SnackbarResult.ActionPerformed) {
                    Logger.log.d("Snackbar action performed.")
                    it.first.restore()
                }
                confirmation = null
            }
        }


        // Query viewmodel
        val picture by viewModel.imagePicture.collectAsStateWithLifecycle(null)
        val lookupRect by viewModel.lookupRect.collectAsStateWithLifecycle(null)
        val lookupProgress by viewModel.lookupProgress.collectAsStateWithLifecycle(-1)
        val originalText by viewModel.originalText.collectAsStateWithLifecycle(ScanText())
        val modifiedText by viewModel.modifiedText.collectAsStateWithLifecycle(ScanText())

        // Main view
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
        ) {
            val (progressStatus, setProgressStatus) = remember {
                mutableStateOf<RecognizedStatus>(RecognizedStatus.None)
            }

            RecognizedText(
                modifier = Modifier
                    .fillMaxSize(),
                picture = picture,
                lookupRect = lookupRect?.toComposeRect(),
                lookupProgress = lookupProgress,
                originalText = originalText,
                modifiedText = modifiedText,
                onCancel = {
                    viewModel.cancel()
                },
                onConfirmRestore = { items ->
                    confirmation = Pair(this, items)
                },
                onModifiedTextChanged = viewModel::onModifiedTextChanged,
                onProgress = setProgressStatus,
            ) { modifier ->
                Text(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .then(modifier),
                    text = stringResource(R.string.read_empty_text),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            val imageUnavailable by viewModel.imageUnavailable.collectAsStateWithLifecycle(false)
            if (imageUnavailable) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    text = stringResource(R.string.page_unavailable_message),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                // Show Progress overlay
                WaitingOverlay(progressStatus)
            }
        }

        // Show or hide keyboard when recognize done
        val localView = LocalView.current
        val context = LocalContext.current
        val imm = remember {
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        }
        val hideKeyboard by remember {
            derivedStateOf {
                lookupRect != null
            }
        }

        SideEffect {
            if (hideKeyboard) {
                imm.hideSoftInputFromWindow(localView.windowToken, 0)
            }
        }

        // Save modified text on exit
        DisposableEffect(viewModel) {
            onDispose {
                imm.hideSoftInputFromWindow(localView.windowToken, 0)
                viewModel.saveChanges()
            }
        }
    }
}

private object Logger: Tag by AppTagger("TextScreen")