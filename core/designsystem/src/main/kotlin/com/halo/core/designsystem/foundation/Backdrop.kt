package com.halo.core.designsystem.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp

/**
 * Hand-built backdrop blur (no third-party libraries). The [backdropSource] records its content into a
 * [GraphicsLayer] every frame; any [backdropGlass] sibling drawn above it re-draws that layer, offset to
 * its own position, through a RenderEffect blur and clipped to its shape.
 *
 * Rule: a glass element must never be inside its own source (it would record itself).
 */
@Stable
class HaloBackdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var sourcePosition by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberHaloBackdrop(): HaloBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { HaloBackdrop(layer) }
}

/** Marks the content that glass surfaces blur. Usually the screen's scrolling content. */
fun Modifier.backdropSource(backdrop: HaloBackdrop): Modifier = this
    .onGloballyPositioned { backdrop.sourcePosition = it.positionInRoot() }
    .drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }

/** Paints the blurred [backdrop] behind this element's content, clipped to [shape]. */
fun Modifier.backdropGlass(backdrop: HaloBackdrop, shape: Shape, blurRadius: Dp): Modifier =
    this then BackdropGlassElement(backdrop, shape, blurRadius)

private data class BackdropGlassElement(
    val backdrop: HaloBackdrop,
    val shape: Shape,
    val blurRadius: Dp,
) : ModifierNodeElement<BackdropGlassNode>() {
    override fun create() = BackdropGlassNode(backdrop, shape, blurRadius)
    override fun update(node: BackdropGlassNode) {
        node.backdrop = backdrop
        node.shape = shape
        node.blurRadius = blurRadius
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "backdropGlass"
        properties["blurRadius"] = blurRadius
    }
}

private class BackdropGlassNode(
    var backdrop: HaloBackdrop,
    var shape: Shape,
    var blurRadius: Dp,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private var layer: GraphicsLayer? = null
    private var position by mutableStateOf(Offset.Zero)
    // Reused every frame: the clip path is refilled in place, the blur is rebuilt only when its radius changes.
    private val clip = Path()
    private var blur: BlurEffect? = null
    private var blurPx = 0f

    override fun onAttach() {
        layer = requireGraphicsContext().createGraphicsLayer()
    }

    override fun onDetach() {
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        position = coordinates.positionInRoot()
    }

    override fun ContentDrawScope.draw() {
        val glass = layer
        if (glass != null) {
            val delta = backdrop.sourcePosition - position
            val radius = blurRadius.toPx()
            if (radius != blurPx) {
                blurPx = radius
                blur = if (radius > 0f) BlurEffect(radius, radius, TileMode.Clamp) else null
            }
            glass.renderEffect = blur
            glass.record {
                translate(delta.x, delta.y) { drawLayer(backdrop.layer) }
            }
            clip.reset()
            clip.addOutline(shape.createOutline(size, layoutDirection, this))
            clipPath(clip) { drawLayer(glass) }
        }
        drawContent()
    }
}
