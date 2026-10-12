package com.halo.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.halo.core.designsystem.foundation.HaloIndication
import com.halo.core.designsystem.foundation.HaloPaymentNetwork
import com.halo.core.designsystem.foundation.HaloPreview
import com.halo.core.designsystem.foundation.material.Recess
import com.halo.core.designsystem.foundation.material.ReliefPainter
import com.halo.core.designsystem.foundation.networkColors
import com.halo.core.designsystem.icons.HaloIcons
import com.halo.core.designsystem.theme.HaloFade
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** One option of a [HaloSegmentedControl]. [network] colours its selection (null = accent, a UI choice). */
@Immutable
class HaloSegment(
    val label: String,
    val icon: ImageVector? = null,
    val network: HaloPaymentNetwork? = null,
)

enum class HaloSegmentedSize { Medium, Large }

/**
 * A selector you can grab: the toggle's ceramic / graphite thumb slides along a sunken track on the measured
 * [HaloSpring.Mechanical] spring, clicks into a detent at every segment, compresses against the wall when pushed past
 * an end, and the label it rests under takes the colour of what it means (accent, on-chain orange, Lightning yellow).
 *
 * The track never redraws; the thumb is translated (not repainted) as it travels; label colours follow the thumb in
 * the draw phase. Selection commits on release: [onSelect] fires once per gesture.
 *
 * Spec: docs/components/HaloSegmentedControl.md · Archetype: Toggle · Showroom: halo://showroom/segmented
 *
 * @param selectedIndex hoisted selection; clamped for drawing.
 * @param modifier give it a width (usually `fillMaxWidth()`): segments share it equally.
 */
@Composable
fun HaloSegmentedControl(
    segments: List<HaloSegment>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: HaloSegmentedSize = HaloSegmentedSize.Medium,
) {
    val count = segments.size.coerceAtLeast(1)
    val last = (count - 1).toFloat()
    val selected = selectedIndex.coerceIn(0, count - 1)
    val motion = HaloTheme.motion
    val haptics = HaloTheme.haptics
    val light = HaloTheme.light
    val materials = HaloTheme.materials
    val colors = HaloTheme.colors
    val spacing = HaloTheme.spacing
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val scope = rememberCoroutineScope()

    val nominal = if (size == HaloSegmentedSize.Large) HaloTheme.sizes.controlMd else HaloTheme.sizes.controlSm
    val nominalPx = with(density) { nominal.toPx() }
    val thumbInsetDp = nominal * THUMB_INSET
    val labelPadPx = with(density) { (thumbInsetDp + spacing.xs).toPx() }

    val position = remember { Animatable(selected.toFloat()) }
    val sink = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragRaw by remember { mutableFloatStateOf(0f) }
    // What is moving the thumb: a tap (its target index), a drag (incl. the throw after it), or nothing (passive).
    var travel by remember { mutableIntStateOf(PASSIVE) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    var boxLeft by remember { mutableFloatStateOf(0f) }
    var labelHeight by remember { mutableIntStateOf(0) }
    val selectedNow by rememberUpdatedState(selected)
    val motionNow by rememberUpdatedState(motion)
    val onSelectNow by rememberUpdatedState(onSelect)

    val geometry = remember(box, labelHeight, nominalPx, labelPadPx, count) {
        SegmentGeometry(
            width = box.width.toFloat(),
            height = box.height.toFloat(),
            housingHeight = max(nominalPx, labelHeight + 2f * labelPadPx),
            nominal = nominalPx,
            count = count,
        )
    }
    val direction = if (rtl) -1f else 1f
    fun pan(index: Int): Float {
        if (windowWidth <= 0) return 0f
        val centre = boxLeft + geometry.thumbLeft(index.toFloat(), rtl) + geometry.thumbWidth / 2f
        return (centre / windowWidth * 2f - 1f).coerceIn(-1f, 1f)
    }

    // Selection from outside (or after a tap) → the thumb travels, unless a finger holds it.
    LaunchedEffect(selected, count) {
        if (!dragging) {
            position.animateTo(selected.toFloat(), motionNow.spring(HaloSpring.Mechanical))
            travel = PASSIVE
        }
    }
    // Detents: one click per half-way crossing, only for the user's own motion (never for passive updates).
    // A tap already clicked for its target, so the detents it passes on the way click softer.
    LaunchedEffect(count) {
        snapshotFlow { SegmentPhysics.nearest(position.value, count) }
            .drop(1)
            .collect { detent ->
                val moving = travel
                when {
                    moving == DRAG -> haptics.perform(HaloHaptic.Tick, pan(detent))
                    moving >= 0 && detent != moving && !motionNow.reduced -> haptics.tick(PASSING_TICK)
                }
            }
    }
    LaunchedEffect(enabled) {
        if (!enabled && dragging) {
            dragging = false
            launch { sink.animateTo(0f, motionNow.spring(HaloSpring.Quick)) }
            position.animateTo(selectedNow.toFloat(), motionNow.spring(HaloSpring.Mechanical))
        }
    }

    val dragState = rememberDraggableState { delta ->
        if (!dragging) return@rememberDraggableState
        dragRaw += direction * delta / geometry.thumbWidth
        val shown = SegmentPhysics.rubberBand(dragRaw, count)
        scope.launch { position.snapTo(shown) }
    }
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else HaloTheme.effects.disabledAlpha,
        animationSpec = motion.fade(HaloFade.Base),
        label = "segmentedAlpha",
    )
    val trackPainter = remember { ReliefPainter() }
    val thumbPainter = remember { ReliefPainter() }
    val focusRing = remember { HaloIndication(SEGMENT_SHAPE, highlight = Color.Transparent) }
    val tints = segments.map { segmentTint(it) }
    val resting = colors.content.secondary
    val labelStyle = HaloTheme.typography.label
    val iconSize = HaloTheme.sizes.iconSm

    Box(
        modifier
            .heightIn(min = HaloTheme.sizes.minTouch)
            .graphicsLayer { this.alpha = alpha }
            .onSizeChanged { box = it }
            .onGloballyPositioned { boxLeft = it.positionInWindow().x }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                enabled = enabled && count > 1,
                startDragImmediately = false,
                onDragStarted = { start ->
                    if (geometry.thumbRect(position.value, rtl).inflate(geometry.grabSlop).contains(start)) {
                        position.stop()
                        dragRaw = position.value
                        travel = DRAG
                        dragging = true
                        launch { sink.animateTo(1f, motionNow.spring(HaloSpring.Snap)) }
                    }
                },
                onDragStopped = { velocity ->
                    if (!dragging) return@draggable
                    try {
                        val v = direction * velocity / geometry.thumbWidth
                        val target = SegmentPhysics.target(position.value, v, count)
                        dragging = false
                        launch { sink.animateTo(0f, motionNow.spring(HaloSpring.Quick)) }
                        if (target != selectedNow) onSelectNow(target)
                        position.animateTo(target.toFloat(), motionNow.spring(HaloSpring.Mechanical), initialVelocity = v)
                        travel = PASSIVE
                    } finally {
                        dragging = false
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Layer 1: housing + sunken channel + its contact shadow. Static (repaints only when the light moves).
        Canvas(Modifier.matchParentSize()) {
            val g = geometry
            if (g.width <= 0f) return@Canvas
            trackPainter.drawContactShadow(this, g.housing, g.housingRadius, g.housingElevation, light, HOUSING_SHADOW)
            trackPainter.drawRelief(
                scope = this,
                bounds = g.housing,
                radius = g.housingRadius,
                bevel = g.housingBevel,
                height = g.housingRaise,
                material = materials.body,
                light = light,
                recess = g.recess(materials.recessFloor),
                seed = HOUSING_SEED,
            )
        }
        // Layer 2: the thumb, drawn once at segment 0 and *translated* along the track; past an end it compresses
        // against the wall (scaleX, pivot on the wall). It repaints only when it sinks or the light moves.
        Canvas(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val g = geometry
                    if (g.width <= 0f) return@graphicsLayer
                    val p = position.value
                    val at = p.coerceIn(0f, last)
                    translationX = g.thumbLeft(at, rtl) - g.thumbLeft(0f, rtl)
                    val squash = SegmentPhysics.squash(p, count)
                    scaleX = 1f - squash
                    // The wall it's pressed against: the end it overshot, mirrored in RTL.
                    val pushingEnd = p > last
                    val wallOnRight = pushingEnd != rtl
                    val base = g.thumbLeft(0f, rtl)
                    val pivot = if (wallOnRight) base + g.thumbWidth else base
                    transformOrigin = TransformOrigin(pivot / g.width, 0.5f)
                },
        ) {
            val g = geometry
            if (g.width <= 0f) return@Canvas
            val body = g.thumbRect(0f, rtl)
            val elevation = g.thumbElevation * (1f - (1f - PRESSED_ELEVATION) * sink.value.coerceIn(0f, 1f))
            thumbPainter.drawContactShadow(this, body, body.height / 2f, elevation, light, THUMB_SHADOW)
            thumbPainter.drawRelief(
                scope = this,
                bounds = body,
                radius = body.height / 2f,
                bevel = body.height * THUMB_BEVEL,
                height = body.height * THUMB_RAISE,
                material = materials.knob,
                light = light,
                seed = THUMB_SEED,
            )
        }
        // Layer 3: the segments: tap targets over the full height, labels coloured by the thumb's distance (draw phase).
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = HaloTheme.sizes.minTouch)
                .padding(horizontal = thumbInsetDp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            segments.forEachIndexed { index, segment ->
                val source = remember { MutableInteractionSource() }
                LaunchedEffect(source) {
                    source.interactions.collect { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> when {
                                dragging -> Unit
                                index == selectedNow -> launch { sink.animateTo(1f, motionNow.spring(HaloSpring.Snap)) }
                                !motionNow.reduced -> {
                                    val lean = selectedNow + LEAN * (if (index > selectedNow) 1f else -1f)
                                    launch { position.animateTo(lean, motionNow.spring(HaloSpring.Snap)) }
                                }
                            }
                            is PressInteraction.Release -> launch { sink.animateTo(0f, motionNow.spring(HaloSpring.Quick)) }
                            is PressInteraction.Cancel -> {
                                launch { sink.animateTo(0f, motionNow.spring(HaloSpring.Quick)) }
                                if (!dragging) {
                                    launch { position.animateTo(selectedNow.toFloat(), motionNow.spring(HaloSpring.Mechanical)) }
                                }
                            }
                        }
                    }
                }
                val tint = tints[index]
                val color = { lerp(resting, tint, SegmentPhysics.weight(position.value, index, count)) }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = HaloTheme.sizes.minTouch)
                        .selectable(
                            selected = index == selected,
                            enabled = enabled,
                            role = Role.Tab,
                            interactionSource = source,
                            indication = focusRing,
                        ) {
                            if (index != selectedNow) {
                                travel = index
                                haptics.perform(HaloHaptic.Tick, pan(index))
                                onSelectNow(index)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        Modifier.padding(horizontal = spacing.sm, vertical = with(density) { labelPadPx.toDp() }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        segment.icon?.let { icon ->
                            val painter = rememberVectorPainter(icon)
                            Canvas(Modifier.size(iconSize)) {
                                with(painter) { draw(this@Canvas.size, colorFilter = ColorFilter.tint(color())) }
                            }
                            Spacer(Modifier.width(spacing.xs))
                        }
                        BasicText(
                            text = segment.label,
                            style = labelStyle,
                            color = color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { layout ->
                                if (layout.size.height > labelHeight) labelHeight = layout.size.height
                            },
                        )
                    }
                }
            }
        }
    }
}

/** The colour a segment's label takes under the thumb: its network's, else the accent. */
@Composable
private fun segmentTint(segment: HaloSegment): Color =
    segment.network?.let { networkColors(it).text } ?: HaloTheme.colors.accent.text

// ------------------------------------------------------------------------------------------- physics

/** Pure motion model of the thumb, in segment units (0 … count − 1). Unit-tested in `SegmentPhysicsTest`. */
internal object SegmentPhysics {
    /** A release carries the thumb this long at its release speed (as the toggle). */
    const val PROJECTION_S = 0.12f
    /** Past an end the thumb can be pulled at most this many segments (Apple's rubber band approaches it). */
    const val OVERSHOOT = 0.24f
    /** Share of the overshoot shown as compression against the wall (the rest is resisted away). */
    const val SQUASH = 0.5f

    /** The detent the thumb is in: the nearest segment, clamped to the track. */
    fun nearest(position: Float, count: Int): Int = position.roundToInt().coerceIn(0, (count - 1).coerceAtLeast(0))

    /** Where a release at [position] moving [velocity] segments/s comes to rest. */
    fun target(position: Float, velocity: Float, count: Int): Int = nearest(position + velocity * PROJECTION_S, count)

    /** The thumb follows [raw] along the track and resists beyond either end. */
    fun rubberBand(raw: Float, count: Int): Float =
        SheetPhysics.rubberBand(raw, 0f, (count - 1).coerceAtLeast(0).toFloat(), OVERSHOOT)

    /** How much the thumb is compressed against the wall at [position] (0 inside the track, < [OVERSHOOT]·[SQUASH]). */
    fun squash(position: Float, count: Int): Float {
        val last = (count - 1).coerceAtLeast(0).toFloat()
        val overshoot = when {
            position < 0f -> -position
            position > last -> position - last
            else -> 0f
        }
        return overshoot * SQUASH
    }

    /** How strongly segment [index] shows its colour with the thumb at [position]: 1 under it, 0 a segment away. */
    fun weight(position: Float, index: Int, count: Int): Float {
        val p = position.coerceIn(0f, (count - 1).coerceAtLeast(0).toFloat())
        return (1f - abs(p - index)).coerceIn(0f, 1f)
    }
}

// ------------------------------------------------------------------------------------------- geometry

/** Track proportions, in px of the nominal height (the toggle's material language at selector scale). */
private class SegmentGeometry(
    val width: Float,
    height: Float,
    housingHeight: Float,
    nominal: Float,
    count: Int,
) {
    private val top = (height - housingHeight) / 2f
    val housing = Rect(0f, top, width, top + housingHeight)
    val housingRadius = housingHeight / 2f
    val housingBevel = nominal * 0.10f
    val housingRaise = nominal * 0.06f
    val housingElevation = nominal * 0.04f

    private val channelInset = nominal * CHANNEL_INSET
    private val channel = Rect(channelInset, top + channelInset, width - channelInset, top + housingHeight - channelInset)
    private val channelBevel = nominal * 0.10f
    private val channelDepth = nominal * 0.14f

    private val thumbInset = nominal * THUMB_INSET
    private val thumbTop = top + thumbInset
    private val thumbHeight = housingHeight - thumbInset * 2f
    val thumbWidth = ((width - thumbInset * 2f) / count).coerceAtLeast(1f)
    val thumbElevation = nominal * 0.14f
    /** A drag may start this close to the thumb (fingers are wider than its edge). */
    val grabSlop = nominal * 0.25f

    fun thumbLeft(position: Float, rtl: Boolean): Float =
        if (rtl) width - thumbInset - (position + 1f) * thumbWidth else thumbInset + position * thumbWidth

    fun thumbRect(position: Float, rtl: Boolean): Rect {
        val left = thumbLeft(position, rtl)
        return Rect(left, thumbTop, left + thumbWidth, thumbTop + thumbHeight)
    }

    fun recess(floor: Color) = Recess(channel, channel.height / 2f, channelBevel, channelDepth, floor)
}

// ------------------------------------------------------------------------------------------- tokens

private val SEGMENT_SHAPE = RoundedCornerShape(percent = 50)
private const val PASSIVE = -1
private const val DRAG = -2
private const val CHANNEL_INSET = 0.08f
private const val THUMB_INSET = 0.13f
private const val LEAN = 0.15f
private const val PASSING_TICK = 0.45f
private const val PRESSED_ELEVATION = 0.43f
private const val HOUSING_SHADOW = 0.6f
private const val THUMB_SHADOW = 1f
private const val THUMB_BEVEL = 0.30f
private const val THUMB_RAISE = 0.18f
private const val HOUSING_SEED = 5f
private const val THUMB_SEED = 13f

// ------------------------------------------------------------------------------------------- previews

@Preview
@Composable
private fun HaloSegmentedControlPreview() = HaloPreview {
    Column(verticalArrangement = Arrangement.spacedBy(HaloTheme.spacing.lg)) {
        HaloSegmentedControl(
            segments = listOf(HaloSegment("sats"), HaloSegment("BTC"), HaloSegment("USD")),
            selectedIndex = 0,
            onSelect = {},
            modifier = Modifier.fillMaxWidth(),
        )
        HaloSegmentedControl(
            segments = listOf(
                HaloSegment("On-chain", HaloIcons.Bitcoin, HaloPaymentNetwork.Onchain),
                HaloSegment("Lightning", HaloIcons.Bolt, HaloPaymentNetwork.Lightning),
            ),
            selectedIndex = 1,
            onSelect = {},
            modifier = Modifier.fillMaxWidth(),
            size = HaloSegmentedSize.Large,
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HaloSegmentedControl(
                segments = listOf(HaloSegment("Slow"), HaloSegment("Normal"), HaloSegment("Fast")),
                selectedIndex = 2,
                onSelect = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
        }
    }
}
