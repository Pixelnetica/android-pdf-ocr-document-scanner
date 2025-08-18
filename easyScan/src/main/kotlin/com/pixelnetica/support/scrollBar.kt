package com.pixelnetica.support

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max

 // Got from https://stackoverflow.com/a/76630645/3023211
 fun Modifier.scrollbar(
    scrollState: ScrollState,
    direction: Orientation,
    config: ScrollbarConfig = ScrollbarConfig(),
): Modifier = composed {
    var (
        indicatorThickness, indicatorColor, trackColor, indicatorCornerRadius,
        alpha, alphaAnimationSpec, padding
    ) = config

    val isScrollingOrPanning = scrollState.isScrollInProgress
    val isVertical = direction == Orientation.Vertical

    alpha = alpha ?: if (isScrollingOrPanning) 0.8f else 0f
    alphaAnimationSpec = alphaAnimationSpec ?: tween(
        delayMillis = if (isScrollingOrPanning) 0 else 1500,
        durationMillis = if (isScrollingOrPanning) 150 else 500
    )

    val scrollbarAlpha by animateFloatAsState(alpha, alphaAnimationSpec)

    drawWithContent {
        drawContent()

        val showScrollbar = isScrollingOrPanning || scrollbarAlpha > 0.0f

        // Draw scrollbar only if currently scrolling or if scroll animation is ongoing.
        if (showScrollbar) {
            val (topPadding, bottomPadding, startPadding, endPadding) = arrayOf(
                padding.calculateTopPadding().toPx(), padding.calculateBottomPadding().toPx(),
                padding.calculateStartPadding(layoutDirection).toPx(),
                padding.calculateEndPadding(layoutDirection).toPx()
            )

            val isLtr = layoutDirection == LayoutDirection.Ltr
            val contentOffset = scrollState.value
            val viewPortLength = if (isVertical) size.height else size.width
            val viewPortCrossAxisLength = if (isVertical) size.width else size.height
            val contentLength = max(viewPortLength + scrollState.maxValue, 0.001f /* To prevent divide by zero error */)
            val scrollbarLength = viewPortLength -
                    (if (isVertical) topPadding + bottomPadding else startPadding + endPadding)
            val indicatorThicknessPx = indicatorThickness.toPx()
            val indicatorLength = max((scrollbarLength / contentLength) * viewPortLength, 20f.dp.toPx())
            val indicatorOffset = (scrollbarLength / contentLength) * contentOffset
            val scrollIndicatorSize = if (isVertical) Size(indicatorThicknessPx, indicatorLength)
            else Size(indicatorLength, indicatorThicknessPx)

            // Draw the track
            if (trackColor.isSpecified) {
                val trackPosition = if (isVertical)
                    Offset(
                        x = if (isLtr) viewPortCrossAxisLength - indicatorThicknessPx - endPadding
                        else endPadding,
                        y = topPadding,
                    )
                else
                    Offset(
                        x = if (isLtr) startPadding
                        else viewPortLength - endPadding,
                        y = viewPortCrossAxisLength - indicatorThicknessPx - bottomPadding,
                    )

                val trackSize = if (isVertical) Size(indicatorThicknessPx, viewPortLength)
                else Size(viewPortLength, indicatorThicknessPx)

                drawRoundRect(
                    color = trackColor,
                    cornerRadius = indicatorCornerRadius.let { CornerRadius(it.toPx(), it.toPx()) },
                    topLeft = trackPosition,
                    size = trackSize,
                    alpha = scrollbarAlpha
                )
            }

            val scrollIndicatorPosition = if (isVertical)
                Offset(
                    x = if (isLtr) viewPortCrossAxisLength - indicatorThicknessPx - endPadding
                    else startPadding,
                    y = indicatorOffset + topPadding
                )
            else
                Offset(
                    x = if (isLtr) indicatorOffset + startPadding
                    else viewPortLength - indicatorOffset - indicatorLength - endPadding,
                    y = viewPortCrossAxisLength - indicatorThicknessPx - bottomPadding
                )

            drawRoundRect(
                color = indicatorColor,
                cornerRadius = indicatorCornerRadius.let { CornerRadius(it.toPx(), it.toPx()) },
                topLeft = scrollIndicatorPosition,
                size = scrollIndicatorSize,
                alpha = scrollbarAlpha
            )
        }
    }
}

data class ScrollbarConfig(
    val indicatorThickness: Dp = 8.dp,
    val indicatorColor: Color = Color.Gray.copy(alpha = 0.7f),
    val trackColor: Color = Color.Unspecified,
    val indicatorCornerRadius: Dp = indicatorThickness / 2,
    val alpha: Float? = null,
    val alphaAnimationSpec: AnimationSpec<Float>? = null,
    val padding: PaddingValues = PaddingValues(all = 0.dp),
)

@Stable
@Composable
fun defaultScrollbarConfig(
    indicatorColor: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7F),
    trackColor: Color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7F),
    indicatorThickness: Dp = 8.dp,
    ) =
    ScrollbarConfig(indicatorThickness, indicatorColor, trackColor)

fun Modifier.verticalScrollWithScrollbar(
    scrollState: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    scrollbarConfig: ScrollbarConfig = ScrollbarConfig()
): Modifier = this
    .scrollbar(scrollState, direction = Orientation.Vertical, config = scrollbarConfig)
    .verticalScroll(scrollState, enabled, flingBehavior, reverseScrolling)


fun Modifier.horizontalScrollWithScrollbar(
    scrollState: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    scrollbarConfig: ScrollbarConfig = ScrollbarConfig()
): Modifier = this
    .scrollbar(scrollState, direction = Orientation.Horizontal, config = scrollbarConfig)
    .horizontalScroll(scrollState, enabled, flingBehavior, reverseScrolling)


// Got from https://stackoverflow.com/a/71932181/3023211
/**
 * Renders a scrollbar.
 *
 * <ul> <li> A scrollbar is composed of two components: a track and a knob. The knob moves across
 * the track <li> The scrollbar appears automatically when the user starts scrolling and disappears
 * after the scrolling is finished </ul>
 *
 * @param state The [LazyListState] that has been passed into the lazy list or lazy row
 * @param horizontal If `true`, this will be a horizontally-scrolling (left and right) scroll bar,
 * if `false`, it will be vertically-scrolling (up and down)
 * @param alignEnd If `true`, the scrollbar will appear at the "end" of the scrollable composable it
 * is decorating (at the right-hand side in left-to-right locales or left-hand side in right-to-left
 * locales, for the vertical scrollbars -or- the bottom for horizontal scrollbars). If `false`, the
 * scrollbar will appear at the "start" of the scrollable composable it is decorating (at the
 * left-hand side in left-to-right locales or right-hand side in right-to-left locales, for the
 * vertical scrollbars -or- the top for horizontal scrollbars)
 * @param thickness How thick/wide the track and knob should be
 * @param fixedKnobRatio If not `null`, the knob will always have this size, proportional to the
 * size of the track. You should consider doing this if the size of the items in the scrollable
 * composable is not uniform, to avoid the knob from oscillating in size as you scroll through the
 * list
 * @param knobCornerRadius The corner radius for the knob
 * @param trackCornerRadius The corner radius for the track
 * @param knobColor The color of the knob
 * @param trackColor The color of the track. Make it [Color.Transparent] to hide it
 * @param padding Edge padding to "squeeze" the scrollbar start/end in so it's not flush with the
 * contents of the scrollable composable it is decorating
 * @param visibleAlpha The alpha when the scrollbar is fully faded-in
 * @param hiddenAlpha The alpha when the scrollbar is fully faded-out. Use a non-`0` number to keep
 * the scrollbar from ever fading out completely
 * @param fadeInAnimationDurationMs The duration of the fade-in animation when the scrollbar appears
 * once the user starts scrolling
 * @param fadeOutAnimationDurationMs The duration of the fade-out animation when the scrollbar
 * disappears after the user is finished scrolling
 * @param fadeOutAnimationDelayMs Amount of time to wait after the user is finished scrolling before
 * the scrollbar begins its fade-out animation
 */
@Composable
fun Modifier.scrollbar(
    state: LazyListState,
    horizontal: Boolean,
    alignEnd: Boolean = true,
    thickness: Dp = 8.dp,
    fixedKnobRatio: Float? = null,
    knobCornerRadius: Dp = 4.dp,
    trackCornerRadius: Dp = 2.dp,
    knobColor: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75F),
    trackColor: Color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.75F),
    padding: Dp = 0.dp,
    visibleAlpha: Float = 1f,
    hiddenAlpha: Float = 0f,
    fadeInAnimationDurationMs: Int = 150,
    fadeOutAnimationDurationMs: Int = 500,
    fadeOutAnimationDelayMs: Int = 1000,
): Modifier {
    check(thickness > 0.dp) { "Thickness must be a positive integer." }
    check(fixedKnobRatio == null || fixedKnobRatio < 1f) {
        "A fixed knob ratio must be smaller than 1."
    }
    check(knobCornerRadius >= 0.dp) { "Knob corner radius must be greater than or equal to 0." }
    check(trackCornerRadius >= 0.dp) { "Track corner radius must be greater than or equal to 0." }
    check(hiddenAlpha <= visibleAlpha) { "Hidden alpha cannot be greater than visible alpha." }
    check(fadeInAnimationDurationMs >= 0) {
        "Fade in animation duration must be greater than or equal to 0."
    }
    check(fadeOutAnimationDurationMs >= 0) {
        "Fade out animation duration must be greater than or equal to 0."
    }
    check(fadeOutAnimationDelayMs >= 0) {
        "Fade out animation delay must be greater than or equal to 0."
    }

    val targetAlpha =
        if (state.isScrollInProgress) {
            visibleAlpha
        } else {
            hiddenAlpha
        }
    val animationDurationMs =
        if (state.isScrollInProgress) {
            fadeInAnimationDurationMs
        } else {
            fadeOutAnimationDurationMs
        }
    val animationDelayMs =
        if (state.isScrollInProgress) {
            0
        } else {
            fadeOutAnimationDelayMs
        }

    val alpha by
    animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec =
            tween(delayMillis = animationDelayMs, durationMillis = animationDurationMs))

    return drawWithContent {
        drawContent()

        state.layoutInfo.visibleItemsInfo.firstOrNull()?.let { firstVisibleItem ->
            if (state.isScrollInProgress || alpha > 0f) {
                // Size of the viewport, the entire size of the scrollable composable we are decorating with
                // this scrollbar.
                val viewportSize =
                    if (horizontal) {
                        size.width
                    } else {
                        size.height
                    } - padding.toPx() * 2

                // The size of the first visible item. We use this to estimate how many items can fit in the
                // viewport. Of course, this works perfectly when all items have the same size. When they
                // don't, the scrollbar knob size will grow and shrink as we scroll.
                val firstItemSize = firstVisibleItem.size

                // The *estimated* size of the entire scrollable composable, as if it's all on screen at
                // once. It is estimated because it's possible that the size of the first visible item does
                // not represent the size of other items. This will cause the scrollbar knob size to grow
                // and shrink as we scroll, if the item sizes are not uniform.
                val estimatedFullListSize = firstItemSize * state.layoutInfo.totalItemsCount

                // The difference in position between the first pixels visible in our viewport as we scroll
                // and the top of the fully-populated scrollable composable, if it were to show all the
                // items at once. At first, the value is 0 since we start all the way to the top (or start
                // edge). As we scroll down (or towards the end), this number will grow.
                val viewportOffsetInFullListSpace =
                    state.firstVisibleItemIndex * firstItemSize + state.firstVisibleItemScrollOffset

                // Where we should render the knob in our composable.
                val knobPosition =
                    (viewportSize / estimatedFullListSize) * viewportOffsetInFullListSpace + padding.toPx()
                // How large should the knob be.
                val knobSize =
                    fixedKnobRatio?.let { it * viewportSize }
                        ?: ((viewportSize * viewportSize) / estimatedFullListSize)

                // Draw the track
                drawRoundRect(
                    color = trackColor,
                    topLeft =
                        when {
                            // When the scrollbar is horizontal and aligned to the bottom:
                            horizontal && alignEnd -> Offset(padding.toPx(), size.height - thickness.toPx())
                            // When the scrollbar is horizontal and aligned to the top:
                            horizontal && !alignEnd -> Offset(padding.toPx(), 0f)
                            // When the scrollbar is vertical and aligned to the end:
                            alignEnd -> Offset(size.width - thickness.toPx(), padding.toPx())
                            // When the scrollbar is vertical and aligned to the start:
                            else -> Offset(0f, padding.toPx())
                        },
                    size =
                        if (horizontal) {
                            Size(size.width - padding.toPx() * 2, thickness.toPx())
                        } else {
                            Size(thickness.toPx(), size.height - padding.toPx() * 2)
                        },
                    alpha = alpha,
                    cornerRadius = CornerRadius(x = trackCornerRadius.toPx(), y = trackCornerRadius.toPx()),
                )

                // Draw the knob
                drawRoundRect(
                    color = knobColor,
                    topLeft =
                        when {
                            // When the scrollbar is horizontal and aligned to the bottom:
                            horizontal && alignEnd -> Offset(knobPosition, size.height - thickness.toPx())
                            // When the scrollbar is horizontal and aligned to the top:
                            horizontal && !alignEnd -> Offset(knobPosition, 0f)
                            // When the scrollbar is vertical and aligned to the end:
                            alignEnd -> Offset(size.width - thickness.toPx(), knobPosition)
                            // When the scrollbar is vertical and aligned to the start:
                            else -> Offset(0f, knobPosition)
                        },
                    size =
                        if (horizontal) {
                            Size(knobSize, thickness.toPx())
                        } else {
                            Size(thickness.toPx(), knobSize)
                        },
                    alpha = alpha,
                    cornerRadius = CornerRadius(x = knobCornerRadius.toPx(), y = knobCornerRadius.toPx()),
                )
            }
        }
    }
}
