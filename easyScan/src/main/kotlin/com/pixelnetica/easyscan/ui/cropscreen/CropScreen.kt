package com.pixelnetica.easyscan.ui.cropscreen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pixelnetica.design.crop.CropPicture
import com.pixelnetica.easyscan.R

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
    val canExpand by viewModel.canExpandCutout.collectAsStateWithLifecycle(false)
    val canRevert by viewModel.canRevertCutout.collectAsStateWithLifecycle(false)

    // Save a modified cutout on an exit
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.acceptCutout()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.title_activity_crop))
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
        val image by viewModel.imagePicture.collectAsStateWithLifecycle(null)
        val orientation by viewModel.imageOrientation.collectAsStateWithLifecycle(null)
        val cutout by viewModel.imageCutout.collectAsStateWithLifecycle()
        CropPicture(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            picture = image,
            orientation = orientation,
            cutout = cutout,
            onPictureReady = setPictureReady,
            onCutoutChanged = viewModel::changeCutout
        )
    }
}