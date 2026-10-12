package com.halo.feature.showroom.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.halo.core.designsystem.foundation.HaloBackdrop
import com.halo.core.designsystem.foundation.HaloPressable
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.backdropGlass
import com.halo.core.designsystem.foundation.material.ReliefPainter
import com.halo.core.designsystem.icons.HaloIcon
import com.halo.core.designsystem.theme.HaloAccentColors
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloLight
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

/*
 * Clean glass, the lab version (2026-10-12). One material family for the whole UI:
 * - Pane: a thin sheet. Strong frost, very light tint, a specular line along the lit edge, a faint far rim.
 * - Solid glass: a thick piece (tab thumb, key). Brighter body, a specular streak, a caustic it focuses onto what
 *   lies beneath (away from the light), and its state as coloured light inside it.
 * - Inset glass: a groove in a pane (the tab track). Darker, shaded on the lit side, its far lip catching light.
 * All of it reads the world-fixed light in draw.
 */

// ------------------------------------------------------------------------------------------- tuning

/** Live multipliers for fine-tuning the glass on the device (read in draw, so a change only redraws). */
@Stable
internal class GlassTuning {
    var frost by mutableFloatStateOf(1f)
    var tint by mutableFloatStateOf(1f)
    var rim by mutableFloatStateOf(1f)
    var inner by mutableFloatStateOf(1f)
    var caustic by mutableFloatStateOf(1f)
}

internal val LocalGlassTuning = staticCompositionLocalOf { GlassTuning() }

// ------------------------------------------------------------------------------------------- pane

/** A thin pane of clean glass over [backdrop]: frost, a whisper of tint, a lit edge line and a short contact shadow. */
@Composable
internal fun Modifier.clearGlass(radius: Dp, backdrop: HaloBackdrop): Modifier {
    val painter = remember { ReliefPainter() }
    val light = HaloTheme.light
    val colors = HaloTheme.colors
    val dark = colors.isDark
    val tint = colors.bg.glass
    val d = LocalDensity.current
    val r = with(d) { radius.toPx() }
    val line = with(d) { HaloTheme.stroke.thin.toPx() }
    val elevation = with(d) { HaloTheme.spacing.xxs.toPx() }
    val tune = LocalGlassTuning.current
    val blur = HaloTheme.effects.glassBlur * PANE_BLUR * tune.frost
    return drawBehind { painter.drawContactShadow(this, Rect(Offset.Zero, size), r, elevation, light, PANE_SHADOW) }
        .backdropGlass(backdrop, RoundedCornerShape(radius), blur)
        .drawBehind {
            val body = Rect(Offset.Zero, size)
            val dir = towardLight(light)
            val reach = hypot(size.width, size.height) / 2f
            val lit = center + dir * reach
            val far = center - dir * reach
            drawRoundRect(tint.copy(alpha = (tint.alpha * tune.tint * if (dark) PANE_TINT_DARK else PANE_TINT_LIGHT).coerceAtMost(1f)), cornerRadius = CornerRadius(r))
            val k = tune.rim
            // Broad sheen: the pane reflects the room a little more on the side facing the light.
            drawRoundRect(
                Brush.linearGradient(listOf(light.highlight.copy(alpha = if (dark) 0.035f else 0.25f), Color.Transparent), lit, center),
                cornerRadius = CornerRadius(r),
            )
            // Edge: a crisp specular line where the lit edge catches the key light, a faint glow on the far edge.
            val edge = body.deflate(line / 2f)
            drawRoundRect(
                Brush.linearGradient(
                    listOf(
                        light.highlight.copy(alpha = ((if (dark) 0.55f else 1f) * k).coerceAtMost(1f)),
                        light.highlight.copy(alpha = ((if (dark) 0.06f else 0.5f) * k).coerceAtMost(1f)),
                        light.highlight.copy(alpha = ((if (dark) 0.14f else 0.8f) * k).coerceAtMost(1f)),
                    ),
                    lit, far,
                ),
                edge.topLeft, edge.size, CornerRadius(r - line / 2f), style = Stroke(line),
            )
            if (!dark) {
                // On light, white glass needs its silhouette from the shadow side, as real glass does on paper.
                drawRoundRect(
                    Brush.linearGradient(listOf(Color.Transparent, light.shadow.copy(alpha = light.shadowOpacity * 0.35f)), center, far),
                    edge.topLeft, edge.size, CornerRadius(r - line / 2f), style = Stroke(line),
                )
            }
        }
}

// ------------------------------------------------------------------------------------------- solid glass

/**
 * Draws a thick piece of solid glass into [body]: caustic beneath, a bright body, coloured light inside ([glow] ×
 * [lit] 0..1), a specular streak and a lit rim. [press] 0..1 lowers it (the caustic tightens, the inner light rises).
 */
private fun DrawScope.solidGlass(
    body: Rect,
    radius: Float,
    light: HaloLight,
    dark: Boolean,
    glow: Color,
    lit: Float,
    press: Float,
    painter: ReliefPainter,
    elevation: Float,
    line: Float,
    tune: GlassTuning,
    clarity: Float = 0f,
) {
    val dir = towardLight(light)
    val corner = CornerRadius(radius)
    val shape = Path().apply { addRoundRect(RoundRect(body, corner)) }
    painter.drawContactShadow(this, body, radius, elevation * (1f - 0.6f * press), light, SOLID_SHADOW)
    // Caustic: the glass focuses the key light onto what lies beneath, on the side away from the light.
    val focus = body.center - dir * (body.height * (0.32f - 0.12f * press))
    drawCircle(
        Brush.radialGradient(
            listOf(light.highlight.copy(alpha = ((if (dark) 0.10f else 0.55f) * tune.caustic).coerceAtMost(1f)), Color.Transparent),
            focus, body.height * 0.9f,
        ),
        body.height * 0.9f, focus,
    )
    clipPath(shape) {
        val smoky = if (dark) 0.075f else 0.62f
        drawRect(light.highlight.copy(alpha = smoky + (CLEAR_BODY - smoky) * clarity), body.topLeft, body.size)
        // The state, as coloured light living inside the glass (strongest where light exits, away from the key).
        if (lit > 0f) {
            val exit = body.center - dir * body.height * 0.4f
            drawRect(
                Brush.radialGradient(
                    listOf(glow.copy(alpha = (glow.alpha * lit * tune.inner * (1.2f + 0.8f * press)).coerceAtMost(1f)), Color.Transparent),
                    exit, body.width * 0.55f,
                ),
                body.topLeft, body.size,
            )
        }
        // Specular streak: a soft band entering from the lit edge.
        val reach = hypot(body.width, body.height) / 2f
        drawRect(
            Brush.linearGradient(
                listOf(light.highlight.copy(alpha = ((if (dark) 0.22f else 0.9f) * tune.rim).coerceAtMost(1f)), Color.Transparent),
                body.center + dir * reach,
                body.center + dir * reach * 0.15f,
            ),
            body.topLeft, body.size,
        )
    }
    val reach = hypot(body.width, body.height) / 2f
    val edge = body.deflate(line / 2f)
    drawRoundRect(
        Brush.linearGradient(
            listOf(
                light.highlight.copy(alpha = ((if (dark) 0.55f else 1f) * tune.rim).coerceAtMost(1f)),
                light.highlight.copy(alpha = ((if (dark) 0.05f else 0.4f) * tune.rim).coerceAtMost(1f)),
                light.highlight.copy(alpha = ((if (dark) 0.22f else 0.9f) * tune.rim).coerceAtMost(1f)),
            ),
            body.center + dir * reach, body.center - dir * reach,
        ),
        edge.topLeft, edge.size, CornerRadius(radius - line / 2f), style = Stroke(line),
    )
    if (!dark) {
        drawRoundRect(
            Brush.linearGradient(listOf(Color.Transparent, light.shadow.copy(alpha = light.shadowOpacity * 0.45f)), body.center, body.center - dir * reach),
            edge.topLeft, edge.size, CornerRadius(radius - line / 2f), style = Stroke(line),
        )
    }
}

/**
 * A frosted glass disc (the Nothing look): milky, matte, no gloss streak. It diffuses what lies beneath, so coloured
 * light under it ([tint] × [amount] 0..1) blooms softly through the whole disc. Its volume shows as a gentle lit/far
 * gradient, its edge as a soft even rim that is a touch brighter towards the light. [press] lowers it.
 */
internal fun DrawScope.frostedDisc(
    body: Rect,
    light: HaloLight,
    dark: Boolean,
    tint: Color,
    amount: Float,
    press: Float,
    painter: ReliefPainter,
    elevation: Float,
    line: Float,
    tune: GlassTuning,
    radius: Float = body.minDimension / 2f,
) {
    val dir = towardLight(light)
    val reach = hypot(body.width, body.height) / 2f
    painter.drawContactShadow(this, body, radius, elevation * (1f - 0.6f * press), light, FROST_SHADOW)
    clipPath(Path().apply { addRoundRect(RoundRect(body, CornerRadius(radius))) }) {
        // Milk: the frost itself.
        val milk = (if (dark) FROST_MILK_DARK else FROST_MILK_LIGHT) + FROST_MILK_LIT * amount
        drawRect(light.highlight.copy(alpha = (milk * tune.tint).coerceAtMost(1f)), body.topLeft, body.size)
        // Light from beneath, scattered through the frost: strongest in the middle, still present at the edge.
        if (amount > 0f) {
            drawRect(
                Brush.radialGradient(
                    listOf(
                        tint.copy(alpha = (FROST_GLOW_CORE * amount * tune.inner).coerceAtMost(1f)),
                        tint.copy(alpha = (FROST_GLOW_EDGE * amount * tune.inner).coerceAtMost(1f)),
                    ),
                    body.center, body.width / 2f,
                ),
                body.topLeft, body.size,
            )
        }
        // Volume: frosted glass is lit softly on the light's side and dims away from it (diffuse, no streak).
        drawRect(
            Brush.linearGradient(
                listOf(
                    light.highlight.copy(alpha = if (dark) 0.14f else 0.45f),
                    Color.Transparent,
                    light.shadow.copy(alpha = light.shadowOpacity * if (dark) 0.35f else 0.18f),
                ),
                body.center + dir * reach, body.center - dir * reach,
            ),
            body.topLeft, body.size,
        )
    }
    // Edge: frosted edges glow evenly, a touch brighter towards the light.
    val edge = body.deflate(line / 2f)
    drawRoundRect(
        Brush.linearGradient(
            listOf(
                light.highlight.copy(alpha = ((if (dark) 0.5f else 1f) * tune.rim).coerceAtMost(1f)),
                light.highlight.copy(alpha = ((if (dark) 0.22f else 0.7f) * tune.rim).coerceAtMost(1f)),
            ),
            body.center + dir * reach, body.center - dir * reach,
        ),
        edge.topLeft, edge.size, CornerRadius(radius - line / 2f), style = Stroke(line),
    )
}

/** An inset groove in the glass: darker, shaded on its lit wall, its far lip catching the light. */
private fun DrawScope.insetGlass(body: Rect, radius: Float, light: HaloLight, dark: Boolean, line: Float) {
    val dir = towardLight(light)
    val corner = CornerRadius(radius)
    val reach = hypot(body.width, body.height) / 2f
    drawRoundRect(light.shadow.copy(alpha = if (dark) 0.5f else 0.1f), body.topLeft, body.size, corner)
    clipPath(Path().apply { addRoundRect(RoundRect(body, corner)) }) {
        drawRect(
            Brush.linearGradient(
                listOf(light.shadow.copy(alpha = light.shadowOpacity * if (dark) 0.5f else 0.25f), Color.Transparent),
                body.center + dir * body.height / 2f,
                body.center + dir * body.height * 0.1f,
            ),
            body.topLeft, body.size,
        )
    }
    val edge = body.deflate(line / 2f)
    drawRoundRect(
        Brush.linearGradient(
            listOf(light.shadow.copy(alpha = light.shadowOpacity * if (dark) 0.6f else 0.3f), light.highlight.copy(alpha = if (dark) 0.22f else 1f)),
            body.center + dir * reach, body.center - dir * reach,
        ),
        edge.topLeft, edge.size, CornerRadius(radius - line / 2f), style = Stroke(line),
    )
}

// ------------------------------------------------------------------------------------------- tab

/** One option of a [GlassTabs]; [tone] colours its light (accent unless a network). */
internal class GlassTab(val label: String, val tone: @Composable () -> HaloAccentColors = { HaloTheme.colors.accent })

/**
 * The tab (segmented control) in solid glass: a thick glass thumb slides in an inset groove on the Mechanical spring;
 * the selected option's colour lives inside the thumb as light, and labels take it by distance (draw phase).
 */
@Composable
internal fun GlassTabs(tabs: List<GlassTab>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val n = tabs.size.coerceAtLeast(1)
    val motion = HaloTheme.motion
    val haptics = HaloTheme.haptics
    val light = HaloTheme.light
    val dark = HaloTheme.colors.isDark
    val resting = lerp(HaloTheme.colors.content.secondary, HaloTheme.colors.content.primary, LABEL_LIFT)
    val tones = tabs.map { it.tone() }
    val position = remember { Animatable(selected.toFloat()) }
    val press = remember { Animatable(0f) }
    val painter = remember { ReliefPainter() }
    val d = LocalDensity.current
    val insetDp = HaloTheme.spacing.xs
    val inset = with(d) { insetDp.toPx() }
    val line = with(d) { HaloTheme.stroke.thin.toPx() }
    val elevation = with(d) { HaloTheme.spacing.xxs.toPx() }
    val current by rememberUpdatedState(selected)
    val tune = LocalGlassTuning.current
    LaunchedEffect(selected) { position.animateTo(selected.toFloat(), motion.spring(HaloSpring.Mechanical)) }

    Row(
        modifier
            .height(HaloTheme.sizes.controlMd)
            .selectableGroup()
            .drawBehind {
                val track = Rect(Offset.Zero, size)
                insetGlass(track, size.height / 2f, light, dark, line)
                val w = (size.width - inset * 2f) / n
                val p = position.value
                val thumb = Rect(inset + p * w, inset, inset + (p + 1f) * w, size.height - inset)
                val color = lerp(tones[p.toInt().coerceIn(0, n - 1)].fill, tones[(p.toInt() + 1).coerceAtMost(n - 1)].fill, p - p.toInt())
                frostedDisc(thumb, light, dark, color, THUMB_LIGHT, press.value, painter, elevation, line, tune, radius = thumb.height / 2f)
            }
            .padding(horizontal = insetDp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            val source = remember { MutableInteractionSource() }
            LaunchedEffect(source) {
                source.interactions.collect {
                    when (it) {
                        is PressInteraction.Press -> if (index == current) launch { press.animateTo(1f, motion.spring(HaloSpring.Snap)) }
                        is PressInteraction.Release, is PressInteraction.Cancel -> launch { press.animateTo(0f, motion.spring(HaloSpring.Quick)) }
                    }
                }
            }
            // The frosted thumb carries the meaning colour; the label under it goes full contrast.
            val tone = HaloTheme.colors.content.primary
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(
                        selected = index == selected,
                        role = Role.Tab,
                        interactionSource = source,
                        indication = null,
                    ) {
                        if (index != current) {
                            haptics.perform(HaloHaptic.Tick)
                            onSelect(index)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = tab.label,
                    style = HaloTheme.typography.label,
                    color = { lerp(resting, tone, (1f - abs(position.value - index)).coerceIn(0f, 1f)) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------- key

/** A key of solid glass: a thick disc, its icon and inner light in the accent; pressing lowers it and the light rises. */
@Composable
internal fun GlassKey(label: String, icon: ImageVector) {
    val source = remember { MutableInteractionSource() }
    val press = remember { Animatable(0f) }
    val motion = HaloTheme.motion
    val painter = remember { ReliefPainter() }
    val light = HaloTheme.light
    val dark = HaloTheme.colors.isDark
    val accent = HaloTheme.colors.accent
    val d = LocalDensity.current
    val line = with(d) { HaloTheme.stroke.thin.toPx() }
    val elevation = with(d) { HaloTheme.spacing.xs.toPx() }
    val tune = LocalGlassTuning.current
    LaunchedEffect(source) {
        source.interactions.collect {
            when (it) {
                is PressInteraction.Press -> launch { press.animateTo(1f, motion.spring(HaloSpring.Snap)) }
                is PressInteraction.Release, is PressInteraction.Cancel -> launch { press.animateTo(0f, motion.spring(HaloSpring.Quick)) }
            }
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HaloPressable(
            onClick = {},
            shape = CircleShape,
            interactionSource = source,
            onClickLabel = label,
            indication = null,
            modifier = Modifier
                .size(HaloTheme.sizes.controlLg)
                .drawBehind {
                    val body = Rect(Offset.Zero, size)
                    frostedDisc(body, light, dark, accent.fill, KEY_LIGHT + (1f - KEY_LIGHT) * press.value, press.value, painter, elevation, line, tune)
                },
        ) {
            HaloIcon(icon, contentDescription = null, modifier = Modifier.wrapContentSize(), size = HaloTheme.sizes.iconMd, tint = accent.text)
        }
        Spacer(Modifier.height(HaloTheme.spacing.sm))
        HaloText(label, style = HaloTheme.typography.bodySmall, color = HaloTheme.colors.content.secondary)
    }
}

// ------------------------------------------------------------------------------------------- toggle

/**
 * The switch in glass: a solid glass bead in an inset groove. When on, the groove fills with the tone's light from the
 * start edge to the bead, and the bead carries that light inside. Mechanical travel, a 15% lean on press, ToggleOn/Off.
 */
@Composable
internal fun GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tone: HaloAccentColors = HaloTheme.colors.accent,
    contentDescription: String? = null,
) {
    val source = remember { MutableInteractionSource() }
    val position = remember { Animatable(if (checked) 1f else 0f) }
    val press = remember { Animatable(0f) }
    val motion = HaloTheme.motion
    val haptics = HaloTheme.haptics
    val light = HaloTheme.light
    val dark = HaloTheme.colors.isDark
    val painter = remember { ReliefPainter() }
    val tune = LocalGlassTuning.current
    val d = LocalDensity.current
    val line = with(d) { HaloTheme.stroke.thin.toPx() }
    val elevation = with(d) { HaloTheme.spacing.xxs.toPx() }
    val housing = HaloTheme.sizes.switchLg
    val checkedNow by rememberUpdatedState(checked)
    LaunchedEffect(checked) { position.animateTo(if (checked) 1f else 0f, motion.spring(HaloSpring.Mechanical)) }
    LaunchedEffect(source) {
        source.interactions.collect {
            when (it) {
                is PressInteraction.Press -> {
                    launch { press.animateTo(1f, motion.spring(HaloSpring.Snap)) }
                    if (!motion.reduced) launch { position.animateTo(if (checkedNow) 1f - LEAN else LEAN, motion.spring(HaloSpring.Snap)) }
                }
                is PressInteraction.Release -> launch { press.animateTo(0f, motion.spring(HaloSpring.Quick)) }
                is PressInteraction.Cancel -> {
                    launch { press.animateTo(0f, motion.spring(HaloSpring.Quick)) }
                    launch { position.animateTo(if (checkedNow) 1f else 0f, motion.spring(HaloSpring.Mechanical)) }
                }
            }
        }
    }
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
                    val track = Rect(Offset.Zero, size)
                    insetGlass(track, h / 2f, light, dark, line)
                    val inset = h * BEAD_INSET
                    val bead = h - inset * 2f
                    val travel = size.width - inset * 2f - bead
                    val p = position.value.coerceIn(0f, 1f)
                    val left = inset + p * travel
                    // On: the groove becomes lit coloured glass, opaque at the full meaning colour (a wash over a dark
                    // floor turns olive; a lit body never does). It grows from the start edge with the bead and ends
                    // under it, shaded on its lit wall, its far lip catching light, its core a touch brighter.
                    if (p > 0f) {
                        val groove = track.deflate(line)
                        val end = left + bead + inset
                        clipPath(Path().apply { addRoundRect(RoundRect(groove, CornerRadius(groove.height / 2f))) }) {
                            val lit = Rect(groove.left, groove.top, end, groove.bottom)
                            drawRect(tone.fill.copy(alpha = p), lit.topLeft, lit.size)
                            drawRect(
                                Brush.verticalGradient(
                                    listOf(
                                        light.shadow.copy(alpha = light.shadowOpacity * WALL_SHADE * p),
                                        Color.Transparent,
                                        light.highlight.copy(alpha = CORE_GLOW * p * tune.inner),
                                        Color.Transparent,
                                    ),
                                    groove.top, groove.bottom,
                                ),
                                lit.topLeft, lit.size,
                            )
                        }
                    }
                    val beadRect = Rect(left, inset, left + bead, inset + bead)
                    frostedDisc(beadRect, light, dark, tone.fill, p, press.value, painter, elevation, line, tune)
                },
        )
    }
}

/** Unit vector toward the key light in screen space (top-left by default). */
internal fun towardLight(light: HaloLight): Offset {
    val len = hypot(light.x, light.y)
    return if (len < 1e-3f) Offset(-0.7f, -0.7f) else Offset(light.x / len, light.y / len)
}

private const val PANE_BLUR = 1.5f
private const val PANE_SHADOW = 0.75f
private const val PANE_TINT_DARK = 0.85f
private const val PANE_TINT_LIGHT = 0.95f
private const val SOLID_SHADOW = 0.55f
private const val KEY_LIGHT = 0.25f
private const val THUMB_LIGHT = 0.35f
private const val LABEL_LIFT = 0.35f
private const val FROST_SHADOW = 0.6f
private const val FROST_MILK_DARK = 0.22f
private const val FROST_MILK_LIGHT = 0.72f
private const val FROST_GLOW_CORE = 0.5f
private const val FROST_MILK_LIT = 0.28f
private const val FROST_GLOW_EDGE = 0.3f
private const val LEAN = 0.15f
private const val BEAD_INSET = 0.1f
private const val WALL_SHADE = 0.55f
private const val CORE_GLOW = 0.22f
private const val CLEAR_BODY = 0.92f
