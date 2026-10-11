package com.halo.core.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * HDR emission. On an HDR display, things that *emit* light (the radium liquid, accent glows, the Tide's foam and
 * glints) may be brighter than paper white, as real light sources are; everything else stays in standard range.
 *
 * [headroom] is how many times brighter than SDR white the display can show right now (1 = no HDR). It changes with
 * the screen's brightness, so it is snapshot state: read it **in the draw phase only**, and multiply only emission by
 * [emission].
 */
@Stable
class HaloHdr internal constructor() {
    var headroom: Float by mutableFloatStateOf(1f)
        internal set

    /** Gain for emitted light: the available headroom, capped so glows stay pleasant rather than blinding. */
    val emission: Float get() = headroom.coerceIn(1f, MAX_EMISSION)

    companion object {
        /** Requested and used headroom: 2.5× SDR white for emitters. */
        const val MAX_EMISSION = 2.5f
    }
}

val LocalHaloHdr = staticCompositionLocalOf { HaloHdr() }

/**
 * Opts the window into HDR when the display supports it (Android 14+) and tracks the available headroom. On SDR
 * displays and older Android, [HaloHdr.headroom] stays 1 and nothing changes.
 */
@Composable
fun rememberHaloHdr(): HaloHdr {
    val hdr = remember { HaloHdr() }
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()
    DisposableEffect(view, activity) {
        val display = view.display
        if (activity == null || display == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            !display.isHdrSdrRatioAvailable
        ) {
            return@DisposableEffect onDispose { }
        }
        val window = activity.window
        window.colorMode = ActivityInfo.COLOR_MODE_HDR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.desiredHdrHeadroom = HaloHdr.MAX_EMISSION
        }
        hdr.headroom = display.hdrSdrRatio
        val listener = java.util.function.Consumer<android.view.Display> { hdr.headroom = it.hdrSdrRatio }
        display.registerHdrSdrRatioChangedListener(activity.mainExecutor, listener)
        onDispose { display.unregisterHdrSdrRatioChangedListener(listener) }
    }
    return hdr
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
