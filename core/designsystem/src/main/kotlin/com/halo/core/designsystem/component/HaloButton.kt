package com.halo.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.halo.core.designsystem.foundation.HaloPressable
import com.halo.core.designsystem.foundation.HaloPreview
import com.halo.core.designsystem.foundation.HaloSpinner
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.haloGlow
import com.halo.core.designsystem.foundation.rememberPressProgress
import com.halo.core.designsystem.icons.HaloIcon
import com.halo.core.designsystem.icons.HaloIcons
import com.halo.core.designsystem.theme.HaloFade
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.core.designsystem.theme.LocalHaloContentColor
import kotlin.math.max
import kotlin.math.roundToInt

enum class HaloButtonVariant {
    /** Lime fill + glow. One per view. */
    Primary,
    /** Raised surface with a lit edge. */
    Secondary,
    /** No container; for tertiary actions. */
    Ghost,
    /** Orange fill + glow. On-chain Bitcoin actions only. */
    Bitcoin,
    /** Cyan fill + glow. Lightning actions only. */
    Lightning,
}

enum class HaloButtonSize { Small, Medium, Large, XLarge }

/**
 * The primary way to act. Presses squeeze the pill (0.97) and soften its corners while light blooms from
 * the touch point; [loading] collapses it into a circle with a [HaloSpinner].
 *
 * Spec: docs/components/HaloButton.md · Archetype: Pressable · Showroom: halo://showroom/button
 *
 * Wraps its content by default; give it `Modifier.fillMaxWidth()` (or any fixed width) to fill.
 */
@Composable
fun HaloButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HaloButtonVariant = HaloButtonVariant.Primary,
    size: HaloButtonSize = HaloButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val metrics = buttonMetrics(size)
    val colors = buttonColors(variant)
    val motion = HaloTheme.motion
    val effects = HaloTheme.effects
    val density = LocalDensity.current

    val press = rememberPressProgress(source)
    val collapse = remember { Animatable(if (loading) 1f else 0f) }
    LaunchedEffect(loading) { collapse.animateTo(if (loading) 1f else 0f, motion.spring(HaloSpring.Standard)) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else effects.disabledAlpha,
        animationSpec = motion.fade(HaloFade.Base),
        label = "buttonAlpha",
    )

    val heightPx = with(density) { metrics.height.roundToPx() }
    val minTouchPx = with(density) { HaloTheme.sizes.minTouch.roundToPx() }
    val pressedRadiusPx = with(density) { metrics.pressedRadius.toPx() }
    val liveShape = remember(press, collapse, pressedRadiusPx) { CollapsingPillShape(press, collapse, pressedRadiusPx) }
    val glow = if (colors.glow != Color.Unspecified) {
        Modifier.haloGlow(colors.glow, liveShape, effects.glowRadius, effects.glowSpread) {
            if (enabled) (0.55f + 0.45f * press.value.coerceIn(0f, 1f)) * (1f - 0.3f * collapse.value) else 0f
        }
    } else {
        Modifier
    }
    val stroke = HaloTheme.stroke

    Layout(
        modifier = modifier.semantics { if (loading) stateDescription = "Loading" },
        content = {
            HaloPressable(
                onClick = onClick,
                enabled = enabled && !loading,
                shape = liveShape,
                interactionSource = source,
                modifier = Modifier
                    .then(glow)
                    .graphicsLayer {
                        alpha = contentAlpha
                        shape = liveShape.snapshot()
                        clip = true
                    }
                    .drawBehind {
                        val geometry = liveShape.geometry(this.size)
                        val fill = lerp(colors.fill, colors.fillPressed, press.value.coerceIn(0f, 1f))
                        drawRoundRect(fill, geometry.topLeft, geometry.size, CornerRadius(geometry.radius))
                        if (colors.edge != Color.Unspecified) drawEdge(geometry, colors, stroke.thin.toPx())
                    },
            ) {
                CompositionLocalProvider(LocalHaloContentColor provides colors.content) {
                    ButtonContent(text, leadingIcon, trailingIcon, metrics, collapse)
                }
            }
        },
    ) { measurables, constraints ->
        val body = measurables.single()
        val fixedWidth = constraints.hasFixedWidth
        val natural = max(body.maxIntrinsicWidth(heightPx), heightPx)
        val rest = if (fixedWidth) constraints.maxWidth else natural.coerceIn(constraints.minWidth, constraints.maxWidth)
        // Content is always laid out at the rest width (stable text, real ellipsis); the collapse is visual
        // (clip + background) while the reported width shrinks so neighbours reflow with it.
        val placeable = body.measure(Constraints.fixed(rest, heightPx))
        val collapsedWidth = (rest + (heightPx - rest) * collapse.value).roundToInt()
        val width = if (fixedWidth) rest else collapsedWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = max(heightPx, minTouchPx).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            placeable.place((width - rest) / 2, (height - heightPx) / 2)
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    metrics: ButtonMetrics,
    collapse: Animatable<Float, AnimationVector1D>,
) {
    Box(contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .padding(horizontal = metrics.padding)
                .graphicsLayer { alpha = 1f - collapse.value.coerceIn(0f, 1f) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HaloTheme.spacing.sm, Alignment.CenterHorizontally),
        ) {
            leadingIcon?.let { HaloIcon(it, contentDescription = null, size = metrics.icon) }
            HaloText(text = text, style = HaloTheme.typography.label, maxLines = 1, softWrap = false)
            trailingIcon?.let { HaloIcon(it, contentDescription = null, size = metrics.icon) }
        }
        val spinnerVisible by remember { derivedStateOf { collapse.value > 0.02f } }
        if (spinnerVisible) {
            HaloSpinner(
                Modifier
                    .size(metrics.icon)
                    .graphicsLayer { alpha = collapse.value.coerceIn(0f, 1f) },
            )
        }
    }
}

// ------------------------------------------------------------------------------------------- geometry

private class PillGeometry(val topLeft: Offset, val size: Size, val radius: Float)

/**
 * The button's live outline: a pill whose corners soften with press progress and whose width collapses
 * to a circle with the loading progress. Reads animated values inside [createOutline], so the indication
 * (which re-creates outlines every draw) and the glow follow it frame by frame.
 */
private class CollapsingPillShape(
    private val press: Animatable<Float, AnimationVector1D>,
    private val collapse: Animatable<Float, AnimationVector1D>,
    private val pressedRadiusPx: Float,
) : Shape {
    fun geometry(size: Size): PillGeometry = geometry(size, press.value, collapse.value)

    fun geometry(size: Size, pressValue: Float, collapseValue: Float): PillGeometry {
        val c = collapseValue.coerceIn(0f, 1f)
        val inset = max(0f, size.width - size.height) / 2f * c
        val rest = size.height / 2f
        val pressed = pressValue.coerceIn(0f, 1f) * (1f - c)
        val radius = rest + (pressedRadiusPx.coerceAtMost(rest) - rest) * pressed
        return PillGeometry(Offset(inset, 0f), Size(size.width - inset * 2f, size.height), radius)
    }

    /** An immutable copy for graphicsLayer clipping (layers only refresh their outline when the shape changes). */
    fun snapshot(): Shape = FrozenPill(press.value, collapse.value, this)

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        geometry(size).toOutline()
}

private data class FrozenPill(val press: Float, val collapse: Float, val source: CollapsingPillShape) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        source.geometry(size, press, collapse).toOutline()
}

private fun PillGeometry.toOutline(): Outline = Outline.Rounded(
    RoundRect(topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height, CornerRadius(radius)),
)

private fun DrawScope.drawEdge(g: PillGeometry, colors: ButtonColors, widthPx: Float) {
    val half = widthPx / 2f
    val topLeft = Offset(g.topLeft.x + half, g.topLeft.y + half)
    val size = Size(g.size.width - widthPx, g.size.height - widthPx)
    val radius = CornerRadius((g.radius - half).coerceAtLeast(0f))
    drawRoundRect(colors.edge, topLeft, size, radius, style = Stroke(widthPx))
    if (colors.highlight != Color.Unspecified) {
        val light = Brush.verticalGradient(0f to colors.highlight, 0.5f to Color.Transparent, startY = 0f, endY = g.size.height)
        drawRoundRect(light, topLeft, size, radius, style = Stroke(widthPx))
    }
}

// ------------------------------------------------------------------------------------------- tokens

@Immutable
private class ButtonMetrics(val height: Dp, val padding: Dp, val icon: Dp, val pressedRadius: Dp)

@Composable
private fun buttonMetrics(size: HaloButtonSize): ButtonMetrics {
    val s = HaloTheme.sizes
    val sp = HaloTheme.spacing
    val r = HaloTheme.radius
    return when (size) {
        HaloButtonSize.Small -> ButtonMetrics(s.controlSm, sp.md, s.iconSm, r.sm)
        HaloButtonSize.Medium -> ButtonMetrics(s.controlMd, sp.lg, s.iconMd, r.md)
        HaloButtonSize.Large -> ButtonMetrics(s.controlLg, sp.xl, s.iconMd, r.md)
        HaloButtonSize.XLarge -> ButtonMetrics(s.controlXl, sp.xxl, s.iconLg, r.lg)
    }
}

@Immutable
private class ButtonColors(
    val fill: Color,
    val fillPressed: Color,
    val content: Color,
    val edge: Color = Color.Unspecified,
    val highlight: Color = Color.Unspecified,
    val glow: Color = Color.Unspecified,
)

@Composable
private fun buttonColors(variant: HaloButtonVariant): ButtonColors {
    val c = HaloTheme.colors
    return when (variant) {
        HaloButtonVariant.Primary -> ButtonColors(c.accent.fill, c.accent.fillPressed, c.accent.onFill, glow = c.accent.glow)
        HaloButtonVariant.Bitcoin -> ButtonColors(c.bitcoin.fill, c.bitcoin.fillPressed, c.bitcoin.onFill, glow = c.bitcoin.glow)
        HaloButtonVariant.Lightning -> ButtonColors(c.lightning.fill, c.lightning.fillPressed, c.lightning.onFill, glow = c.lightning.glow)
        HaloButtonVariant.Secondary -> ButtonColors(c.bg.raised, c.bg.raised, c.content.primary, edge = c.line.default, highlight = c.line.highlight)
        HaloButtonVariant.Ghost -> ButtonColors(Color.Transparent, Color.Transparent, c.content.primary)
    }
}

// ------------------------------------------------------------------------------------------- previews

@Preview
@Composable
private fun HaloButtonVariantsPreview() = HaloPreview {
    HaloButton("Continue", onClick = {}, modifier = Modifier.fillMaxWidth(), size = HaloButtonSize.XLarge)
    HaloButton("Pay invoice", onClick = {}, variant = HaloButtonVariant.Bitcoin, leadingIcon = HaloIcons.Bolt)
    HaloButton("Receive", onClick = {}, variant = HaloButtonVariant.Secondary, leadingIcon = HaloIcons.Receive)
    HaloButton("Skip", onClick = {}, variant = HaloButtonVariant.Ghost, size = HaloButtonSize.Medium)
}

@Preview
@Composable
private fun HaloButtonStatesPreview() = HaloPreview {
    HaloButton("Disabled", onClick = {}, enabled = false)
    HaloButton("Sending", onClick = {}, loading = true)
    HaloButton("Small", onClick = {}, size = HaloButtonSize.Small, variant = HaloButtonVariant.Secondary)
}
