package com.pixelnetica.support

import androidx.annotation.FloatRange
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.pixelnetica.design.read.RecognizedStatus
import com.pixelnetica.easyscan.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Cover content by shadow and show rounded pane with progress indicator
 */
@Composable
fun WaitingOverlay(
    text: String,
    overlayColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5F),
    delayMillis: Long = 0,
    @FloatRange(to = 1.0)
    progress: Float? = null,
    onCancel: (() -> Unit)? = null) {

    // Delay before show the overlay.
    var showContent by remember {
        mutableStateOf(delayMillis == 0L)
    }

    LaunchedEffect(Unit) {
        delay(delayMillis)

        showContent = true
    }

    if (showContent) {

        val pointerInteropFilter = if (onCancel != null) {
            // Enable cancel button.
            Modifier
        } else {
            // consume all input to disable interaction with CutoutView
            Modifier.pointerInteropFilter { true }
        }

        // Overlay shadow
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
                .then(pointerInteropFilter)
        ) {
            val (box, indicator, button, label) = createRefs()
            val startBarrier = createStartBarrier(indicator, label)
            val endBarrier = createEndBarrier(indicator, label)

            // Box with margins around progress circle and label
            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .constrainAs(box) {
                        linkTo(
                            startBarrier, indicator.top, endBarrier, label.bottom,
                            -ButtonDefaults.IconSpacing,
                            -ButtonDefaults.IconSpacing,
                            -ButtonDefaults.IconSpacing,
                            -ButtonDefaults.IconSpacing,
                        )

                        width = Dimension.fillToConstraints
                        // NOTE: height = Dimension.ratio() is not worked. We use
                        // Modified.aspectRation() instead
                    }
                    .aspectRatio(1f),
            ) {}

            if (progress == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .wrapContentSize()
                        .constrainAs(indicator) {
                            linkTo(parent.start, parent.end)
                            linkTo(parent.top, label.top)
                        },
                    color = MaterialTheme.colorScheme.primaryContainer,
                )
            } else {
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                )

                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .wrapContentSize()
                        .constrainAs(indicator) {
                            linkTo(parent.start, parent.end)
                            linkTo(parent.top, label.top)
                        },
                    color = MaterialTheme.colorScheme.primaryContainer,
                )
            }

            if (onCancel != null) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.constrainAs(button) {
                        linkTo(indicator.start, indicator.end)
                        linkTo(indicator.top, indicator.bottom)
                    }
                        .wrapContentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "cancel",
                    )
                }
            }

            Text(
                text = text.uppercase(),
                modifier = Modifier
                    .padding(top = ButtonDefaults.IconSpacing)
                    .wrapContentSize()
                    .constrainAs(label) {
                        linkTo(parent.start, parent.end)
                        linkTo(indicator.top, parent.bottom)
                    },
                color = MaterialTheme.colorScheme.onSurface,    // we really draw on surface!
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(
                    letterSpacing = TextUnit(0.16666667F, TextUnitType.Em)
                ),
            )
            createVerticalChain(indicator, label, chainStyle = ChainStyle.Packed)
        }
    }
}


/**
 * Special version for Recognize [RecognizedStatus]
 */
@Composable
fun WaitingOverlay(recognizingStatus: RecognizedStatus) =
    when (recognizingStatus) {
        is RecognizedStatus.None -> Unit

        is RecognizedStatus.Loading ->
            WaitingOverlay(stringResource(R.string.loading))

        is RecognizedStatus.Analyzing ->
            WaitingOverlay(
                text = stringResource(R.string.analyze),
                onCancel = recognizingStatus::onCancel
            )

        is RecognizedStatus.Recognizing ->
            WaitingOverlay(
                text = stringResource(R.string.recognize) +
                        "\n%3d%%".format((recognizingStatus.progress*100).roundToInt()),
                progress = recognizingStatus.progress,
                onCancel = recognizingStatus::onCancel,
            )
    }

