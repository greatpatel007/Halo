package com.halo.core.designsystem.foundation

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp

/**
 * Light emitted by an element — Halo's replacement for drop shadows in dark mode. A blurred copy of
 * [shape], grown by [spread], painted behind the element. [intensity] is read at draw time, so animating
 * it never recomposes. The outline is rebuilt each draw (into a reused path), so a [shape] that morphs
 * (reads animated state in `createOutline`) is followed exactly. Recomposing with equal arguments keeps the
 * cached paint and draw.
 */
fun Modifier.haloGlow(
    color: Color,
    shape: Shape,
    radius: Dp,
    spread: Dp,
    intensity: () -> Float = FullIntensity,
): Modifier = this then GlowElement(color, shape, radius, spread, intensity)

private val FullIntensity: () -> Float = { 1f }

private data class GlowElement(
    val color: Color,
    val shape: Shape,
    val radius: Dp,
    val spread: Dp,
    val intensity: () -> Float,
) : ModifierNodeElement<GlowNode>() {
    override fun create() = GlowNode(color, shape, radius, spread, intensity)
    override fun update(node: GlowNode) = node.update(color, shape, radius, spread, intensity)
    override fun InspectorInfo.inspectableProperties() {
        name = "haloGlow"
        properties["color"] = color
        properties["shape"] = shape
        properties["radius"] = radius
        properties["spread"] = spread
    }
}

private class GlowNode(
    private var color: Color,
    private var shape: Shape,
    private var radius: Dp,
    private var spread: Dp,
    private var intensity: () -> Float,
) : DelegatingNode() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val draw = delegate(
        CacheDrawModifierNode {
            val blurPx = radius.toPx()
            val spreadPx = spread.toPx()
            paint.maskFilter = if (blurPx > 0f) BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL) else null
            val grown = Size(size.width + spreadPx * 2f, size.height + spreadPx * 2f)
            onDrawBehind {
                val a = intensity().coerceIn(0f, 1f)
                if (a > 0f) {
                    path.reset()
                    path.addOutline(shape.createOutline(grown, layoutDirection, this))
                    paint.color = color.copy(alpha = color.alpha * a).toArgb()
                    drawIntoCanvas { canvas ->
                        val native = canvas.nativeCanvas
                        val save = native.save()
                        native.translate(-spreadPx, -spreadPx)
                        native.drawPath(path.asAndroidPath(), paint)
                        native.restoreToCount(save)
                    }
                }
            }
        },
    )

    fun update(color: Color, shape: Shape, radius: Dp, spread: Dp, intensity: () -> Float) {
        if (color == this.color && shape == this.shape && radius == this.radius && spread == this.spread &&
            intensity === this.intensity
        ) {
            return
        }
        this.color = color
        this.shape = shape
        this.radius = radius
        this.spread = spread
        this.intensity = intensity
        draw.invalidateDrawCache()
    }
}
