package com.halo.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corner radii: Material 3 Expressive's corner scale (proposal 0006). Nested rule: inner radius = outer radius −
 * padding between them (use [nested]) so concentric corners stay parallel.
 *
 * The old Halo names (`xs … sheet`) are **migration aliases** onto the same values; they go away in 0006 phase 7.
 */
@Immutable
class HaloRadius(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val largeIncreased: Dp = 20.dp,
    val extraLarge: Dp = 28.dp,
    /** Top corners of bottom sheets and full-screen cards. */
    val extraLargeIncreased: Dp = 32.dp,
    val extraExtraLarge: Dp = 48.dp,
) {
    /** Fully rounded ends. */
    val full = RoundedCornerShape(percent = 50)

    fun nested(outer: Dp, padding: Dp): Dp = (outer - padding).coerceAtLeast(0.dp)

    // ---- Migration aliases (0006).
    val xs: Dp get() = small
    val sm: Dp get() = medium
    val md: Dp get() = large
    val lg: Dp get() = largeIncreased
    val xl: Dp get() = extraLarge
    val sheet: Dp get() = extraLargeIncreased
}
