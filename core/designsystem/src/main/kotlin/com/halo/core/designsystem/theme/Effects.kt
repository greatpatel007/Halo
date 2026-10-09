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
    /** Tide (theme transition): width of the smeared band either side of the wave front. */
    val tideBand: Dp = 140.dp,
    /** Tide: half-width of the sand dissolve at the front. */
    val tideDissolve: Dp = 14.dp,
    /** Tide: sand grain size. */
    val tideGrain: Dp = 1.dp,
    /** Tide: how far the front meanders (water). */
    val tideAmplitude: Dp = 28.dp,
    /** Tide: longest directional smear, at the front. */
    val tideSmear: Dp = 36.dp,
)

/** Light ceramic shows grain more readily than soft-touch black, so it gets less. */
fun haloEffects(dark: Boolean): HaloEffects =
    if (dark) HaloEffects() else HaloEffects(grain = 0.03f)
