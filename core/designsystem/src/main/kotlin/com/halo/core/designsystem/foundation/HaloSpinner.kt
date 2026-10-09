package com.halo.core.designsystem.foundation

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.core.designsystem.theme.LocalHaloContentColor

/**
 * Indeterminate progress: a ring with a fading comet tail, one revolution per [SPIN_MS] (scaled by
 * slow-mo, kept under reduced motion because it is functional). Size it with the modifier (icon sizes).
 * Prefer skeletons for loading *content*; this is for in-control pending states (buttons, commits).
 */
@Composable
fun HaloSpinner(modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val tint = color.takeOrElse { LocalHaloContentColor.current }
    val strokeWidth = HaloTheme.stroke.indicator
    val turn by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(HaloTheme.motion.timed(SPIN_MS)),
        label = "spinnerTurn",
    )
    Canvas(modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }) {
        val w = strokeWidth.toPx()
        // The arc starts at 12° so its round start-cap sits in the transparent part of the sweep
        // (otherwise the cap samples the gradient's wrap-around and shows a stray dot).
        val tail = Brush.sweepGradient(0f to Color.Transparent, 0.04f to tint.copy(alpha = 0f), 0.86f to tint, 1f to tint)
        rotate(turn) {
            drawArc(
                brush = tail,
                startAngle = 12f,
                sweepAngle = 296f,
                useCenter = false,
                topLeft = Offset(w / 2f, w / 2f),
                size = Size(size.width - w, size.height - w),
                style = Stroke(width = w, cap = StrokeCap.Round),
            )
        }
    }
}

private const val SPIN_MS = 900
