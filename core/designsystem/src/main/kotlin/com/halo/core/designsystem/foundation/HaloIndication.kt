package com.halo.core.designsystem.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import com.halo.core.designsystem.theme.HaloFade
import com.halo.core.designsystem.theme.LocalHaloColors
import com.halo.core.designsystem.theme.LocalHaloContentColor
import com.halo.core.designsystem.theme.LocalHaloMotion
import com.halo.core.designsystem.theme.LocalHaloStroke
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

/**
 * Halo's replacement for the ripple. Keeps Android's touch-origin cue — a soft light blooms from the
 * exact press point — but as a radial glow instead of a hard-edged wave. Also draws the keyboard /
 * D-pad focus ring (2dp, offset 2dp outside the shape).
 *
 * The highlight uses the container's content color, so it lightens dark surfaces and darkens light fills.
 */
class HaloIndication(
    private val shape: Shape,
    private val highlight: Color = Color.Unspecified,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        HaloIndicationNode(interactionSource, shape, highlight)

    override fun equals(other: Any?): Boolean =
        other is HaloIndication && other.shape == shape && other.highlight == highlight

    override fun hashCode(): Int = 31 * shape.hashCode() + highlight.hashCode()
}

private class HaloIndicationNode(
    private val interactionSource: InteractionSource,
    private val shape: Shape,
    private val highlight: Color,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val press = Animatable(0f)
    private val focus = Animatable(0f)
    private var origin = Offset.Unspecified
    /** Refilled in place each frame of the press highlight. */
    private val clip = Path()

    override fun onAttach() {
        coroutineScope.trackPress(
            interactionSource = interactionSource,
            progress = press,
            motion = { currentValueOf(LocalHaloMotion) },
            onPressAt = { origin = it },
        )
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                val motion = currentValueOf(LocalHaloMotion)
                when (interaction) {
                    is FocusInteraction.Focus -> launch { focus.animateTo(1f, motion.fade(HaloFade.Fast)) }
                    is FocusInteraction.Unfocus -> launch { focus.animateTo(0f, motion.fade(HaloFade.Fast)) }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val p = press.value.coerceIn(0f, 1f)
        if (p > 0f) {
            val tint = highlight
                .takeOrElse { currentValueOf(LocalHaloContentColor) }
                .takeOrElse { Color.White }
            val center = if (origin.isSpecified) origin else Offset(size.width / 2f, size.height / 2f)
            val reach = farthestCorner(center, size)
            val radius = reach * (0.35f + 0.65f * p)
            val outline = shape.createOutline(size, layoutDirection, this)
            clip.reset()
            clip.addOutline(outline)
            clipPath(clip) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to tint.copy(alpha = 0.18f * p),
                        1f to tint.copy(alpha = 0.06f * p),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
        }
        val f = focus.value
        if (f > 0f) {
            val stroke = currentValueOf(LocalHaloStroke)
            val ringColor = currentValueOf(LocalHaloColors).line.focus
            val inset = stroke.focusOffset.toPx() + stroke.focus.toPx() / 2f
            val ringSize = Size(size.width + inset * 2f, size.height + inset * 2f)
            val ring = shape.createOutline(ringSize, layoutDirection, this)
            translate(-inset, -inset) {
                drawOutline(ring, ringColor.copy(alpha = f), style = Stroke(stroke.focus.toPx()))
            }
        }
    }

    private fun farthestCorner(from: Offset, size: Size): Float = max(
        max(hypot(from.x, from.y), hypot(size.width - from.x, from.y)),
        max(hypot(from.x, size.height - from.y), hypot(size.width - from.x, size.height - from.y)),
    )
}
