package com.pixelnetica.easyscan.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pixelnetica.easyscan.ui.TestTags
import com.pixelnetica.support.defaultScrollbarConfig
import com.pixelnetica.support.verticalScrollWithScrollbar

/**
 * Common style for navigation dialogs
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NavDialog(
    navController: NavController,
    image: @Composable RowScope.() -> Unit = { },
    title: String,
    content: @Composable ColumnScope.(NavController) -> Unit
) {
    Surface(
        // Dialogs render in their own window, so the tag-to-resource-id
        // exposure must be re-declared on the dialog root
        modifier = Modifier
            .wrapContentHeight()
            .semantics { testTagsAsResourceId = true },
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
            ,
        ) {
            // Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 8.dp)
            ) {
                image()

                Text(
                    text = title,
                    modifier = Modifier.align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.titleLarge,
                )

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = {
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .testTag(TestTags.DIALOG_CLOSE),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null
                    )
                }
            }

            HorizontalDivider(Modifier.padding(bottom = 16.dp))

            val scrollState = rememberScrollState()
            Column(modifier = Modifier
                // Apply the scrollbar first
                .verticalScrollWithScrollbar(
                    scrollState = scrollState,
                    scrollbarConfig = defaultScrollbarConfig(),
                )
                ,
                ) {
                content(navController)
            }
        }
    }
}