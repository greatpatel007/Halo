package com.halo.core.designsystem.foundation

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import com.halo.core.designsystem.theme.HaloMotion
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Press progress (0 = rest, 1 = fully pressed) for extra press-reactive visuals — corner morphs, fill
 * shifts, glow. Shares the exact timing of [pressScale] and [HaloIndication] (including the minimum
 * flash for fast taps), so every press response moves as one. Read `.value` in draw/layout lambdas.
 */
@Composable
fun rememberPressProgress(interactionSource: InteractionSource): Animatable<Float, AnimationVector1D> {
    val progress = remember { Animatable(0f) }
    val motion by rememberUpdatedState(HaloTheme.motion)
    LaunchedEffect(interactionSource) {
        trackPress(interactionSource, progress, { motion })
    }
    return progress
}

/** A quick tap still shows its full press response for at least this long. */
internal const val MIN_PRESS_FLASH_MS = 90L

/**
 * Drives [progress] 0 → 1 while pressed and back on release, with a guaranteed visible flash for fast
 * taps. Shared by every press response (scale, highlight, morph) so they stay perfectly in sync.
 */
internal fun CoroutineScope.trackPress(
    interactionSource: InteractionSource,
    progress: Animatable<Float, AnimationVector1D>,
    motion: () -> HaloMotion,
    onPressAt: (Offset) -> Unit = {},
): Job = launch {
    var pressedAt = 0L
    var releaseJob: Job? = null
    interactionSource.interactions.collect { interaction ->
        when (interaction) {
            is PressInteraction.Press -> {
                releaseJob?.cancel()
                pressedAt = SystemClock.uptimeMillis()
                onPressAt(interaction.pressPosition)
                launch { progress.animateTo(1f, motion().spring(HaloSpring.Snap)) }
            }
            is PressInteraction.Release, is PressInteraction.Cancel -> {
                releaseJob = launch {
                    val held = SystemClock.uptimeMillis() - pressedAt
                    if (held < MIN_PRESS_FLASH_MS) delay(motion().millis((MIN_PRESS_FLASH_MS - held).toInt()))
                    progress.animateTo(0f, motion().spring(HaloSpring.Quick))
                }
            }
        }
    }
}
