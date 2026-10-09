package com.halo.core.designsystem.foundation

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

/**
 * Light emitted by an element — Halo's replacement for drop shadows in dark mode. A blurred copy of
 * [shape], grown by [spread], painted behind the element. [intensity] is read at draw time, so animating
 * it never recomposes. The outline is rebuilt each draw (into a reused path), so a [shape] that morphs
 * (reads animated state in `createOutline`) is followed exactly.
 */
fun Modifier.haloGlow(
    color: Color,
    shape: Shape,
    radius: Dp,
    spread: Dp,
    intensity: () -> Float = { 1f },
): Modifier = drawWithCache {
    val blurPx = radius.toPx()
    val spreadPx = spread.toPx()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        if (blurPx > 0f) maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
    }
    val path = Path()
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
}
