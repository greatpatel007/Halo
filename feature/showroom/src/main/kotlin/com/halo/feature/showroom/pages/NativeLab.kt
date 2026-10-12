package com.halo.feature.showroom.pages

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.halo.core.designsystem.foundation.HaloPressable
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.material.ReliefPainter
import com.halo.core.designsystem.icons.HaloIcon
import com.halo.core.designsystem.theme.HaloAccentColors
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloLight
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

/*
 * Android-first lab (2026-10-13, research 0004). Android's guides lead; Apple fills the gaps; motion is Apple's.
 * - Content sits on opaque TONAL surfaces (Material 3 tone ladder: canvas < surface < raised). No blur, no glass.
 * - Glass is only the floating layer: a floating toolbar pill (Material 3 Expressive) with blur + a translucent tint
 *   (AOSP window-blur rule) and edge lensing (Apple's gap-filler) on Android 13+.
 * - Controls are tonal and quiet at rest; while touched they lift into frosted glass with light from the finger
 *   (Apple's "controls become glass during interaction"). Colour only for real meaning (network) and the key action.
 */

// ------------------------------------------------------------------------------------------- tonal surfaces

internal enum class Tone { Low, High }

/** An opaque tonal container (Material 3's surface-container idea on Halo's tones). Tone only: no edge, no blur. */
@Composable
internal fun Modifier.tonal(tone: Tone, radius: Dp): Modifier {
    val scheme = m3
    val fill = if (tone == Tone.High) scheme.containerHigh else scheme.container
    val r = with(LocalDensity.current) { radius.toPx() }
    return drawBehind { drawRoundRect(fill, cornerRadius = CornerRadius(r)) }
}

// ------------------------------------------------------------------------------------------- touch glass

/**
 * Draws a control's moving part: a solid tonal [rest] body that lifts into frosted glass while [press] > 0, lit from
 * the touch point [touch] (local px). [lift] grows the part with the press (Apple: the knob grows into glass).
 */
private fun DrawScope.touchGlass(
    body: Rect,
    radius: Float,
    rest: Color,
    press: Float,
    touch: Offset,
    light: HaloLight,
    dark: Boolean,
    glow: Color,
    painter: ReliefPainter,
    elevation: Float,
    line: Float,
) {
    painter.drawContactShadow(this, body, radius, elevation * (1f + press), light, 0.45f + 0.25f * press)
    val shape = Path().apply { addRoundRect(RoundRect(body, CornerRadius(radius))) }
    clipPath(shape) {
        // Rest: solid tone. Fades as the glass takes over.
        drawRect(rest.copy(alpha = rest.alpha * (1f - press * GLASS_TAKEOVER)), body.topLeft, body.size)
        if (press > 0f) {
            // Frost.
            drawRect(light.highlight.copy(alpha = (if (dark) 0.18f else 0.55f) * press), body.topLeft, body.size)
            // Light blooming from under the fingertip, spreading through the glass.
            drawRect(
                Brush.radialGradient(
                    listOf(glow.copy(alpha = 0.55f * press), glow.copy(alpha = 0f)),
                    touch, body.width * (0.35f + 0.65f * press),
                ),
                body.topLeft, body.size,
            )
        }
    }
    if (press > 0f) {
        val dir = towardLight(light)
        val reach = maxOf(body.width, body.height) / 2f
        val edge = body.deflate(line / 2f)
        drawRoundRect(
            Brush.linearGradient(
                listOf(light.highlight.copy(alpha = 0.8f * press), light.highlight.copy(alpha = 0.25f * press)),
                body.center + dir * reach, body.center - dir * reach,
            ),
            edge.topLeft, edge.size, CornerRadius(radius - line / 2f), style = Stroke(line),
        )
    }
}

/** Tracks press and touch point for a control (press springs Apple-style: response first, interruptible). */
private class TouchState {
    val press = Animatable(0f)
    var touch by mutableStateOf(Offset.Zero)
}

@Composable
private fun rememberTouch(source: MutableInteractionSource): TouchState {
    val state = remember { TouchState() }
    val motion = HaloTheme.motion
    LaunchedEffect(source) {
        source.interactions.collect {
            when (it) {
                is PressInteraction.Press -> {
                    state.touch = it.pressPosition
                    launch { state.press.animateTo(1f, motion.spring(HaloSpring.Quick)) }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> launch { state.press.animateTo(0f, motion.spring(HaloSpring.Gentle)) }
            }
        }
    }
    return state
}

// ------------------------------------------------------------------------------------------- tabs

/**
 * Segmented control, Android-first: a recessed tonal track, a raised tonal thumb (neutral unless the option carries
 * meaning: then its meaning container tint), full-contrast label. While touched the thumb lifts into frosted glass,
 * grows a little and lights from the finger; it travels on the Mechanical spring.
 */
@Composable
internal fun TonalTabs(
    tabs: List<GlassTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    families: List<M3Family>? = null,
) {
    val meaning = families != null
    val n = tabs.size.coerceAtLeast(1)
    val motion = HaloTheme.motion
    val haptics = HaloTheme.haptics
    val light = HaloTheme.light
    val colors = HaloTheme.colors
    val dark = colors.isDark
    val scheme = m3
    val fams = families ?: tabs.map { scheme.secondary }
    val position = remember { Animatable(selected.toFloat()) }
    val source = remember { MutableInteractionSource() }
    val touch = rememberTouch(source)
    val painter = remember { ReliefPainter() }
    val d = LocalDensity.current
    val insetDp = HaloTheme.spacing.xs
    val inset = with(d) { insetDp.toPx() }
    val line = with(d) { HaloTheme.stroke.thin.toPx() }
    val elevation = with(d) { HaloTheme.stroke.thin.toPx() * 2f }
    val current by rememberUpdatedState(selected)
    LaunchedEffect(selected) { position.animateTo(selected.toFloat(), motion.spring(HaloSpring.Mechanical)) }
    // Material 3 segmented control: the selected segment is the (secondary or meaning) container, labels Label Large.
    val track = scheme.containerHighest
    val resting = scheme.onSurfaceVariant

    Row(
        modifier
            .height(HaloTheme.sizes.controlMd)
            .selectableGroup()
            .drawBehind {
                drawRoundRect(track, cornerRadius = CornerRadius(size.height / 2f))
                val w = (size.width - inset * 2f) / n
                val p = position.value
                val grow = touch.press.value * LIFT * size.height
                val thumb = Rect(inset + p * w - grow, inset - grow, inset + (p + 1f) * w + grow, size.height - inset + grow)
                val i = p.toInt().coerceIn(0, n - 1)
                val j = (i + 1).coerceAtMost(n - 1)
                val rest = lerp(fams[i].container, fams[j].container, p - i)
                val glow = lerp(fams[i].color, fams[j].color, p - i).copy(alpha = 0.5f)
                val local = thumb.center
                touchGlass(thumb, thumb.height / 2f, rest, touch.press.value, local, light, dark, glow, painter, elevation, line)
            }
            .padding(horizontal = insetDp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            val tone = fams[index].onContainer
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(selected = index == selected, role = Role.Tab, interactionSource = source, indication = null) {
                        if (index != current) {
                            haptics.perform(HaloHaptic.Tick)
                            onSelect(index)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = tab.label,
                    style = m3type.labelLarge,
                    color = { lerp(resting, tone, (1f - abs(position.value - index)).coerceIn(0f, 1f)) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------- switch

/**
 * The switch, Android-first (Material 3 shape: a coloured track when on, a light handle) with Apple's interaction:
 * pressing lifts the handle into frosted glass and stretches it, lit from the finger. Travel on Mechanical.
 */
@Composable
internal fun TonalSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    family: M3Family = m3.primary,
    contentDescription: String? = null,
) {
    val scheme = m3
    val source = remember { MutableInteractionSource() }
    val touch = rememberTouch(source)
    val position = remember { Animatable(if (checked) 1f else 0f) }
    val motion = HaloTheme.motion
    val haptics = HaloTheme.haptics
    val light = HaloTheme.light
    val colors = HaloTheme.colors
    val dark = colors.isDark
    val painter = remember { ReliefPainter() }
    val d = LocalDensity.current
    val line = with(d) { HaloTheme.stroke.thin.toPx() }
    val elevation = with(d) { HaloTheme.stroke.thin.toPx() * 2f }
    val housing = HaloTheme.sizes.switchLg
    LaunchedEffect(checked) { position.animateTo(if (checked) 1f else 0f, motion.spring(HaloSpring.Mechanical)) }
    // Material 3 switch: on = the family colour; off = the highest container with an outline.
    val offTrack = scheme.containerHighest
    val outlineColor = scheme.outline
    val tune = LocalGlassTuning.current
    Box(
        modifier
            .size(housing.width, HaloTheme.sizes.minTouch)
            .semantics { contentDescription?.let { this.contentDescription = it } }
            .toggleable(checked, source, indication = null, role = Role.Switch) { value ->
                haptics.perform(if (value) HaloHaptic.ToggleOn else HaloHaptic.ToggleOff)
                onCheckedChange(value)
            },
        contentAlignment = Alignment.Center,
    ) {
        Spacer(
            Modifier
                .size(housing)
                .drawBehind {
                    val h = size.height
                    val p = position.value.coerceIn(0f, 1f)
                    val press = touch.press.value
                    drawRoundRect(lerp(offTrack, family.fill, p), cornerRadius = CornerRadius(h / 2f))
                    if (p < 1f) {
                        // Off: a hairline outline keeps the empty track legible on any surface (Material 3 off state).
                        drawRoundRect(
                            outlineColor.copy(alpha = 1f - p),
                            topLeft = Offset(line / 2f, line / 2f),
                            size = androidx.compose.ui.geometry.Size(size.width - line, h - line),
                            cornerRadius = CornerRadius(h / 2f), style = Stroke(line),
                        )
                    }
                    val inset = h * HANDLE_INSET
                    val disc = h - inset * 2f
                    val stretch = disc * STRETCH * press
                    val travel = size.width - inset * 2f - disc
                    val left = inset + p * travel - stretch * p
                    val handle = Rect(left, inset, left + disc + stretch, inset + disc)
                    // The handle is a frosted glass disc (owner's call, the Nothing look) that blooms with the track's
                    // colour when on; touched, it stretches and lights from within (Apple's interaction).
                    frostedDisc(handle, light, dark, family.fill, p, press, painter, elevation, line, tune, radius = disc / 2f)
                    if (press > 0f) {
                        clipPath(Path().apply { addRoundRect(RoundRect(handle, CornerRadius(disc / 2f))) }) {
                            drawRect(
                                Brush.radialGradient(listOf(light.highlight.copy(alpha = 0.35f * press), Color.Transparent), handle.center, handle.width * 0.7f),
                                handle.topLeft, handle.size,
                            )
                        }
                    }
                },
        )
    }
}

// ------------------------------------------------------------------------------------------- key

/** A round action key: tonal at rest (icon in the accent), lifting into lit glass while pressed. */
@Composable
internal fun TonalKey(label: String, icon: ImageVector, primary: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val touch = rememberTouch(source)
    val light = HaloTheme.light
    val colors = HaloTheme.colors
    val dark = colors.isDark
    val painter = remember { ReliefPainter() }
    val d = LocalDensity.current
    val line = with(d) { HaloTheme.stroke.thin.toPx() }
    val elevation = with(d) { HaloTheme.stroke.thin.toPx() * 2f }
    val scheme = m3
    // Material 3 Expressive: the key action is the (brand) primary container; the others are tonal (secondary container).
    val rest = if (primary) scheme.primary.container else scheme.secondary.container
    val iconTint = if (primary) scheme.primary.onContainer else scheme.secondary.onContainer
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HaloPressable(
            onClick = {},
            shape = CircleShape,
            interactionSource = source,
            onClickLabel = label,
            indication = null,
            pressedScale = 1f,
            modifier = Modifier
                .size(HaloTheme.sizes.controlLg)
                .drawBehind {
                    val grow = touch.press.value * LIFT * size.height
                    val body = Rect(Offset.Zero, size).inflate(grow)
                    touchGlass(body, body.width / 2f, rest, touch.press.value, touch.touch, light, dark, scheme.primary.color.copy(alpha = 0.5f), painter, elevation, line)
                },
        ) {
            HaloIcon(icon, contentDescription = null, modifier = Modifier.wrapContentSize(), size = HaloTheme.sizes.iconMd, tint = iconTint)
        }
        Spacer(Modifier.height(HaloTheme.spacing.sm))
        HaloText(label, style = m3type.labelMedium, color = scheme.onSurfaceVariant)
    }
}

// ------------------------------------------------------------------------------------------- floating glass

/** The content a floating glass element refracts (a lab-local backdrop with its own layer). */
internal class LabBackdrop(val layer: GraphicsLayer) {
    var position by mutableStateOf(Offset.Zero)
}

@Composable
internal fun rememberLabBackdrop(): LabBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { LabBackdrop(layer) }
}

internal fun Modifier.labSource(backdrop: LabBackdrop): Modifier = this
    .onGloballyPositioned { backdrop.position = it.positionInRoot() }
    .drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }

/**
 * A floating glass pill (Material 3 Expressive's floating toolbar) over [backdrop]:
 * - Android 13+: the backdrop blurred, then **lensed** at the rim by an AGSL shader (Apple's defining glass optic).
 * - Android 12: blur only. Below: a frosted fill.
 * - Then a translucent tint with less dim because it's blurred (AOSP window-blur rule), a 1 px light-facing rim and a
 *   short contact shadow that is stronger over busy content (Apple's adaptive shadow, approximated).
 */
@Composable
internal fun Modifier.floatingGlass(backdrop: LabBackdrop, radius: Dp): Modifier {
    val glass = rememberGraphicsLayer()
    var position by remember { mutableStateOf(Offset.Zero) }
    val painter = remember { ReliefPainter() }
    val light = HaloTheme.light
    val colors = HaloTheme.colors
    val dark = colors.isDark
    val d = LocalDensity.current
    val r = with(d) { radius.toPx() }
    val blur = with(d) { (HaloTheme.effects.glassBlur * FLOAT_BLUR).toPx() }
    val band = with(d) { HaloTheme.spacing.xl.toPx() }
    val line = with(d) { HaloTheme.stroke.thin.toPx() }
    val elevation = with(d) { HaloTheme.spacing.xs.toPx() }
    val lens = remember { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) LensEffect() else null }
    val tint = m3.container.copy(alpha = if (dark) FLOAT_TINT_DARK else FLOAT_TINT_LIGHT)
    return this
        .onGloballyPositioned { position = it.positionInRoot() }
        .drawBehind {
            val body = Rect(Offset.Zero, size)
            painter.drawContactShadow(this, body, r, elevation, light, FLOAT_SHADOW)
            val shape = Path().apply { addRoundRect(RoundRect(body, CornerRadius(r))) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Capture a margin around the glass: the rim refracts what lies just outside it.
                val margin = (band * LENS_STRENGTH + blur).toInt()
                glass.renderEffect = if (lens != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    lens.effect(size.width, size.height, r, band, blur, margin.toFloat())
                } else {
                    android.graphics.RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP).asComposeRenderEffect()
                }
                val delta = backdrop.position - position
                glass.topLeft = IntOffset(-margin, -margin)
                glass.record(size = IntSize(size.width.toInt() + margin * 2, size.height.toInt() + margin * 2)) {
                    translate(delta.x + margin, delta.y + margin) { drawLayer(backdrop.layer) }
                }
                if (lens != null) drawLayer(glass) else clipPath(shape) { drawLayer(glass) }
            } else {
                drawPath(shape, colors.bg.raised.copy(alpha = 0.94f))
            }
            drawPath(shape, tint)
            val dir = towardLight(light)
            val reach = maxOf(size.width, size.height) / 2f
            val edge = body.deflate(line / 2f)
            drawRoundRect(
                Brush.linearGradient(
                    listOf(
                        light.highlight.copy(alpha = if (dark) 0.55f else 1f),
                        light.highlight.copy(alpha = if (dark) 0.08f else 0.4f),
                        light.highlight.copy(alpha = if (dark) 0.22f else 0.8f),
                    ),
                    center + dir * reach, center - dir * reach,
                ),
                edge.topLeft, edge.size, CornerRadius(r - line / 2f), style = Stroke(line),
            )
        }
}

/** Blur, then lens the rim: near the edge, pixels sample from beyond it, bending the surroundings in like thick glass. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LensEffect {
    private val shader = RuntimeShader(LENS_AGSL)
    private var key = FloatArray(5)
    private var cached: androidx.compose.ui.graphics.RenderEffect? = null

    fun effect(w: Float, h: Float, radius: Float, band: Float, blur: Float, margin: Float): androidx.compose.ui.graphics.RenderEffect {
        val k = floatArrayOf(w, h, radius, blur, margin)
        cached?.let { if (k.contentEquals(key)) return it }
        key = k
        shader.setFloatUniform("size", w, h)
        shader.setFloatUniform("margin", margin)
        shader.setFloatUniform("radius", radius)
        shader.setFloatUniform("band", band)
        shader.setFloatUniform("strength", LENS_STRENGTH)
        val blurred = RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP)
        val lensed = RenderEffect.createRuntimeShaderEffect(shader, "content")
        return RenderEffect.createChainEffect(lensed, blurred).asComposeRenderEffect().also { cached = it }
    }
}

private const val LENS_AGSL = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float band;
uniform float strength;
uniform float margin;

float sdRound(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 fc) {
    float2 c = size * 0.5;
    float2 p = fc - margin - c;                      // the layer is captured with a margin around the glass
    float dist = -sdRound(p, c, radius);              // > 0 inside: distance to the rim
    float e = 1.0;
    float2 n = normalize(float2(
        sdRound(p + float2(e, 0.0), c, radius) - sdRound(p - float2(e, 0.0), c, radius),
        sdRound(p + float2(0.0, e), c, radius) - sdRound(p - float2(0.0, e), c, radius)) + 1e-4);
    float t = clamp(1.0 - dist / band, 0.0, 1.0);      // 1 at the rim, 0 past the band
    float bend = t * t * t * strength * band;          // refraction rises smoothly, steepest right at the rim
    half4 col = content.eval(fc + n * bend);           // sample outward: the curved rim pulls in what lies beyond it
    float inside = clamp(dist + 0.5, 0.0, 1.0);        // the shader masks the pill itself (AA): no clip, so the
    return col * half(inside);                         // margin it samples is really rendered
}
"""

private const val GLASS_TAKEOVER = 0.75f
private const val LIFT = 0.06f
private const val STRETCH = 0.35f
private const val HANDLE_INSET = 0.14f
private const val FLOAT_BLUR = 0.3f
private const val FLOAT_TINT_DARK = 0.3f
private const val FLOAT_TINT_LIGHT = 0.3f
private const val FLOAT_SHADOW = 0.8f
private const val LENS_STRENGTH = 0.9f

// ------------------------------------------------------------------------------------------- navigation bar

/** One destination in [M3NavBar]. */
internal class NavItem(val label: String, val icon: ImageVector)

/**
 * Material 3 Expressive's flexible (short) navigation bar: 64 dp, docked, items = a 24 dp icon over a Label Medium,
 * a 56 × 32 dp pill indicator in the secondary container behind the active icon (6 dp to the label). Active: icon
 * onSecondaryContainer, label secondary; inactive: onSurfaceVariant. Content scrolls beneath it, so its
 * surfaceContainer is translucent over a blurred backdrop (Android 16/17's tinted system blur; AOSP: blur always
 * paired with a colour layer). The indicator springs to the new item (Apple motion: response first).
 */
@Composable
internal fun M3NavBar(backdrop: LabBackdrop, items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val scheme = m3
    val haptics = HaloTheme.haptics
    val motion = HaloTheme.motion
    val d = LocalDensity.current
    val glass = rememberGraphicsLayer()
    var position by remember { mutableStateOf(Offset.Zero) }
    val blur = with(d) { HaloTheme.effects.glassBlur.toPx() }
    val tint = scheme.container.copy(alpha = NAV_TINT)
    val indicatorColor = scheme.secondary.container
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, motion.spring(HaloSpring.Expressive))
    }
    Row(
        modifier
            .height(NAV_HEIGHT)
            .onGloballyPositioned { position = it.positionInRoot() }
            .drawBehind {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    glass.renderEffect = android.graphics.RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP).asComposeRenderEffect()
                    val delta = backdrop.position - position
                    glass.record { translate(delta.x, delta.y) { drawLayer(backdrop.layer) } }
                    drawLayer(glass)
                    drawRect(tint)
                } else {
                    drawRect(scheme.container)
                }
            }
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item ->
            val active = i == selected
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(selected = active, role = Role.Tab, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (!active) {
                            haptics.perform(HaloHaptic.Tick)
                            onSelect(i)
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(INDICATOR_W, INDICATOR_H)
                        .drawBehind {
                            if (active) {
                                val k = reveal.value
                                val w = size.width * (0.5f + 0.5f * k)
                                drawRoundRect(
                                    indicatorColor.copy(alpha = indicatorColor.alpha * k.coerceIn(0f, 1f)),
                                    topLeft = Offset((size.width - w) / 2f, 0f),
                                    size = androidx.compose.ui.geometry.Size(w, size.height),
                                    cornerRadius = CornerRadius(size.height / 2f),
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    HaloIcon(
                        item.icon,
                        contentDescription = null,
                        size = HaloTheme.sizes.iconLg,
                        tint = if (active) scheme.secondary.onContainer else scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(NAV_GAP))
                HaloText(
                    item.label,
                    style = if (active) m3type.labelMediumEmphasized else m3type.labelMedium,
                    color = if (active) scheme.secondary.color else scheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val NAV_HEIGHT = androidx.compose.ui.unit.Dp(64f)
private val INDICATOR_W = androidx.compose.ui.unit.Dp(56f)
private val INDICATOR_H = androidx.compose.ui.unit.Dp(32f)
private val NAV_GAP = androidx.compose.ui.unit.Dp(6f)
private const val NAV_TINT = 0.82f
