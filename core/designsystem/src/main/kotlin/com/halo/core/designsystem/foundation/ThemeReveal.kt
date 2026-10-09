package com.halo.core.designsystem.foundation

import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.halo.core.designsystem.foundation.material.TIDE_AGSL
import com.halo.core.designsystem.theme.HaloEasing
import com.halo.core.designsystem.theme.HaloMotion
import com.halo.core.designsystem.theme.HaloTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * "Tide" theme transition. [reveal] freezes the current frame, applies the theme change underneath it, then a
 * wave front sweeps from the trigger to the far corner: content smears toward the coming front, sand grains
 * flip from the old theme to the new, a broken foam crest rides the edge, wet sand and faint water glints
 * trail behind. One full-screen AGSL pass ([TIDE_AGSL]) on the window layer; the new theme stays live.
 *
 * Reduced motion, or a device that can't run the shader (emergency fallback), switches instantly.
 */
@Stable
class HaloThemeRevealState internal constructor(internal val layer: GraphicsLayer) {
    internal var active by mutableStateOf(false)
    internal var front by mutableFloatStateOf(0f)
    internal var time by mutableFloatStateOf(0f)
    internal var isDark by mutableStateOf(false)
    internal var previous: BitmapShader? = null
    internal var origin = Offset.Zero
    internal var direction = Offset(-1f, 1f)
    internal var size = IntSize.Zero
    internal var supported = false
    /** Front travel margin beyond the screen (band + meander), set by the host from tokens. */
    internal var margin = 0f

    /**
     * @param origin centre of the trigger in root coordinates — where the wave starts.
     * @param change flips the theme (usually a ViewModel action); the wave waits for it to take effect.
     */
    suspend fun reveal(origin: Offset, motion: HaloMotion, change: () -> Unit) {
        if (motion.reduced || !supported || size == IntSize.Zero || active) {
            change()
            return
        }
        val frozen = runCatching { layer.toImageBitmap() }.getOrNull()
        if (frozen == null) {
            change()
            return
        }
        val from = isDark
        previous = BitmapShader(frozen.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        this.origin = origin
        val corners = listOf(
            Offset(0f, 0f),
            Offset(size.width.toFloat(), 0f),
            Offset(0f, size.height.toFloat()),
            Offset(size.width.toFloat(), size.height.toFloat()),
        )
        val far = corners.maxBy { (it - origin).getDistance() }
        direction = (far - origin) / (far - origin).getDistance()
        val alongs = corners.map { dot(it - origin, direction) }
        val start = alongs.min() - margin
        val end = alongs.max() + margin
        front = start
        time = 0f
        active = true
        try {
            change()
            withTimeoutOrNull(SWITCH_TIMEOUT_MS) { snapshotFlow { isDark }.first { it != from } }
            withFrameNanos { }
            val duration = motion.millis(TIDE_MS).toFloat()
            val began = withFrameNanos { it }
            var progress = 0f
            while (progress < 1f) {
                val now = withFrameNanos { it }
                val elapsed = (now - began) / NANOS_PER_MS
                progress = (elapsed / duration).coerceIn(0f, 1f)
                time = elapsed / MS_PER_S / motion.scale
                front = start + (end - start) * HaloEasing.Standard.transform(progress)
            }
        } finally {
            active = false
            previous = null
        }
    }

    private companion object {
        const val SWITCH_TIMEOUT_MS = 1_000L
        const val TIDE_MS = 1_300
        const val NANOS_PER_MS = 1_000_000f
        const val MS_PER_S = 1_000f
    }
}

private fun dot(a: Offset, b: Offset) = a.x * b.x + a.y * b.y

/** The app's theme transition, or null outside a [HaloThemeRevealHost]. */
val LocalHaloThemeReveal = staticCompositionLocalOf<HaloThemeRevealState?> { null }

/** Wrap the whole window (inside `HaloTheme`) so the tide sweeps every pixel, including floating chrome. */
@Composable
fun HaloThemeRevealHost(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val state = remember(layer) { HaloThemeRevealState(layer) }
    val shader = remember { runCatching { RuntimeShader(TIDE_AGSL) }.getOrNull() }
    val effects = HaloTheme.effects
    val foam = HaloTheme.light.highlight.toArgb()
    val density = LocalDensity.current
    val band = with(density) { effects.tideBand.toPx() }
    val dissolve = with(density) { effects.tideDissolve.toPx() }
    val grain = with(density) { effects.tideGrain.toPx() }
    val amplitude = with(density) { effects.tideAmplitude.toPx() }
    val smear = with(density) { effects.tideSmear.toPx() }
    SideEffect {
        state.isDark = isDark
        state.supported = shader != null
        state.margin = band + amplitude * 1.5f
    }
    CompositionLocalProvider(LocalHaloThemeReveal provides state) {
        Box(
            modifier
                .onSizeChanged { state.size = it }
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    val previous = state.previous
                    if (state.active && shader != null && previous != null) {
                        with(shader) {
                            setFloatUniform("size", size.width, size.height)
                            setFloatUniform("origin", state.origin.x, state.origin.y)
                            setFloatUniform("dir", state.direction.x, state.direction.y)
                            setFloatUniform("front", state.front)
                            setFloatUniform("band", band)
                            setFloatUniform("dissolve", dissolve)
                            setFloatUniform("grain", grain)
                            setFloatUniform("amplitude", amplitude)
                            setFloatUniform("smear", smear)
                            setFloatUniform("time", state.time)
                            setColorUniform("foam", foam)
                            setInputShader("previous", previous)
                        }
                        layer.renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
                    } else if (layer.renderEffect != null) {
                        layer.renderEffect = null
                    }
                    drawLayer(layer)
                },
        ) { content() }
    }
}
