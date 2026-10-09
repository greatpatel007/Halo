package com.halo.core.designsystem.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import com.halo.core.designsystem.theme.LocalHaloMotion
import kotlinx.coroutines.Job

/** Default press depth for buttons and cards. */
const val HALO_PRESSED_SCALE = 0.97f

/** Press depth for full-width controls (a 3% squeeze on a 360dp element reads as too much). */
const val HALO_PRESSED_SCALE_WIDE = 0.98f

/**
 * Shrinks the element while [interactionSource] is pressed (Snap spring in, Quick spring out with a
 * hair of overshoot). Runs entirely in the layer phase — no recomposition per frame.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = HALO_PRESSED_SCALE,
): Modifier = this then PressScaleElement(interactionSource, pressedScale)

private data class PressScaleElement(
    val interactionSource: InteractionSource,
    val pressedScale: Float,
) : ModifierNodeElement<PressScaleNode>() {
    override fun create() = PressScaleNode(interactionSource, pressedScale)
    override fun update(node: PressScaleNode) = node.update(interactionSource, pressedScale)
    override fun InspectorInfo.inspectableProperties() {
        name = "pressScale"
        properties["pressedScale"] = pressedScale
    }
}

private class PressScaleNode(
    private var interactionSource: InteractionSource,
    var pressedScale: Float,
) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    private val progress = Animatable(0f)
    private var job: Job? = null

    override fun onAttach() = observe()

    fun update(source: InteractionSource, scale: Float) {
        pressedScale = scale
        if (source != interactionSource) {
            interactionSource = source
            job?.cancel()
            observe()
        }
    }

    private fun observe() {
        job = coroutineScope.trackPress(interactionSource, progress, { currentValueOf(LocalHaloMotion) })
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val s = 1f - (1f - pressedScale) * progress.value
                scaleX = s
                scaleY = s
            }
        }
    }
}
