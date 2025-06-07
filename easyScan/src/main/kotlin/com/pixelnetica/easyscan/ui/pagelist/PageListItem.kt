package com.pixelnetica.easyscan.ui.pagelist

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pixelnetica.camera.BuildConfig
import com.pixelnetica.composability.PreviewBox
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.ui.viewitem.InitialViewItem
import com.pixelnetica.easyscan.ui.viewitem.InputViewItem
import com.pixelnetica.easyscan.ui.viewitem.InvalidViewItem
import com.pixelnetica.easyscan.ui.viewitem.OriginalViewItem
import com.pixelnetica.easyscan.ui.viewitem.OutputViewItem
import com.pixelnetica.easyscan.ui.viewitem.PageViewItem
import com.pixelnetica.easyscan.ui.viewitem.PreviewViewItem
import com.pixelnetica.easyscan.ui.viewitem.ProcessedViewItem
import com.pixelnetica.composability.dragContainerForDragHandle
import com.pixelnetica.composability.visibility
import com.pixelnetica.easyscan.ui.viewitem.PendingViewItem


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageListItem(
    navController: NavController,
    viewModel: PageListViewModel,
    viewItem: PageViewItem,
    dragDropState: com.pixelnetica.composability.DragDropState,
    hasChecked: Boolean,
    isDragging: Boolean,
) {

    val cardInteractionSource = remember { MutableInteractionSource() }

    val shape =
        if (isDragging) {
            CardDefaults.elevatedShape
        } else {
            CardDefaults.shape
        }

    val elevation =
        if (isDragging) {
            CardDefaults.cardElevation()
        } else {
            CardDefaults.cardElevation(16.dp)
        }

    val colors =
        if (viewItem.isChecked) {
            // Apply accent color as tint
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    .compositeOver(MaterialTheme.colorScheme.surfaceVariant),
                contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        else {
            // Simple default
            CardDefaults.cardColors()
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = cardInteractionSource,
                // Disable ripple for checked mode
                indication = if (hasChecked) null else LocalIndication.current,
                enabled = true,
                onLongClick = {
                    viewModel.checkPage(viewItem, !viewItem.isChecked)
                },
                onClick = {
                    // Check in action mode
                    if (hasChecked) {
                        viewModel.checkPage(viewItem, !viewItem.isChecked)
                    } else {
                        // Open current page in PageSlider
                        navController.navigate("pageSlider/${viewItem.id.asArg()}")
                    }
                }
            ),
        shape = shape,
        colors = colors,
        elevation = elevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .height(IntrinsicSize.Min)
                .padding(8.dp)
        ) {
            // Preview with progress bar
            when (viewItem) {
                is PreviewViewItem -> {
                    val imageFilter = if (viewItem.isProcessing) {
                        ColorFilter.tint(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8F),
                            BlendMode.Overlay
                        )
                    } else {
                        null
                    }

                    PreviewBox(
                        bitmap = viewItem.preview.asImageBitmap(),
                        contentDescription = "Preview",
                        colorFilter = imageFilter,
                    ) {
                        // Show progress for processing page
                        if (viewItem.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }

                // Image placeholder
                else -> {
                    PreviewBox(
                        painter = painterResource(id = R.drawable.document_photo_icon),
                        contentDescription = "Preview",
                    )
                }
            }

            // Text block
            Column(
                modifier = Modifier
                    .padding(start = 24.dp)
                    .weight(1.0F)
                    .align(Alignment.CenterVertically)
            ) {
                // The main title: modification date
                val title = when (viewItem) {
                    is InvalidViewItem -> stringResource(id = R.string.page_list_page_invalid_title)
                    is InitialViewItem -> stringResource(id = R.string.page_list_page_initial_title)
                    is InputViewItem -> stringResource(id = R.string.page_list_page_source_title)
                    is OriginalViewItem -> stringResource(id = R.string.page_list_page_pending_title)
                    is PendingViewItem -> stringResource(id = R.string.page_list_page_pending_title)
                    is ProcessedViewItem -> {
                        val dateFormat =
                            DateFormat.getMediumDateFormat(LocalContext.current)
                        dateFormat.format(viewItem.dateModified)
                    }
                }.let {
                    if (BuildConfig.DEBUG) {
                        // Display debug information
                        "{${viewItem.id.asArg()}:${viewItem.orientation.name}}\n$it"
                    } else {
                        // Normal title
                        it
                    }
                }
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                )

                // The subtitle: modification time
                val subtitle = when (viewItem) {
                    is InvalidViewItem -> {
                        viewItem.errorMessage
                    }

                    is ProcessedViewItem -> {
                        val timeFormat =
                            DateFormat.getTimeFormat(LocalContext.current)
                        timeFormat.format(viewItem.dateModified)
                    }

                    else -> ""
                }
                Text(
                    text = subtitle,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                )

                // The third title: estimated size
                val estimatedSizeTitle =
                    if (viewItem is OutputViewItem) {
                        viewItem.estimatedSize.formatBytes(LocalContext.current)
                    } else {
                        ""
                    }
                Text(
                    text = estimatedSizeTitle,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Recognition status or label
                val recognitionStatus = when {
                    viewItem.isRecognitionReady -> stringResource(id = R.string.recognized_label_ready)
                    viewItem.isRecognitionPending ->
                        if (viewItem is ProcessedViewItem) {
                            if (viewItem.hasRecognitionLookup) {
                                stringResource(
                                    id = R.string.recognized_label_percent,
                                    viewItem.recognitionProgress
                                )
                            } else {
                                stringResource(id = R.string.recognized_label_pending)
                            }
                        } else {
                            ""
                        }

                    else -> ""
                }

                // Recognition status
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .visibility(recognitionStatus.isNotEmpty()),

                    ) {
                    Text(
                        text = recognitionStatus,
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize * 3f / 4f,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                // Show warning sign if cutout is undefined
                Image(
                    painter = painterResource(id = R.drawable.ic_attention),
                    contentDescription = stringResource(id = R.string.msg_cutout_invalid),
                    modifier = Modifier.visibility(viewItem.isCutoutUndefined)
                )
            }

            // Checkbox, drag grip and page number
            // Check
            Box(
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .fillMaxHeight(),
            ) {
                if (viewItem.isChecked) {
                    // Show checkbox with FAB color
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.secondary) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                }

                // Page number
                Text(
                    text = viewItem.order.toString(),
                    modifier = Modifier.align(Alignment.Center),
                )

                // Drag handle
                Icon(
                    painter = painterResource(id = R.drawable.ic_drag_handle),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .dragContainerForDragHandle(
                            dragDropState = dragDropState,
                            key = viewItem.id
                        ),
                )
            }
        }
    }
}

private const val radix = 1024L
private const val precision = 10L
private const val format = "%.1f"
private fun Long.formatBytes(context: Context): String =
    if (this <= 0) {
        ""
    } else {

        val plurals = context.resources.getStringArray(R.array.bytes_plurals)

        var power = 1L
        val value = Array(plurals.size) { index ->
            val value = precision * this / power
            power *= radix
            Pair(value, plurals[index])
        }.filter {
            it.first > precision
        }.minByOrNull {
            it.first
        }

        if (value != null) {
            if (value.first % precision == 0L) {
                "${value.first / precision}${value.second}"
            } else {
                "${format.format(value.first.toFloat() / precision)}${value.second}"
            }
        } else {
            // Strange case. May be plurals is empty?
            toString()
        }
    }