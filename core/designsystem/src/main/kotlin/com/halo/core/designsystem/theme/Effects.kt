package com.halo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Light-based depth effects (dark mode has no drop shadows). */
@Immutable
class HaloEffects(
    /** Backdrop blur behind L3 glass. */
    val glassBlur: Dp = 24.dp,
    /** Distance over which the L2+ top-edge highlight fades out. */
    val highlightFalloff: Dp = 40.dp,
    /** Glow blur radius behind accent elements. */
    val glowRadius: Dp = 20.dp,
    /** How far the glow shape extends past the element before blurring. */
    val glowSpread: Dp = 2.dp,
    /** Peak opacity of AGSL film grain on hero surfaces. */
    val grain: Float = 0.05f,
    /** Opacity of disabled components. */
    val disabledAlpha: Float = 0.38f,
)

/** Light ceramic shows grain more readily than soft-touch black, so it gets less. */
fun haloEffects(dark: Boolean): HaloEffects =
    if (dark) HaloEffects() else HaloEffects(grain = 0.03f)
