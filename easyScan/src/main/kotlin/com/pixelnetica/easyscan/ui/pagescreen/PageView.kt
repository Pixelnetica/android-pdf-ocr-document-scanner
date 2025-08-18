package com.pixelnetica.easyscan.ui.pagescreen

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.ui.viewitem.PageViewPicture
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelnetica.design.view.ViewPicture
import com.pixelnetica.support.WaitingOverlay

@Composable
fun PageView(
    modifier: Modifier,
    pagerOrientation: Orientation,
    pageContent: PageSliderViewModel.PageContent,
) = Box(modifier = Modifier.fillMaxSize()) {
    val pageViewPicture by pageContent.pagePicture.collectAsStateWithLifecycle(PageViewPicture.Empty)

    val (isPictureReady, setPictureReady) = remember {
        mutableStateOf(false)
    }

    ViewPicture(
        modifier = modifier,
        picture = pageViewPicture.picture,
        imageOrientation = pageViewPicture.orientation,
        scaleToFit = !pageViewPicture.isComplete,
        pagerOrientation = pagerOrientation,
        onPictureReady = setPictureReady,
    )

    // Shadow over content
    if (!isPictureReady || !pageViewPicture.isComplete) {
        val delay =
            if (pageViewPicture.isComplete) {
                // Don't show the waiting overlay on orientation changes.
                1000L
            } else {
                0L
            }

        WaitingOverlay(
            text = stringResource(R.string.loading),
            delayMillis = delay,
        )
    }
}