package com.pixelnetica.easyscan.ui.pagescreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.ui.viewitem.PageViewPicture
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelnetica.design.view.ViewPicture
import com.pixelnetica.design.view.scrollPadding
import com.pixelnetica.design.view.zoomFactor
import com.pixelnetica.support.WaitingOverlay

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PageView(
    modifier: Modifier,
    pageContent: PageSliderViewModel.PageContent,
) {
    val pageViewPicture by pageContent.pagePicture.collectAsStateWithLifecycle(PageViewPicture.Empty)

    Box(modifier = modifier) {
        ViewPicture(
            modifier = Modifier
                .fillMaxSize()
                .scrollPadding(16.dp)
                .zoomFactor(2.0f),
            pageViewPicture.picture,
            pageViewPicture.orientation,
            !pageViewPicture.isComplete,
        )

        // Shadow over content
        if (!pageViewPicture.isComplete) {
            WaitingOverlay(text = stringResource(id = R.string.page_view_progress_label))
        }
    }
}