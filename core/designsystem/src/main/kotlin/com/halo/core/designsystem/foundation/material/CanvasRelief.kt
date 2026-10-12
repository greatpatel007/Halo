package com.halo.core.designsystem.foundation.material

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
import androidx.compose.ui.graphics.drawscope.clipRect
import com.halo.core.designsystem.theme.HaloLight
import kotlin.math.hypot

/**
 * Relief without shaders (Android 8–12), lit by the same scene light. Flat faces are the token colour exactly, as in
 * the AGSL path. The bevel catches the highlight on the side facing the light and falls into shadow on the far side.
 * A recess does the opposite: its near wall is shaded, its far wall lit. Contact shadows are stacked soft rounded
 * rects, which approximate a Gaussian blur on any GPU.
 */
internal class CanvasRelief : ReliefRenderer {
    private val clip = Path()

    override fun drawRelief(
        scope: DrawScope,
        bounds: Rect,
        radius: Float,
        bevel: Float,
        height: Float,
        material: ReliefMaterial,
        light: HaloLight,
        recess: Recess?,
        dishA: Dish?,
        dishB: Dish?,
        seed: Float,
    ): Unit = with(scope) {
        drawRoundRect(material.base, bounds.topLeft, bounds.size, CornerRadius(radius))
        val rim = (light.specular * material.specular * BEVEL_HIGHLIGHT).coerceIn(0f, 1f)
        val shade = light.shadowOpacity * BEVEL_SHADE
        bevel(bounds, radius, bevel, light, lit = light.highlight.copy(alpha = rim), shaded = light.shadow.copy(alpha = shade))
        if (recess != null) {
            val r = recess.bounds
            drawRoundRect(recess.floor, r.topLeft, r.size, CornerRadius(recess.radius))
            val wall = (light.shadowOpacity * material.ambientOcclusion * RECESS_SHADE).coerceIn(0f, 1f)
            // Sunk: the wall nearest the light is in its own shadow, the far wall catches it.
            bevel(r, recess.radius, recess.bevel.coerceAtLeast(MIN_WALL), light,
                lit = light.shadow.copy(alpha = wall), shaded = light.highlight.copy(alpha = rim * RECESS_RIM))
        }
        dishA?.let { dish(it, light, rim, shade) }
        dishB?.let { dish(it, light, rim, shade) }
    }

    override fun drawVelvet(
        scope: DrawScope,
        channel: Recess,
        from: Float,
        to: Float,
        colors: VelvetColors,
        grain: Float,
        light: HaloLight,
        seed: Float,
    ): Unit = with(scope) {
        if (to <= from) return
        val b = channel.bounds
        clip.reset()
        clip.addRoundRect(RoundRect(b, CornerRadius(channel.radius)))
        clipPath(clip) {
            clipRect(from, b.top, to, b.bottom) {
                drawRect(Brush.verticalGradient(listOf(colors.top, colors.bottom), b.top, b.bottom), b.topLeft, b.size)
                drawRoundRect(
                    colors.rim.copy(alpha = colors.rim.alpha * VELVET_RIM_STRENGTH),
                    b.topLeft, b.size, CornerRadius(channel.radius),
                    style = Stroke(channel.bevel.coerceAtLeast(MIN_WALL)),
                )
            }
        }
    }

    override fun drawContactShadow(
        scope: DrawScope,
        caster: Rect,
        radius: Float,
        elevation: Float,
        light: HaloLight,
        strength: Float,
    ): Unit = with(scope) {
        val offset = Offset(light.shadowDx * elevation, light.shadowDy * elevation)
        val blur = elevation * SHADOW_SOFTNESS + SHADOW_MIN_BLUR
        val opacity = (light.shadowOpacity * strength).coerceIn(0f, 1f)
        val core = caster.translate(offset)
        // Each layer grows by one step; together their alpha adds up to the shadow's opacity under the caster and
        // fades out over the blur radius.
        for (i in SHADOW_LAYERS downTo 1) {
            val spread = blur * i / SHADOW_LAYERS
            val r = core.inflate(spread - blur / 2f)
            drawRoundRect(
                light.shadow.copy(alpha = opacity / SHADOW_LAYERS),
                r.topLeft, r.size, CornerRadius((radius + spread - blur / 2f).coerceAtLeast(0f)),
            )
        }
    }

    /**
     * A bevel ring of width [width] inside [bounds], graded from [lit] on the light's side to [shaded] opposite. It is
     * feathered towards the face (concentric rings fading inwards), so its inner edge never reads as an outline.
     */
    private fun DrawScope.bevel(bounds: Rect, radius: Float, width: Float, light: HaloLight, lit: Color, shaded: Color) {
        val len = hypot(light.x, light.y)
        if (len < MIN_LEAN || width <= 0f) return
        val reach = hypot(bounds.width, bounds.height) / 2f
        val toLight = Offset(light.x / len, light.y / len) * reach
        val ring = width / BEVEL_RINGS
        for (i in 0 until BEVEL_RINGS) {
            val fade = 1f - i.toFloat() / BEVEL_RINGS
            val inset = bounds.deflate(ring * i + ring / 2f)
            drawRoundRect(
                Brush.linearGradient(
                    listOf(lit.copy(alpha = lit.alpha * fade), Color.Transparent, shaded.copy(alpha = shaded.alpha * fade)),
                    bounds.center + toLight,
                    bounds.center - toLight,
                ),
                inset.topLeft, inset.size, CornerRadius((radius - ring * i - ring / 2f).coerceAtLeast(0f)),
                style = Stroke(ring),
            )
        }
    }

    /** A concave finger rest: shaded towards the light, lit on the far rim. */
    private fun DrawScope.dish(dish: Dish, light: HaloLight, rim: Float, shade: Float) {
        val len = hypot(light.x, light.y)
        if (len < MIN_LEAN) return
        val toLight = Offset(light.x / len, light.y / len) * dish.radius
        drawCircle(
            Brush.linearGradient(
                listOf(light.shadow.copy(alpha = shade), Color.Transparent, light.highlight.copy(alpha = rim)),
                dish.center + toLight, dish.center - toLight,
            ),
            dish.radius, dish.center,
        )
    }

    private companion object {
        const val BEVEL_HIGHLIGHT = 0.9f
        const val BEVEL_SHADE = 0.6f
        const val RECESS_SHADE = 1.6f
        const val RECESS_RIM = 0.35f
        const val MIN_WALL = 1f
        const val MIN_LEAN = 1e-3f
        const val SHADOW_LAYERS = 12
        const val BEVEL_RINGS = 8
    }
}
