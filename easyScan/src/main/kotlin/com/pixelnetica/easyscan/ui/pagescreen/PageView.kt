package com.pixelnetica.easyscan.ui.pagescreen

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelnetica.easyscan.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelnetica.design.view.ViewPicture
import com.pixelnetica.support.WaitingOverlay

@Composable
fun PageView(
    modifier: Modifier,
    pagerOrientation: Orientation,
    pageContent: PageSliderViewModel.PageContent,
) = Box(modifier = Modifier.fillMaxSize()) {
    val pageViewPicture by pageContent.pagePicture.collectAsStateWithLifecycle()

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

    // A page reached by swiping can also have lost its image file. Nothing
    // will ever load here, so say so rather than wait forever - the same
    // reason the list refuses to open such a page at all.
    if (pageViewPicture.isUnavailable) {
        Text(
            text = stringResource(R.string.page_unavailable_message),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    } else if (!isPictureReady || !pageViewPicture.isComplete) {
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