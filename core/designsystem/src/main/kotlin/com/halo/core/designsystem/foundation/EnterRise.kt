package com.halo.core.designsystem.foundation

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
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
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.LocalHaloMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Standard enter choreography: rise 12dp + fade in on the Gentle spring, staggered 30ms per [index]
 * (capped at 6). Runs once when the element first attaches.
 *
 * Use on static page content only — inside lazy lists it would replay as items scroll into view.
 */
fun Modifier.enterRise(index: Int = 0): Modifier = this then EnterRiseElement(index)

private data class EnterRiseElement(val index: Int) : ModifierNodeElement<EnterRiseNode>() {
    override fun create() = EnterRiseNode(index)
    override fun update(node: EnterRiseNode) = Unit
    override fun InspectorInfo.inspectableProperties() {
        name = "enterRise"
        properties["index"] = index
    }
}

private class EnterRiseNode(private val index: Int) :
    Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    private val progress = Animatable(0f)

    // A one-shot entrance: the motion tokens at the moment the element appears are the right ones.
    @SuppressLint("SuspiciousCompositionLocalModifierRead")
    override fun onAttach() {
        val motion = currentValueOf(LocalHaloMotion)
        coroutineScope.launch {
            delay(motion.staggerDelayMillis(index).toLong())
            progress.animateTo(1f, motion.spring(HaloSpring.Gentle))
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        val rise = currentValueOf(LocalHaloMotion).enterRise.toPx()
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val p = progress.value
                alpha = p.coerceIn(0f, 1f)
                translationY = (1f - p) * rise
            }
        }
    }
}
