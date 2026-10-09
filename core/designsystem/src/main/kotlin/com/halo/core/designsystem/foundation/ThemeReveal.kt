package com.halo.core.designsystem.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.halo.core.designsystem.theme.HaloMotion
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.hypot
import kotlin.math.max

/**
 * Circular theme reveal. [reveal] freezes the current frame, applies the theme change underneath it, then opens
 * a circle from the trigger (e.g. the theme toggle) through which the new theme appears — the old frame never
 * flickers and the light front reads as the new light sweeping the room.
 */
@Stable
class HaloThemeRevealState internal constructor(internal val layer: GraphicsLayer) {
    internal var snapshot by mutableStateOf<ImageBitmap?>(null)
    internal var origin by mutableStateOf(Offset.Zero)
    internal val radius = Animatable(0f)
    internal var isDark by mutableStateOf(false)
    internal var size = IntSize.Zero

    /**
     * @param origin centre of the reveal in root coordinates.
     * @param change flips the theme (usually a ViewModel action); the reveal waits for it to take effect.
     */
    suspend fun reveal(origin: Offset, motion: HaloMotion, change: () -> Unit) {
        if (motion.reduced || size == IntSize.Zero || snapshot != null) {
            change()
            return
        }
        val from = isDark
        snapshot = layer.toImageBitmap()
        this.origin = origin
        radius.snapTo(0f)
        change()
        withTimeoutOrNull(SWITCH_TIMEOUT_MS) { snapshotFlow { isDark }.first { it != from } }
        withFrameNanos { }
        val reach = max(
            max(hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y)),
            max(hypot(origin.x, size.height - origin.y), hypot(size.width - origin.x, size.height - origin.y)),
        )
        try {
            radius.animateTo(reach, motion.spring(HaloSpring.Gentle))
        } finally {
            snapshot = null
        }
    }

    private companion object {
        const val SWITCH_TIMEOUT_MS = 1_000L
    }
}

/** The app's theme reveal, or null outside a [HaloThemeRevealHost]. */
val LocalHaloThemeReveal = staticCompositionLocalOf<HaloThemeRevealState?> { null }

/** Wrap the whole window (inside `HaloTheme`) so a reveal captures every pixel, including floating chrome. */
@Composable
fun HaloThemeRevealHost(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val state = remember(layer) { HaloThemeRevealState(layer) }
    SideEffect { state.isDark = isDark }
    val front = HaloTheme.colors.accent.fill
    val frontWidth = HaloTheme.stroke.focus
    val path = remember { Path() }
    CompositionLocalProvider(LocalHaloThemeReveal provides state) {
        Box(
            modifier
                .onSizeChanged { state.size = it }
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                    val frozen = state.snapshot ?: return@drawWithContent
                    val r = state.radius.value
                    path.reset()
                    path.addOval(Rect(state.origin, r))
                    clipPath(path, ClipOp.Difference) { drawImage(frozen) }
                    if (r > 0f) {
                        drawCircle(
                            color = front.copy(alpha = FRONT_ALPHA),
                            radius = r,
                            center = state.origin,
                            style = Stroke(width = frontWidth.toPx()),
                        )
                    }
                },
        ) { content() }
    }
}

private const val FRONT_ALPHA = 0.6f

