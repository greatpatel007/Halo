package com.halo.core.designsystem.foundation.material

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.halo.core.designsystem.theme.HaloLight
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * The glass halo without shaders (Android 8–12). It keeps what makes the mark read: the light it spills on the
 * surface, its contact shadow cast by the scene light, a tinted glass wall, the radium liquid circulating inside (a
 * sweep of brighter and dimmer bands turned by the same flow physics) and the specular glints on the walls that face
 * the light. Refraction, caustics and the scintillating grains need the AGSL path.
 */
internal fun DrawScope.drawCanvasHalo(
    ring: Float,
    tube: Float,
    light: HaloLight,
    glass: GlassMaterial,
    surface: Color,
    emission: Color,
    phase: Float,
    glow: Float,
) {
    val c = center
    val bright = glow.coerceIn(0f, GLOW_MAX)

    // Floor: the liquid's light spilling around the ring, then the contact shadow from the tube's height.
    // A smooth radial falloff either side of the ring (stepped strokes would read as a target).
    val reach = ring + tube * SPILL_REACH
    val inner = ((ring - tube * SPILL_REACH) / reach).coerceAtLeast(0f)
    val glowColor = emission.copy(alpha = (SPILL_ALPHA * bright).coerceIn(0f, 1f))
    drawCircle(
        Brush.radialGradient(
            0f to Color.Transparent,
            inner to Color.Transparent,
            ring / reach to glowColor,
            1f to Color.Transparent,
            center = c,
            radius = reach,
        ),
        reach, c,
    )
    val shadow = Offset(light.shadowDx, light.shadowDy) * tube
    drawCircle(light.shadow.copy(alpha = light.shadowOpacity * SHADOW_ALPHA), ring, c + shadow, style = Stroke(tube * 2f))

    // Glass wall: the surface seen through tinted glass.
    val wall = glass.tint.copy(alpha = WALL_ALPHA).compositeOver(surface)
    drawCircle(wall, ring, c, style = Stroke(tube * 2f))

    // Liquid: bands of brighter and dimmer radium carried round by the flow.
    val hot = emission.copy(alpha = (LIQUID_ALPHA * bright).coerceIn(0f, 1f))
    val dim = emission.copy(alpha = (LIQUID_ALPHA * DIM * bright).coerceIn(0f, 1f))
    rotate(Math.toDegrees(phase.toDouble()).toFloat(), c) {
        drawCircle(
            Brush.sweepGradient(listOf(hot, dim, hot, dim, hot, dim, hot), c),
            ring, c, style = Stroke(tube * BORE),
        )
    }

    // Specular glints: on the outer wall facing the light and the inner wall facing it across the ring.
    val len = hypot(light.x, light.y)
    if (len > MIN_LEAN) {
        val toward = Math.toDegrees(atan2(light.y, light.x).toDouble()).toFloat()
        val glint = light.highlight.copy(alpha = (light.specular * glass.specular * GLINT).coerceIn(0f, 1f))
        arc(glint, toward, ring + tube * EDGE, tube * GLINT_WIDTH)
        arc(glint.copy(alpha = glint.alpha * INNER_GLINT), toward + HALF_TURN, ring - tube * EDGE, tube * GLINT_WIDTH)
    }
}

private fun DrawScope.arc(color: Color, centerDeg: Float, radius: Float, width: Float) {
    drawArc(
        color = color,
        startAngle = centerDeg - GLINT_SPAN / 2f,
        sweepAngle = GLINT_SPAN,
        useCenter = false,
        topLeft = center - Offset(radius, radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width, cap = StrokeCap.Round),
    )
}

/** How far the spilled light reaches either side of the ring (tube radii), and its peak alpha at glow 1. */
private const val SPILL_REACH = 3.5f
private const val SPILL_ALPHA = 0.22f
private const val GLOW_MAX = 1.6f
private const val SHADOW_ALPHA = 0.3f
private const val WALL_ALPHA = 0.55f
private const val LIQUID_ALPHA = 1f
private const val DIM = 0.82f
/** Bore width in tube radii: the liquid fills most of the tube, a thin glass wall shows around it. */
private const val BORE = 1.7f
private const val GLINT = 0.9f
private const val INNER_GLINT = 0.5f
private const val GLINT_WIDTH = 0.35f
private const val GLINT_SPAN = 38f
/** Where the glints sit across the wall, in tube radii from the centre line. */
private const val EDGE = 0.6f
private const val HALF_TURN = 180f
private const val MIN_LEAN = 1e-3f
