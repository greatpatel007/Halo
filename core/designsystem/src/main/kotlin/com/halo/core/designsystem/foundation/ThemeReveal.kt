package com.halo.core.designsystem.foundation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.core.view.WindowCompat
import com.halo.core.designsystem.foundation.material.TIDE_AGSL
import com.halo.core.designsystem.theme.HaloMotion
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.sqrt

/**
 * "Tide" theme transition. [reveal] freezes the current frame, applies the theme change underneath it, then a
 * wave front bursts from the trigger and runs out to the far corner on a critically damped spring
 * ([HaloSpring.Tide], kicked so it starts at full speed and decelerates like water up a beach): content smears
 * toward the coming front, sand grains flip from the old theme to the new, a broken foam crest rides the edge,
 * wet sand and faint water glints trail behind. One full-screen AGSL pass ([TIDE_AGSL]); the live theme stays live.
 *
 * Interruptible: flipping the switch again mid-wave reverses the front from where it is, momentum intact, and
 * the theme follows it back. The washed ("behind") and unwashed ("ahead") regions keep their themes; whichever
 * one isn't live is drawn from a frozen frame.
 *
 * The host also owns the system-bar icon appearance, so the status and navigation bar icons flip only when the
 * front washes over them, not when the theme changes underneath the frozen frame.
 *
 * Reduced motion, or a device that can't run the shader, crossfades instead (Apple: replace motion with a
 * dissolve).
 */
@Stable
class HaloThemeRevealState internal constructor(internal val layer: GraphicsLayer) {
    internal var active by mutableStateOf(false)
    internal var front by mutableFloatStateOf(0f)
    internal var time by mutableFloatStateOf(0f)
    internal var isDark by mutableStateOf(false)
    /** Theme of the region the front has not reached yet / has washed over. Fixed for one tide. */
    internal var aheadDark = false
    internal var behindDark = true
    /** Frozen frames of each region, used when that region's theme isn't the live one. */
    internal var aheadFrame: BitmapShader? = null
    internal var behindFrame: BitmapShader? = null
    /** Reduced-motion crossfade: the old frame over the live one. */
    internal var fadeFrame by mutableStateOf<ImageBitmap?>(null)
    internal var fadeAlpha by mutableFloatStateOf(0f)
    /** Theme the status-bar icons follow; switches when the front crosses the bar. */
    internal var statusDark by mutableStateOf(false)
    /** Theme the navigation-bar icons follow; switches when the front crosses the bar. */
    internal var navDark by mutableStateOf(false)
    internal var origin = Offset.Zero
    internal var direction = Offset(-1f, 1f)
    internal var size = IntSize.Zero
    internal var supported = false
    /** Front travel margin beyond the screen (band + meander), set by the host from tokens. */
    internal var margin = 0f
    /** Band width; the front starts this far before the trigger so it is born there with no pop. */
    internal var band = 0f
    /** Heights of the status / navigation bars, whose icons flip as the front crosses them (see [barFlip]). */
    internal var statusInset = 0f
    internal var navInset = 0f
    internal var start = 0f

    private val tide = Animatable(0f, visibilityThreshold = 1f)
    private var end = 0f
    private var statusFlip = 0f
    private var navFlip = 0f
    private var began = 0L
    /** Whether the front is heading out (washing in the behind theme) or back. */
    private var outbound = true
    /** Bumped by every reveal / reversal; only the latest owns clean-up. */
    private var generation = 0

    /**
     * @param origin centre of the trigger in root coordinates — where the wave starts.
     * @param change flips the theme (usually a ViewModel action); the wave waits for it to take effect.
     */
    suspend fun reveal(origin: Offset, motion: HaloMotion, change: () -> Unit) {
        if (active) return reverse(motion, change)
        if (size == IntSize.Zero) {
            change()
            return
        }
        val frozen = runCatching { layer.toImageBitmap() }.getOrNull()
        if (frozen == null) {
            change()
            return
        }
        if (motion.reduced || !supported) return crossfade(frozen, motion, change)

        val run = ++generation
        this.origin = origin
        val corners = listOf(
            Offset(0f, 0f),
            Offset(size.width.toFloat(), 0f),
            Offset(0f, size.height.toFloat()),
            Offset(size.width.toFloat(), size.height.toFloat()),
        )
        val far = corners.maxBy { (it - origin).getDistance() }
        direction = (far - origin) / (far - origin).getDistance()
        // The front coordinate is convex with its minimum (0) at the origin, so the corners bound it from above.
        start = -band
        end = corners.maxOf { distance(it) } + margin
        statusFlip = barFlip(statusInset / 2f)
        navFlip = barFlip(size.height - navInset / 2f)
        aheadDark = isDark
        behindDark = !isDark
        aheadFrame = frozen.shader()
        behindFrame = null
        outbound = true
        tide.snapTo(start)
        front = start
        time = 0f
        began = System.nanoTime()
        active = true
        try {
            change()
            withTimeoutOrNull(SWITCH_TIMEOUT_MS) { snapshotFlow { isDark }.first { it == behindDark } }
            if (run != generation) return
            sweep(end, TIDE_KICK * omega(motion) * (end - start), motion)
        } finally {
            if (run == generation) finish()
        }
    }

    /** Mid-wave flip: freeze the live region, flip the theme back, and send the front the other way. */
    private suspend fun reverse(motion: HaloMotion, change: () -> Unit) {
        val run = ++generation
        runCatching { layer.toImageBitmap() }.getOrNull()?.shader()?.let { live ->
            if (isDark == behindDark) behindFrame = live else aheadFrame = live
        }
        outbound = !outbound
        val wanted = if (outbound) behindDark else aheadDark
        try {
            change()
            withTimeoutOrNull(SWITCH_TIMEOUT_MS) { snapshotFlow { isDark }.first { it == wanted } }
            if (run != generation) return
            sweep(if (outbound) end else start, tide.velocity, motion)
        } finally {
            if (run == generation) finish()
        }
    }

    private suspend fun sweep(target: Float, velocity: Float, motion: HaloMotion) {
        tide.animateTo(target, motion.spring(HaloSpring.Tide, visibilityThreshold = 1f), velocity) {
            front = value
            time = (System.nanoTime() - began) / NANOS_PER_S / motion.scale
            statusDark = if (value >= statusFlip) behindDark else aheadDark
            navDark = if (value >= navFlip) behindDark else aheadDark
        }
    }

    private suspend fun crossfade(frozen: ImageBitmap, motion: HaloMotion, change: () -> Unit) {
        val run = ++generation
        val from = isDark
        fadeFrame = frozen
        fadeAlpha = 1f
        try {
            change()
            withTimeoutOrNull(SWITCH_TIMEOUT_MS) { snapshotFlow { isDark }.first { it != from } }
            animate(1f, 0f, animationSpec = motion.fade()) { value, _ -> fadeAlpha = value }
        } finally {
            if (run == generation) fadeFrame = null
        }
    }

    private fun finish() {
        active = false
        aheadFrame = null
        behindFrame = null
        statusDark = isDark
        navDark = isDark
    }

    /** Spring angular frequency (rad/s) at the current slow-mo — [HaloMotion.spring] divides stiffness by scale². */
    private fun omega(motion: HaloMotion) = sqrt(HaloSpring.Tide.stiffness) / motion.scale

    /** The shader's front coordinate of [p]: a conic with its focus on the origin (see [TIDE_AGSL]). */
    private fun distance(p: Offset): Float {
        val d = p - origin
        return lerp(dot(d, direction), d.getDistance(), TIDE_BEND)
    }

    /**
     * Front at which a system bar's icons flip: halfway between the front touching the bar's two ends, so the
     * icon groups at either end are each mismatched for the same (shortest) time.
     */
    private fun barFlip(y: Float): Float =
        (distance(Offset(0f, y)) + distance(Offset(size.width.toFloat(), y))) / 2f

    internal companion object {
        /** 0 = planar sweep, 0.5 = parabola, above = egg around the trigger that flattens as it travels. */
        const val TIDE_BEND = 0.6f
        /**
         * Launch velocity as a fraction of ω·distance. 0.5 makes the critically damped front start at its top
         * speed and only decelerate: x(t) = D·(1 − (1 + ωt/2)·e^(−ωt)).
         */
        const val TIDE_KICK = 0.5f
        const val SWITCH_TIMEOUT_MS = 1_000L
        const val NANOS_PER_S = 1_000_000_000f
    }
}

private fun ImageBitmap.shader() = BitmapShader(asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

private fun dot(a: Offset, b: Offset) = a.x * b.x + a.y * b.y

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** The app's theme transition, or null outside a [HaloThemeRevealHost]. */
val LocalHaloThemeReveal = staticCompositionLocalOf<HaloThemeRevealState?> { null }

/** Wrap the whole window (inside `HaloTheme`) so the tide sweeps every pixel, including floating chrome. */
@Composable
fun HaloThemeRevealHost(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // `layer` holds the plain live content (what gets frozen); `fx` draws it through the tide effect.
    val layer = rememberGraphicsLayer()
    val fx = rememberGraphicsLayer()
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
    // Compile the shader pipeline up front: a first-use compile stalls the first tide by ~250ms on the emulator.
    // For a couple of frames the effect runs with the front far away and roles swapped, which outputs exactly the
    // live content.
    var warming by remember { mutableStateOf(shader != null) }
    val blank = remember { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap().shader() }
    LaunchedEffect(Unit) {
        repeat(WARM_FRAMES) { withFrameNanos { } }
        warming = false
    }
    val statusHeight = WindowInsets.statusBars.getTop(density)
    val navHeight = WindowInsets.navigationBars.getBottom(density)
    SideEffect {
        state.isDark = isDark
        state.supported = shader != null
        state.band = band
        state.margin = band + amplitude * MEANDER_REACH
        state.statusInset = statusHeight.toFloat()
        state.navInset = navHeight.toFloat()
        if (!state.active) {
            state.statusDark = isDark
            state.navDark = isDark
        }
    }
    val view = LocalView.current
    LaunchedEffect(view) {
        val window = view.context.findActivity()?.window ?: return@LaunchedEffect
        val bars = WindowCompat.getInsetsController(window, view)
        snapshotFlow { state.statusDark to state.navDark }.collect { (status, nav) ->
            bars.isAppearanceLightStatusBars = !status
            bars.isAppearanceLightNavigationBars = !nav
        }
    }
    CompositionLocalProvider(LocalHaloThemeReveal provides state) {
        Box(
            modifier
                .onSizeChanged { state.size = it }
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    // The live theme is either the washed (behind) region or, after a reversal, the unwashed one.
                    val liveIsBehind = state.isDark == state.behindDark
                    val frozen = if (liveIsBehind) state.aheadFrame else state.behindFrame
                    val tide = state.active && frozen != null
                    if (shader != null && (tide || warming)) {
                        // The meander grows in over the first band of travel, so the wave is born at the trigger.
                        val grow = if (tide) ((state.front - state.start) / band).coerceIn(0f, 1f) else 0f
                        with(shader) {
                            setFloatUniform("size", size.width, size.height)
                            setFloatUniform("origin", state.origin.x, state.origin.y)
                            setFloatUniform("dir", state.direction.x, state.direction.y)
                            setFloatUniform("bend", HaloThemeRevealState.TIDE_BEND)
                            setFloatUniform("front", if (tide) state.front else -WARM_FRONT)
                            setFloatUniform("band", band)
                            setFloatUniform("dissolve", dissolve)
                            setFloatUniform("grain", grain)
                            setFloatUniform("amplitude", amplitude * grow)
                            setFloatUniform("smear", smear)
                            setFloatUniform("time", state.time)
                            setFloatUniform("swap", if (tide && liveIsBehind) 0f else 1f)
                            setColorUniform("foam", foam)
                            setInputShader("frozen", frozen ?: blank)
                        }
                        fx.renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
                    } else if (fx.renderEffect != null) {
                        fx.renderEffect = null
                    }
                    fx.record { drawLayer(layer) }
                    drawLayer(fx)
                    state.fadeFrame?.let { drawImage(it, alpha = state.fadeAlpha) }
                },
        ) { content() }
    }
}

/** Frames the shader is drawn (invisibly) at start-up to compile it. */
private const val WARM_FRAMES = 2

/** Front position for the warm-up: far behind the origin, so every pixel is "ahead" (the live content). */
private const val WARM_FRONT = 1e6f

/** Furthest the meander (1x) plus swell (0.45x) can push the front, in amplitudes. */
private const val MEANDER_REACH = 1.45f
