package com.halo.core.designsystem.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import com.halo.core.designsystem.theme.HaloColors
import com.halo.core.designsystem.theme.HaloEffects
import com.halo.core.designsystem.theme.HaloStroke
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.core.designsystem.theme.LocalHaloContentColor
import kotlin.math.min

/** Depth levels. Dark mode builds depth from light — tone steps, hairlines, a top-edge light catch. */
enum class HaloDepth {
    /** L0 — the canvas. No edge. */
    Canvas,
    /** L1 — cards, rows, inputs. Surface tone + subtle line. */
    Surface,
    /** L2 — sheets, popovers, selected cards. Raised tone + default line + top highlight. */
    Raised,
    /** L3 — floating chrome. Backdrop blur + glass tint + strong line + top highlight. */
    Glass,
}

/**
 * Applies a depth level to any element. Token-explicit (non-composable) so it can sit inside other
 * modifier chains, e.g. `HaloPressable(modifier = Modifier.depth(...))`.
 *
 * @param fill overrides the level's background tone (e.g. an accent fill) but keeps its edges.
 * @param backdrop required for a real blur at [HaloDepth.Glass]; without it glass falls back to a
 * denser tint.
 */
fun Modifier.depth(
    level: HaloDepth,
    shape: Shape,
    colors: HaloColors,
    stroke: HaloStroke,
    effects: HaloEffects,
    fill: Color = Color.Unspecified,
    backdrop: HaloBackdrop? = null,
): Modifier {
    val bg = colors.bg
    val line = colors.line
    return when (level) {
        HaloDepth.Canvas -> background(fill.takeOrElse { bg.canvas }, shape)
        HaloDepth.Surface -> background(fill.takeOrElse { bg.surface }, shape)
            .edges(shape, line.subtle, null, stroke.thin, effects.highlightFalloff)
        HaloDepth.Raised -> background(fill.takeOrElse { bg.raised }, shape)
            .edges(shape, line.default, line.highlight, stroke.thin, effects.highlightFalloff)
        HaloDepth.Glass -> {
            val glassed = if (backdrop != null) {
                backdropGlass(backdrop, shape, effects.glassBlur).background(fill.takeOrElse { bg.glass }, shape)
            } else {
                background(fill.takeOrElse { bg.glass.copy(alpha = 0.94f) }, shape)
            }
            glassed.edges(shape, line.strong, line.highlight, stroke.thin, effects.highlightFalloff)
        }
    }
}

/** Container at a [HaloDepth] that also sets the content color its children inherit. */
@Composable
fun HaloSurface(
    modifier: Modifier = Modifier,
    depth: HaloDepth = HaloDepth.Surface,
    shape: Shape = RoundedCornerShape(HaloTheme.radius.lg),
    color: Color = Color.Unspecified,
    contentColor: Color = HaloTheme.colors.content.primary,
    backdrop: HaloBackdrop? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.depth(
            level = depth,
            shape = shape,
            colors = HaloTheme.colors,
            stroke = HaloTheme.stroke,
            effects = HaloTheme.effects,
            fill = color,
            backdrop = backdrop,
        ),
        contentAlignment = contentAlignment,
    ) {
        CompositionLocalProvider(LocalHaloContentColor provides contentColor) {
            content()
        }
    }
}

/** Hairline edge inset by half its width (so it never bleeds), plus an optional top light catch. */
private fun Modifier.edges(
    shape: Shape,
    border: Color,
    highlight: Color?,
    width: Dp,
    falloff: Dp,
): Modifier = drawWithCache {
    val w = width.toPx()
    val inset = w / 2f
    val outline = shape.createOutline(Size(size.width - w, size.height - w), layoutDirection, this)
    val style = Stroke(w)
    val catchLight = highlight?.let {
        Brush.verticalGradient(
            0f to it,
            1f to Color.Transparent,
            startY = 0f,
            endY = min(size.height * 0.5f, falloff.toPx()),
        )
    }
    onDrawWithContent {
        drawContent()
        translate(inset, inset) {
            drawOutline(outline, border, style = style)
            if (catchLight != null) drawOutline(outline, catchLight, style = style)
        }
    }
}
