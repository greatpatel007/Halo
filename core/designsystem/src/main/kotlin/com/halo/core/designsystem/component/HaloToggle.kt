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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.max
import com.halo.core.designsystem.foundation.HaloIndication
import com.halo.core.designsystem.foundation.HaloPreview
import com.halo.core.designsystem.foundation.material.Dish
import com.halo.core.designsystem.foundation.material.Recess
import com.halo.core.designsystem.foundation.material.ReliefPainter
import com.halo.core.designsystem.foundation.material.VelvetColors
import com.halo.core.designsystem.theme.HaloFade
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class HaloToggleTone {
    /** Blue — settings. */
    Accent,
    /** Orange — on-chain Bitcoin settings. */
    Bitcoin,
    /** Yellow — Lightning settings. */
    Lightning,
}

enum class HaloToggleSize { Medium, Large }

/**
 * The physical switch from `Refs/toggle.mp4`. A ceramic (light) or graphite (dark) thumb with one finger
 * dish slides in a sunken channel on the measured [HaloSpring.Mechanical] spring, leans under the finger,
 * and uncovers a velvet fill that brightens as it leaves the thumb's light-caused shadow.
 *
 * Every surface is rendered by the SDF lighting shaders in its own layer: the housing never redraws, the fill
 * redraws as the thumb moves, and the thumb is only *placed* (layout phase) — it repaints solely when pressed.
 *
 * Spec: docs/components/HaloToggle.md · Archetype: Toggle · Showroom: halo://showroom/toggle
 */
@Composable
fun HaloToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: HaloToggleTone = HaloToggleTone.Accent,
    size: HaloToggleSize = HaloToggleSize.Medium,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val motion = HaloTheme.motion
    val haptics = HaloTheme.haptics
    val light = HaloTheme.light
    val materials = HaloTheme.materials
    val velvet = velvetColors(tone)
    val housingSize = if (size == HaloToggleSize.Large) HaloTheme.sizes.switchLg else HaloTheme.sizes.switchMd
    val density = LocalDensity.current
    val geometry = remember(housingSize, density) {
        with(density) { ToggleGeometry(housingSize.width.roundToPx(), housingSize.height.roundToPx()) }
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val scope = rememberCoroutineScope()

    val position = remember { Animatable(if (checked) 1f else 0f) }
    val sink = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    val checkedNow by rememberUpdatedState(checked)
    val motionNow by rememberUpdatedState(motion)
    val onChange by rememberUpdatedState(onCheckedChange)

    LaunchedEffect(checked) {
        if (!dragging) position.animateTo(if (checked) 1f else 0f, motionNow.spring(HaloSpring.Mechanical))
    }
    // Press: the thumb sinks and leans toward the state it's about to take (measured anticipation).
    LaunchedEffect(source) {
        source.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    launch { sink.animateTo(1f, motionNow.spring(HaloSpring.Snap)) }
                    if (!motionNow.reduced && !dragging) {
                        val lean = if (checkedNow) 1f - LEAN else LEAN
                        launch { position.animateTo(lean, motionNow.spring(HaloSpring.Snap)) }
                    }
                }
                is PressInteraction.Release -> launch { sink.animateTo(0f, motionNow.spring(HaloSpring.Quick)) }
                is PressInteraction.Cancel -> {
                    launch { sink.animateTo(0f, motionNow.spring(HaloSpring.Quick)) }
                    if (!dragging) {
                        launch { position.animateTo(if (checkedNow) 1f else 0f, motionNow.spring(HaloSpring.Mechanical)) }
                    }
                }
            }
        }
    }

    val direction = if (rtl) -1f else 1f
    val dragState = rememberDraggableState { delta ->
        scope.launch { position.snapTo((position.value + direction * delta / geometry.travel).coerceIn(0f, 1f)) }
    }
    val focusRing = remember { HaloIndication(TOGGLE_SHAPE, highlight = Color.Transparent) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else HaloTheme.effects.disabledAlpha,
        animationSpec = motion.fade(HaloFade.Base),
        label = "toggleAlpha",
    )
    val housingPainter = remember { ReliefPainter() }
    val thumbPainter = remember { ReliefPainter() }

    Box(
        modifier = modifier.size(
            width = housingSize.width,
            height = max(housingSize.height, HaloTheme.sizes.minTouch),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(housingSize)
                .graphicsLayer { alpha = contentAlpha }
                .semantics { contentDescription?.let { this.contentDescription = it } }
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    interactionSource = source,
                    indication = focusRing,
                ) { value ->
                    haptics.perform(if (value) HaloHaptic.ToggleOn else HaloHaptic.ToggleOff)
                    onChange(value)
                }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    startDragImmediately = false,
                    onDragStarted = { dragging = true },
                    onDragStopped = { velocity ->
                        try {
                            val v = direction * velocity / geometry.travel
                            val target = position.value + v * VELOCITY_PROJECTION_S >= 0.5f
                            if (target != checkedNow) {
                                haptics.perform(if (target) HaloHaptic.ToggleOn else HaloHaptic.ToggleOff)
                                onChange(target)
                            }
                            position.animateTo(if (target) 1f else 0f, motionNow.spring(HaloSpring.Mechanical), initialVelocity = v)
                        } finally {
                            dragging = false
                        }
                    },
                ),
        ) {
            // Layer 1: housing + recessed channel + its contact shadow. Static.
            Canvas(Modifier.size(housingSize)) {
                val g = geometry
                housingPainter.drawContactShadow(this, g.housing, g.housingRadius, g.housingElevation, light, HOUSING_SHADOW)
                housingPainter.drawRelief(
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
            // Layer 2: velvet fill (texture) from the start edge to the thumb centre.
            Canvas(Modifier.size(housingSize)) {
                val g = geometry
                val centre = g.thumbLeft(position.value, rtl) + g.thumbWidth / 2f
                val from = if (rtl) centre else g.channel.left
                val to = if (rtl) g.channel.right else centre
                housingPainter.drawVelvet(this, g.recess(materials.recessFloor), from, to, velvet, materials.velvetGrain, light, FILL_SEED)
            }
            // Layer 3: thumb + its moving contact shadow. Placed, not redrawn, as it travels.
            val thumbSize = with(density) { DpSize(geometry.thumbWidth.toDp(), geometry.thumbHeight.toDp()) }
            Canvas(
                Modifier
                    .offset { IntOffset(geometry.thumbLeft(position.value, rtl).roundToInt(), geometry.thumbInset) }
                    .size(thumbSize),
            ) {
                val g = geometry
                val w = this.size.width
                val h = this.size.height
                val body = Rect(0f, 0f, w, h)
                val elevation = g.thumbElevation * (1f - (1f - PRESSED_ELEVATION) * sink.value.coerceIn(0f, 1f))
                thumbPainter.drawContactShadow(this, body, h / 2f, elevation, light, THUMB_SHADOW)
                thumbPainter.drawRelief(
                    scope = this,
                    bounds = body,
                    radius = h / 2f,
                    bevel = h * THUMB_BEVEL,
                    height = h * THUMB_RAISE,
                    material = materials.knob,
                    light = light,
                    dishA = Dish(Offset(w / 2f, h / 2f), h * DISH_RADIUS, h * DISH_DEPTH),
                    seed = THUMB_SEED,
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------- geometry

/** Proportions measured from the reference frame, in px of the housing height. */
private class ToggleGeometry(private val width: Int, height: Int) {
    private val h = height.toFloat()
    val housing = Rect(0f, 0f, width.toFloat(), h)
    val housingRadius = h / 2f
    val housingBevel = h * 0.16f
    val housingRaise = h * 0.10f
    val housingElevation = h * 0.06f

    private val channelInset = h * 0.20f
    val channel = Rect(channelInset, channelInset, width - channelInset, h - channelInset)
    private val channelBevel = h * 0.12f
    private val channelDepth = h * 0.18f

    val thumbInset = (h * 0.11f).roundToInt()
    val thumbHeight = height - thumbInset * 2
    val thumbWidth = thumbHeight
    val travel = (width - thumbInset * 2 - thumbWidth).toFloat()
    val thumbElevation = h * 0.16f

    fun thumbLeft(progress: Float, rtl: Boolean): Float {
        val p = progress.coerceIn(0f, 1f)
        return if (rtl) width - thumbInset - thumbWidth - p * travel else thumbInset + p * travel
    }

    fun recess(floor: Color) = Recess(channel, channel.height / 2f, channelBevel, channelDepth, floor)
}

// ------------------------------------------------------------------------------------------- tokens

/** Velvet derived from the tone's fill: lit top, shaded bottom, deep occluded rim. Uniform darkening keeps hue. */
@Composable
private fun velvetColors(tone: HaloToggleTone): VelvetColors {
    val c = HaloTheme.colors
    val fill = when (tone) {
        HaloToggleTone.Accent -> c.accent.fill
        HaloToggleTone.Bitcoin -> c.bitcoin.fill
        HaloToggleTone.Lightning -> c.lightning.fill
    }
    return remember(fill) {
        VelvetColors(
            top = fill,
            bottom = Color(fill.red * VELVET_SHADE, fill.green * VELVET_SHADE, fill.blue * VELVET_SHADE),
            rim = Color(fill.red * VELVET_RIM, fill.green * VELVET_RIM, fill.blue * VELVET_RIM),
        )
    }
}

private val TOGGLE_SHAPE = RoundedCornerShape(percent = 50)
private const val LEAN = 0.15f
private const val PRESSED_ELEVATION = 0.44f
private const val VELOCITY_PROJECTION_S = 0.12f
private const val HOUSING_SHADOW = 0.8f
private const val THUMB_SHADOW = 1f
private const val THUMB_BEVEL = 0.30f
private const val THUMB_RAISE = 0.18f
private const val DISH_RADIUS = 0.38f
private const val DISH_DEPTH = 0.12f
private const val VELVET_SHADE = 0.80f
private const val VELVET_RIM = 0.48f
private const val HOUSING_SEED = 3f
private const val FILL_SEED = 7f
private const val THUMB_SEED = 11f

// ------------------------------------------------------------------------------------------- previews

@Preview
@Composable
private fun HaloToggleStatesPreview() = HaloPreview {
    Row(horizontalArrangement = Arrangement.spacedBy(HaloTheme.spacing.lg)) {
        HaloToggle(checked = false, onCheckedChange = {})
        HaloToggle(checked = true, onCheckedChange = {})
        HaloToggle(checked = true, onCheckedChange = {}, enabled = false)
    }
}

@Preview
@Composable
private fun HaloToggleTonesPreview() = HaloPreview {
    Row(horizontalArrangement = Arrangement.spacedBy(HaloTheme.spacing.lg)) {
        HaloToggle(checked = true, onCheckedChange = {}, tone = HaloToggleTone.Bitcoin)
        HaloToggle(checked = true, onCheckedChange = {}, tone = HaloToggleTone.Lightning, size = HaloToggleSize.Large)
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        HaloToggle(checked = true, onCheckedChange = {})
    }
}
