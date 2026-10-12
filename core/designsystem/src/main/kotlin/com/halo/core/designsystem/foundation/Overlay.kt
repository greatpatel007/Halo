package com.halo.core.designsystem.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.halo.core.designsystem.runtime.HaloPlatform
import com.halo.core.designsystem.theme.HaloTheme

/**
 * The window's overlay layer: sheets, dialogs and toasts declared anywhere render here, above the app content and
 * inside the theme transition, in the same window (so the Tide sweeps them, the backdrop and haptics are shared, and
 * the content behind can recede). `HaloApp` installs it; outside `HaloApp` overlays draw where they are declared.
 */
@Stable
internal class HaloOverlayHost {
    private val order = mutableStateListOf<Any>()
    private val entries = mutableStateMapOf<Any, @Composable () -> Unit>()
    private val recedes = mutableStateMapOf<Any, () -> Float>()

    /** How far the content behind has receded (0 … 1): the strongest open overlay wins. Read in draw only. */
    val recede: Float get() = recedes.values.maxOfOrNull { it() }?.coerceIn(0f, 1f) ?: 0f

    fun show(owner: Any, recede: (() -> Float)?, content: @Composable () -> Unit) {
        if (owner !in entries) order += owner
        entries[owner] = content
        if (recede != null) recedes[owner] = recede else recedes -= owner
    }

    fun remove(owner: Any) {
        order -= owner
        entries -= owner
        recedes -= owner
    }

    @Composable
    fun Layer() {
        order.forEach { owner -> key(owner) { entries[owner]?.invoke() } }
    }
}

internal val LocalHaloOverlay = staticCompositionLocalOf<HaloOverlayHost?> { null }

/**
 * Hosts the overlay layer around [content]. While an overlay is open the content behind recedes: it scales back
 * slightly, rounds its corners and (where the platform blurs) softens, so the overlay reads as nearer.
 */
@Composable
internal fun HaloOverlayHost(content: @Composable () -> Unit) {
    val host = remember { HaloOverlayHost() }
    val density = LocalDensity.current
    val effects = HaloTheme.effects
    val sheetRadius = HaloTheme.radius.sheet
    val drop = with(density) { HaloTheme.spacing.sm.toPx() }
    val maxBlur = with(density) { effects.glassBlur.toPx() } * RECEDE_BLUR
    val blurs = HaloPlatform.blur
    CompositionLocalProvider(LocalHaloOverlay provides host) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = host.recede
                        val s = 1f - RECEDE_SCALE * p
                        scaleX = s
                        scaleY = s
                        translationY = drop * p
                        if (p > 0f) {
                            shape = RoundedCornerShape(sheetRadius * p)
                            clip = true
                            // Blur in steps of 1px: the effect is rebuilt only when its radius visibly changes.
                            val radius = (maxBlur * p).toInt().toFloat()
                            renderEffect = if (blurs && radius >= 1f) BlurEffect(radius, radius, TileMode.Clamp) else null
                        } else {
                            shape = RectangleShape
                            clip = false
                            renderEffect = null
                        }
                    },
            ) { content() }
            host.Layer()
        }
    }
}

/**
 * Renders [content] in the window's overlay layer while this call is in the composition (or in place, outside
 * `HaloApp`). [recede] reports how open the overlay is (0 … 1) so the content behind can recede with it.
 */
@Composable
internal fun HaloOverlay(recede: (() -> Float)? = null, content: @Composable () -> Unit) {
    val host = LocalHaloOverlay.current
    if (host == null) {
        content()
        return
    }
    val owner = remember { Any() }
    val latest by rememberUpdatedState(content)
    val latestRecede by rememberUpdatedState(recede)
    val hasRecede = recede != null
    DisposableEffect(host, owner, hasRecede) {
        host.show(owner, if (hasRecede) ({ latestRecede?.invoke() ?: 0f }) else null) { latest() }
        onDispose { host.remove(owner) }
    }
}

/** The content behind an open overlay: scale 1 → 0.95, a small drop, and a soft blur where the platform has one. */
private const val RECEDE_SCALE = 0.05f
private const val RECEDE_BLUR = 0.25f
