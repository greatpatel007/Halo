package com.halo.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corner radii. Nested rule: inner radius = outer radius − padding between them
 * (use [nested]) so concentric corners stay parallel.
 */
@Immutable
class HaloRadius(
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 28.dp,
    /** Top corners of bottom sheets and full-screen cards. */
    val sheet: Dp = 32.dp,
) {
    /** Fully rounded ends. */
    val full = RoundedCornerShape(percent = 50)

    fun nested(outer: Dp, padding: Dp): Dp = (outer - padding).coerceAtLeast(0.dp)
}
