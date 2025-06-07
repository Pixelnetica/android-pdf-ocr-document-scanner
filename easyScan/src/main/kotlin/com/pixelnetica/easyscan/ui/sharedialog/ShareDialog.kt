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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pixelnetica.easyscan.R
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

    // Preview strip
    LazyRow(
        modifier = Modifier
            .height(188.dp)
            .align(Alignment.CenterHorizontally)
    ) {
        items(
            count = previews.size,
        ) { index ->
            val previewImage = previews.getOrNull(index)?.asImageBitmap()
            if (previewImage != null) {
                com.pixelnetica.composability.PreviewBox(
                    bitmap = previewImage,
                )
            } else {
                com.pixelnetica.composability.PreviewBox(
                    painter = painterResource(id = R.drawable.document_photo_icon),
                )
            }
        }
    }

    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

    val hasShareSessions by viewModel.hasShareSessions.collectAsStateWithLifecycle(false)
    if (hasShareSessions) {
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
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
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
