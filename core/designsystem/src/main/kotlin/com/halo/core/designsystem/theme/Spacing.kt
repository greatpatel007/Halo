package com.halo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** 4dp grid. Every gap, padding and offset in a component comes from here. */
@Immutable
class HaloSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val x3: Dp = 32.dp,
    val x4: Dp = 40.dp,
    val x5: Dp = 56.dp,
    val x6: Dp = 72.dp,
    /** Horizontal screen margin. */
    val gutter: Dp = 20.dp,
)

/** Fixed component dimensions. */
@Immutable
class HaloSizes(
    val controlSm: Dp = 36.dp,
    val controlMd: Dp = 48.dp,
    val controlLg: Dp = 56.dp,
    val controlXl: Dp = 64.dp,
    /** Minimum touch target, regardless of visual size. */
    val minTouch: Dp = 48.dp,
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 20.dp,
    val iconLg: Dp = 24.dp,
    val iconXl: Dp = 32.dp,
    /** Switch housing, default (visual; touch target is padded to [minTouch]). */
    val switchMd: DpSize = DpSize(52.dp, 30.dp),
    /** Switch housing, hero (e.g. the appearance switch). */
    val switchLg: DpSize = DpSize(76.dp, 42.dp),
    /** Glass halo mark, compact (top bars, small headers). */
    val logoMd: Dp = 32.dp,
    /** Glass halo mark, hero (next to the HALO wordmark). */
    val logoLg: Dp = 56.dp,
    /** The grip pill at the top of a sheet (its touch target is the whole top band). */
    val sheetHandle: DpSize = DpSize(36.dp, 4.dp),
)

@Immutable
class HaloStroke(
    /** Exactly one physical pixel — dividers. */
    val hairline: Dp = Dp.Hairline,
    /** Outlines and borders. */
    val thin: Dp = 1.dp,
    /** Focus ring width (drawn 2dp outside the component). */
    val focus: Dp = 2.dp,
    val focusOffset: Dp = 2.dp,
    /** Icon stroke on the 24dp grid. */
    val icon: Dp = 1.75.dp,
    /** Progress rings and spinners. */
    val indicator: Dp = 2.dp,
)
