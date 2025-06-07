package com.pixelnetica.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.pixelnetica.easyscan.R
import java.util.Locale

/**
 * Cover content by shadow and show rounded pane with progress indicator
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WaitingOverlay(text: String) {
    // Overlay shadow
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5F))
            .pointerInteropFilter { true }   // consume all input to disable interaction with CutoutView
    ) {
        val (box, progress, label) = createRefs()
        val startBarrier = createStartBarrier(progress, label)
        val endBarrier = createEndBarrier(progress, label)

        // Box with margins around progress circle and label
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .constrainAs(box) {
                    linkTo(
                        startBarrier, progress.top, endBarrier, label.bottom,
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

        CircularProgressIndicator(
            modifier = Modifier
                .constrainAs(progress) {
                    linkTo(parent.start, parent.end)
                    linkTo(parent.top, label.top)
                },
            color = MaterialTheme.colorScheme.primaryContainer,
        )

        Text(
            text = text.uppercase(),
            modifier = Modifier
                .padding(top = ButtonDefaults.IconSpacing)
                .constrainAs(label) {
                    linkTo(parent.start, parent.end)
                    linkTo(progress.top, parent.bottom)
                },
            color = MaterialTheme.colorScheme.onSurface,    // we really draw on surface!
            style = MaterialTheme.typography.bodySmall.copy(
                letterSpacing = TextUnit(0.16666667F, TextUnitType.Em)
            ),
        )
        createVerticalChain(progress, label, chainStyle = ChainStyle.Packed)
    }
}