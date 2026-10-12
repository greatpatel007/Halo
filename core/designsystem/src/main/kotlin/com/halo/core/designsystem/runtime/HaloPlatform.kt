package com.halo.core.designsystem.runtime

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * What this phone's graphics stack can do, so every effect picks its rendering path in one place (proposal 0004).
 * Below Android 13 there are no AGSL shaders, and below Android 12 no RenderEffect (blur, shader effects). Each effect
 * keeps its look with a designed canvas fallback: gradients for lit relief, a bitmap grain, a still lit logo,
 * a frosted fill instead of a blur, and a crossfade instead of the Tide.
 */
object HaloPlatform {
    /**
     * Debug and testing: render as an Android 8–11 phone would (no shaders, no blur). Read when an effect is created;
     * set it before a screen opens (the Showroom's `?legacy=1`).
     */
    var simulateLegacy: Boolean by mutableStateOf(false)

    /** AGSL runtime shaders (Android 13+). */
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val shaders: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !simulateLegacy

    /** RenderEffect blur (Android 12+). */
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val blur: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !simulateLegacy
}
