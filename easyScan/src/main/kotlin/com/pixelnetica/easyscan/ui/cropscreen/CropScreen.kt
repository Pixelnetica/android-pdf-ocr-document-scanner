package com.pixelnetica.easyscan.ui.cropscreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pixelnetica.design.crop.CropPicture
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.ui.TestTags
import com.pixelnetica.support.WaitingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    navController: NavController,
    viewModel: CropScreenViewModel = hiltViewModel()
    ) {

    // Enabled state
    val (isPictureReady, setPictureReady) = remember {
        mutableStateOf(false)
    }

    // Query the viewmodel
    val canExpand by viewModel.canExpandCutout.collectAsStateWithLifecycle()
    val canRevert by viewModel.canRevertCutout.collectAsStateWithLifecycle()

    // Save the edit on the way out.
    //
    // This screen has no Save button: whatever the user changes here is kept
    // when they leave. Leaving composition is the one signal that reliably
    // marks that moment, so the save is anchored to it.
    //
    // The obvious alternative - watching the navigation back stack for the
    // route changing away from this screen - is not reliable, and the way it
    // fails is worth knowing if you are writing a screen like this. Reading
    // the current entry only tells the screen it is leaving on a *later*
    // recomposition, and once the destination is popped its entry is destroyed
    // and its ViewModels cleared with no promise that this composable body
    // runs one more time first. A rotation made just before the back press can
    // therefore be dropped without a trace. onDispose carries no such
    // condition: it runs exactly once, whenever this screen actually goes away.
    //
    // The save also runs on a configuration change, which disposes and
    // recomposes the screen. That is harmless here precisely because there is
    // no cancel affordance - the edit was going to be saved on leave anyway,
    // so persisting it a moment earlier costs nothing. Note that no effect of
    // this kind survives the process being killed outright; a draft that must
    // outlive that has to be written to saved state instead.
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.acceptChanges()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.title_activity_crop))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.testTag(TestTags.NAV_BACK),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Spacer(modifier = Modifier.weight(0.5F, true))

                IconButton(
                    onClick = {
                        viewModel.rotatePage(false)
                    },
                    enabled = isPictureReady,
                    modifier = Modifier.testTag(TestTags.CROP_ROTATE_CCW),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_rotate_ccw),
                        contentDescription = null,
                    )
                }

                Spacer(modifier = Modifier.weight(1.0F, true))

                IconButton(
                    onClick = {
                        viewModel.rotatePage(true)
                    },
                    enabled = isPictureReady,
                    modifier = Modifier.testTag(TestTags.CROP_ROTATE_CW),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_rotate_cw),
                        contentDescription = null,
                    )
                }

                Spacer(modifier = Modifier.weight(1.0F, true))

                if (canExpand) {
                    IconButton(
                        onClick = {
                            viewModel.revertCutout(true)
                        },
                        enabled = isPictureReady,
                        modifier = Modifier.testTag(TestTags.CROP_EXPAND),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_expand),
                            contentDescription = null
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            viewModel.revertCutout(false)
                        },
                        enabled = isPictureReady && canRevert,
                        modifier = Modifier.testTag(TestTags.CROP_REVERT),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_collapse),
                            contentDescription = null
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(0.5F, true))
            }
        }
    )
    { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)) {

            val image by viewModel.imagePicture.collectAsStateWithLifecycle()
            val orientation by viewModel.imageOrientation.collectAsStateWithLifecycle()
            val cutout by viewModel.imageCutout.collectAsStateWithLifecycle()
            CropPicture(
                modifier = Modifier
                    .fillMaxSize(),
                picture = image,
                orientation = orientation,
                cutout = cutout,
                onPictureReady = setPictureReady,
                onCutoutChanged = viewModel::changeCutout,
            )

            val imageUnavailable by viewModel.imageUnavailable.collectAsStateWithLifecycle()
            if (imageUnavailable) {
                Text(
                    text = stringResource(R.string.page_unavailable_message),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else if (!isPictureReady) {
                WaitingOverlay(stringResource(R.string.loading))
            }
        }
    }
}

@Suppress("unused")
private object CropScreenLogger : AppTagger("CropScreen")