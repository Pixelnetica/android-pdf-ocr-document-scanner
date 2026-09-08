package com.pixelnetica.easyscan.ui.pageunavailable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.ui.TestTags
import com.pixelnetica.easyscan.ui.main.NavDialog

/**
 * Shown instead of opening a page whose image file is no longer on the device.
 *
 * A page's record and its image files are stored separately, and on a real
 * device they can drift apart - storage cleaners, restores, and interrupted
 * saves all do it. There is nothing to show for such a page, so the app says
 * so plainly and offers the one action that resolves it. Deleting is destructive and coloured
 * accordingly; the page is otherwise left exactly as it was.
 */
@Composable
fun PageUnavailableDialog(
    navController: NavController,
    viewModel: PageUnavailableViewModel = hiltViewModel(),
) = NavDialog(
    navController = navController,
    title = stringResource(id = R.string.page_unavailable_title),
) {
    Text(
        text = stringResource(id = R.string.page_unavailable_message),
        style = MaterialTheme.typography.bodyMedium,
    )

    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            onClick = {
                navController.popBackStack()
            },
            modifier = Modifier.testTag(TestTags.PAGE_UNAVAILABLE_CANCEL),
        ) {
            Text(text = stringResource(id = R.string.page_unavailable_cancel))
        }

        TextButton(
            onClick = {
                viewModel.deletePage()
                navController.popBackStack()
            },
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.testTag(TestTags.PAGE_UNAVAILABLE_DELETE),
        ) {
            Text(text = stringResource(id = R.string.page_delete))
        }
    }
}
