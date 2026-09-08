package com.pixelnetica.easyscan.ui.sharedialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.ui.TestTags
import com.pixelnetica.easyscan.ui.main.NavDialog

@Composable
fun ShareDialog(
    navController: NavController,
    viewModel: ShareViewModel = hiltViewModel()
) = NavDialog(
        navController = navController,
        title = if (viewModel.singlePage) {
            stringResource(id = R.string.share_page_title)
        } else {
            stringResource(id = R.string.share_pages_title)
        }
    ) {
    val previews by viewModel.previews.collectAsStateWithLifecycle(emptyList())

    // Preview strip. One cell per shared page from the first frame, filled in
    // as the previews load: the count comes from the view model, never from
    // `previews`. Sizing a lazy list from state that arrives asynchronously
    // looks harmless and is not — the list can be measured against a count
    // that grew after it had already worked out what its items are, and it
    // crashes asking for an item that is not there yet. Reading `previews`
    // per cell below is fine, because that read belongs to the cell's own
    // content rather than to the list's decision about how many cells exist;
    // the count is the part that must not move.
    LazyRow(
        modifier = Modifier
            .height(188.dp)
            .align(Alignment.CenterHorizontally)
    ) {
        items(
            count = viewModel.pageCount,
        ) { index ->
            // Tagged on both branches so the strip reports the same number of
            // cells whether or not its previews have arrived.
            val cellModifier = Modifier.testTag(TestTags.SHARE_PREVIEW_CELL)
            val previewImage = previews.getOrNull(index)?.asImageBitmap()
            if (previewImage != null) {
                com.pixelnetica.composable.PreviewBox(
                    bitmap = previewImage,
                    modifier = cellModifier,
                )
            } else {
                com.pixelnetica.composable.PreviewBox(
                    painter = painterResource(id = R.drawable.document_photo_icon),
                    modifier = cellModifier,
                )
            }
        }
    }

    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

    val hasUnavailablePages by viewModel.hasUnavailablePages.collectAsStateWithLifecycle(false)
    val hasShareSessions by viewModel.hasShareSessions.collectAsStateWithLifecycle(false)
    if (hasUnavailablePages) {
        // Exporting would have to leave the page out, and a document quietly
        // missing a page is worse than being told why it cannot be made.
        Text(
            text = stringResource(id = R.string.share_unavailable_pages),
            modifier = Modifier.testTag(TestTags.SHARE_UNAVAILABLE),
            style = MaterialTheme.typography.bodyMedium,
        )
    } else if (hasShareSessions) {
        // Show progress indicator
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.primaryContainer,
            trackColor = MaterialTheme.colorScheme.background.copy(alpha = 0.4F),
        )

        Spacer(Modifier.size(ButtonDefaults.IconSpacing))

        Text(
            text = stringResource(id = R.string.loading),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        // PNG
        Button(
            onClick = {
                viewModel.sharePages(ShareViewModel.ShareType.PNG)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.SHARE_AS_IMAGE),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_share_as_image),
                contentDescription = stringResource(id = R.string.share_as_image),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(id = R.string.share_as_image))
        }

        Spacer(Modifier.size(ButtonDefaults.IconSpacing))

        // TIFF
        // TODO: Add TIFF icon
        Button(
            onClick = {
                viewModel.sharePages(ShareViewModel.ShareType.TIFF)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.SHARE_AS_TIFF),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_share_as_tiff),
                contentDescription = stringResource(id = R.string.share_as_tiff),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(id = R.string.share_as_tiff))
        }

        Spacer(Modifier.size(ButtonDefaults.IconSpacing))

        // PDF
        Button(
            onClick = {
                viewModel.sharePages(ShareViewModel.ShareType.PDF)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.SHARE_AS_PDF),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_share_as_pdf),
                contentDescription = stringResource(id = R.string.share_as_pdf),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(id = R.string.share_as_pdf))
        }

        // Show share as text only if at least one page has text
        val hasText = viewModel.hasText.collectAsStateWithLifecycle(false)
        if (hasText.value) {
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))

            Button(
                onClick = {
                    viewModel.sharePages(ShareViewModel.ShareType.Text)
                },
                modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.SHARE_AS_TEXT),
                enabled = hasText.value,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_share_as_text),
                    contentDescription = stringResource(id = R.string.share_as_text),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(id = R.string.share_as_text))
            }
        }
    }
}
