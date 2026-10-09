package com.halo.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.halo.core.designsystem.foundation.AmountFormat
import com.halo.core.designsystem.foundation.HALO_PRESSED_SCALE_WIDE
import com.halo.core.designsystem.foundation.HaloIndication
import com.halo.core.designsystem.foundation.HaloPreview
import com.halo.core.designsystem.foundation.HaloSpinner
import com.halo.core.designsystem.foundation.HaloPaymentNetwork
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.networkColors
import com.halo.core.designsystem.foundation.haloGlow
import com.halo.core.designsystem.foundation.pressScale
import com.halo.core.designsystem.icons.HaloIcons
import com.halo.core.designsystem.theme.HaloEasing
import com.halo.core.designsystem.theme.HaloFade
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Payment lifecycle, owned by the caller (typically mapped from a feature ViewModel). */
enum class HoldToSendState {
    /** Waiting for the hold. */
    Idle,
    /** Committed; the backend is working. */
    Sending,
    /** Confirmed. */
    Success,
}

/**
 * The commit control for outgoing Bitcoin payments. Press and hold: an orange charge stretches across the
 * pill over [HOLD_MS] with rising haptic ticks (the label inverts as the fill passes under it). Releasing
 * early springs it back. At 100% it locks with a Threshold click, calls [onCommit] and collapses into a
 * circle — spinner while [state] is Sending, then a self-drawing check and a spark burst on Success.
 *
 * [network] sets the colour family: on-chain = Bitcoin orange, Lightning = electric cyan (decision D8).
 *
 * Spec: docs/components/HaloHoldToSend.md · Archetype: Gesture-commit · Showroom: halo://showroom/hold-to-send
 *
 * Fills its width. TalkBack users commit via the click / "Send" action instead of holding.
 */
@Composable
fun HaloHoldToSend(
    sats: Long,
    state: HoldToSendState,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Hold to send",
    network: HaloPaymentNetwork = HaloPaymentNetwork.Onchain,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val colors = HaloTheme.colors
    val family = networkColors(network)
    val motion = HaloTheme.motion
    val haptics = HaloTheme.haptics
    val effects = HaloTheme.effects
    val sizes = HaloTheme.sizes
    val spacing = HaloTheme.spacing
    val stroke = HaloTheme.stroke
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val charge = remember { Animatable(if (state == HoldToSendState.Idle) 0f else 1f) }
    val collapse = remember { Animatable(if (state == HoldToSendState.Idle) 0f else 1f) }
    val check = remember { Animatable(if (state == HoldToSendState.Success) 1f else 0f) }
    val burst = remember { Animatable(0f) }
    var locked by remember { mutableStateOf(state != HoldToSendState.Idle) }
    val chargeJob = remember { JobSlot() }
    val previousState = remember { StateSlot(state) }

    val interactive = enabled && !locked && state == HoldToSendState.Idle
    val interactiveNow by rememberUpdatedState(interactive)
    val motionNow by rememberUpdatedState(motion)
    val commitNow by rememberUpdatedState(onCommit)

    fun lockAndCommit() {
        if (locked) return
        locked = true
        haptics.perform(HaloHaptic.Threshold)
        commitNow()
    }

    // Lock ⇄ unlock choreography.
    LaunchedEffect(locked) {
        if (locked) {
            charge.snapTo(1f)
            collapse.animateTo(1f, motionNow.spring(HaloSpring.Standard))
        } else {
            check.snapTo(0f)
            burst.snapTo(0f)
            launch { charge.animateTo(0f, motionNow.spring(HaloSpring.Quick)) }
            collapse.animateTo(0f, motionNow.spring(HaloSpring.Standard))
        }
    }
    // Follow the caller's lifecycle.
    LaunchedEffect(state) {
        val previous = previousState.value
        previousState.value = state
        when (state) {
            HoldToSendState.Idle -> locked = false
            HoldToSendState.Sending -> locked = true
            HoldToSendState.Success -> {
                locked = true
                if (previous != HoldToSendState.Success) {
                    haptics.perform(HaloHaptic.Celebrate)
                    if (!motionNow.reduced) {
                        launch { burst.animateTo(1f, motionNow.timed(BURST_MS, HaloEasing.EmphasizedDecelerate)) }
                    }
                    check.animateTo(1f, motionNow.spring(HaloSpring.Quick))
                }
            }
        }
    }
    // Disabled mid-charge: abandon the hold.
    LaunchedEffect(enabled) {
        if (!enabled && !locked) {
            chargeJob.job?.cancel()
            haptics.cancel()
            charge.animateTo(0f, motionNow.spring(HaloSpring.Quick))
        }
    }

    val gesture = Modifier.pointerInput(source) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!interactiveNow) return@awaitEachGesture
            val press = PressInteraction.Press(down.position)
            source.tryEmit(press)
            chargeJob.job = scope.launch {
                haptics.perform(HaloHaptic.Crescendo)
                var nextTick = TICK_FROM
                val remaining = ((1f - charge.value) * HOLD_MS).roundToInt()
                charge.animateTo(1f, motionNow.timed(remaining)) {
                    while (value >= nextTick && nextTick < 1f) {
                        haptics.tick(nextTick)
                        nextTick += TICK_STEP
                    }
                }
                lockAndCommit()
            }
            val up = waitForUpOrCancellation()
            source.tryEmit(if (up != null) PressInteraction.Release(press) else PressInteraction.Cancel(press))
            if (!locked) {
                chargeJob.job?.cancel()
                haptics.cancel()
                scope.launch { charge.animateTo(0f, motionNow.spring(HaloSpring.Quick)) }
            }
        }
    }

    val amountText = remember(sats) { AmountFormat.satsWithUnitAnnotated(sats) }
    val spokenAmount = remember(sats) { "${NumberFormat.getIntegerInstance(Locale.US).format(sats)} sats" }
    val semantics = Modifier.clearAndSetSemantics {
        role = Role.Button
        contentDescription = "$label $spokenAmount"
        stateDescription = when {
            !enabled -> "Unavailable"
            state == HoldToSendState.Success -> "Sent"
            locked -> "Sending"
            else -> "Ready"
        }
        if (interactive) {
            val commit = {
                scope.launch { charge.snapTo(1f) }
                lockAndCommit()
                true
            }
            onClick(label = "Send", action = commit)
            customActions = listOf(CustomAccessibilityAction("Send $spokenAmount", commit))
        } else {
            disabled()
        }
    }

    val contentAlpha by animateFloatAsState(if (enabled) 1f else effects.disabledAlpha, motion.fade(HaloFade.Base), label = "holdAlpha")
    val trackShape = remember(collapse) { CollapsingTrackShape(collapse) }
    val indication = remember(trackShape) { HaloIndication(trackShape) }
    val bolt = rememberVectorPainter(HaloIcons.Bolt)
    val boltTint = remember(family.onFill) { ColorFilter.tint(family.onFill) }
    val padPx = with(density) { spacing.xs.toPx() }
    val iconPx = with(density) { sizes.iconLg.toPx() }
    val hairlinePx = with(density) { stroke.thin.toPx() }
    val sparks = remember { sparks() }
    val sparkColors = listOf(family.fill, family.text, colors.content.primary)
    val sparkMaxPx = with(density) { spacing.xs.toPx() }

    Box(
        modifier
            .fillMaxWidth()
            .height(sizes.controlXl)
            .then(semantics)
            .pressScale(source, HALO_PRESSED_SCALE_WIDE)
            .graphicsLayer { alpha = contentAlpha }
            .drawBehind { drawBurst(burst.value, sparks, sparkColors, sparkMaxPx) }
            .haloGlow(family.glow, trackShape, effects.glowRadius, effects.glowSpread) {
                if (enabled) GLOW_REST + (1f - GLOW_REST) * charge.value else 0f
            }
            .drawBehind {
                val g = trackGeometry(size, collapse.value)
                val c = collapse.value.coerceIn(0f, 1f)
                // Track
                drawRoundRect(family.tint, g.topLeft, g.size, CornerRadius(g.radius))
                drawRoundRect(
                    color = colors.line.default,
                    topLeft = g.topLeft + Offset(hairlinePx / 2f, hairlinePx / 2f),
                    size = Size(g.size.width - hairlinePx, g.size.height - hairlinePx),
                    cornerRadius = CornerRadius(g.radius - hairlinePx / 2f),
                    style = Stroke(hairlinePx),
                )
                // Charge fill (mirrored in RTL so it grows from the start edge)
                scale(if (layoutDirection == LayoutDirection.Rtl) -1f else 1f, 1f) {
                    val fill = fillGeometry(g, padPx * (1f - c), charge.value)
                    drawRoundRect(family.fill, fill.topLeft, fill.size, CornerRadius(fill.radius))
                    if (c < 1f && charge.value > 0f) {
                        val frontEnd = fill.topLeft.x + fill.size.width
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                1f to family.text.copy(alpha = FRONT_GLOW_ALPHA * (1f - c)),
                                startX = frontEnd - fill.size.height,
                                endX = frontEnd,
                            ),
                            topLeft = fill.topLeft,
                            size = fill.size,
                            cornerRadius = CornerRadius(fill.radius),
                        )
                    }
                    // Bolt stays in the thumb; the inverting label carries the progress.
                    if (c < 1f) {
                        val cx = fill.topLeft.x + fill.size.height / 2f
                        val cy = fill.topLeft.y + fill.size.height / 2f
                        translate(cx - iconPx / 2f, cy - iconPx / 2f) {
                            with(bolt) { draw(Size(iconPx, iconPx), alpha = 1f - c, colorFilter = boltTint) }
                        }
                    }
                }
            }
            .indication(source, indication)
            .then(gesture),
    ) {
        // Label, drawn twice: base colour, then onFill clipped to the charged region (pixel-exact
        // inversion). Both layers fill the component and clip in its coordinates: the base layer to the
        // live track (so a collapsing pill never shows text outside itself), the overlay to the fill.
        val labelAlpha = Modifier.graphicsLayer { alpha = 1f - (collapse.value * LABEL_FADE_RATE).coerceIn(0f, 1f) }
        Box(
            Modifier
                .matchParentSize()
                .then(labelAlpha)
                .drawWithContent {
                    val track = trackGeometry(size, collapse.value)
                    clipRect(left = track.topLeft.x, right = track.topLeft.x + track.size.width) {
                        this@drawWithContent.drawContent()
                    }
                },
        ) {
            HoldLabelSlot(label, amountText, colors.content.primary)
        }
        Box(
            Modifier
                .matchParentSize()
                .then(labelAlpha)
                .drawWithContent {
                    val c = collapse.value.coerceIn(0f, 1f)
                    val fill = fillGeometry(trackGeometry(size, c), padPx * (1f - c), charge.value)
                    val left = fill.topLeft.x
                    val right = left + fill.size.width
                    if (layoutDirection == LayoutDirection.Rtl) {
                        clipRect(left = size.width - right, right = size.width - left) { this@drawWithContent.drawContent() }
                    } else {
                        clipRect(left = left, right = right) { this@drawWithContent.drawContent() }
                    }
                },
        ) {
            HoldLabelSlot(label, amountText, family.onFill)
        }
        // Locked state: spinner while sending, then the self-drawing check.
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
            val spinnerVisible by remember { derivedStateOf { locked && check.value < 1f } }
            if (spinnerVisible) {
                HaloSpinner(
                    modifier = Modifier
                        .size(sizes.iconLg)
                        .graphicsLayer { alpha = ((collapse.value - SPINNER_FROM) / (1f - SPINNER_FROM)).coerceIn(0f, 1f) * (1f - check.value.coerceIn(0f, 1f)) },
                    color = family.onFill,
                )
            }
            CheckMark(check, family.onFill, Modifier.size(sizes.iconXl))
        }
    }
}

/** The label, centred in the track to the end of the thumb. */
@Composable
private fun HoldLabelSlot(label: String, amount: AnnotatedString, color: Color) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(start = HaloTheme.sizes.controlXl, end = HaloTheme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        HoldLabel(label, amount, color)
    }
}

@Composable
private fun HoldLabel(label: String, amount: AnnotatedString, color: Color) {
    Row {
        HaloText(
            text = label,
            style = HaloTheme.typography.label,
            color = color,
            maxLines = 1,
            modifier = Modifier
                .weight(1f, fill = false)
                .alignByBaseline(),
        )
        Spacer(Modifier.width(HaloTheme.spacing.sm))
        HaloText(
            text = amount,
            style = HaloTheme.typography.amountSmall,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Composable
private fun CheckMark(progress: Animatable<Float, AnimationVector1D>, color: Color, modifier: Modifier = Modifier) {
    val path = remember { Path() }
    val segment = remember { Path() }
    val measure = remember { PathMeasure() }
    val iconStroke = HaloTheme.stroke.icon.value
    Canvas(modifier) {
        val p = progress.value.coerceIn(0f, 1f)
        if (p <= 0f) return@Canvas
        val unit = size.width / ICON_GRID
        path.reset()
        path.moveTo(5f * unit, 12.5f * unit)
        path.lineTo(10f * unit, 17.5f * unit)
        path.lineTo(19f * unit, 7.5f * unit)
        measure.setPath(path, false)
        segment.reset()
        measure.getSegment(0f, measure.length * p, segment, true)
        drawPath(segment, color, style = Stroke(iconStroke * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ------------------------------------------------------------------------------------------- geometry

private class TrackGeometry(val topLeft: Offset, val size: Size, val radius: Float)

/** Full-width pill that collapses to a centred circle as [collapse] → 1. */
private fun trackGeometry(size: Size, collapse: Float): TrackGeometry {
    val c = collapse.coerceIn(0f, 1f)
    val inset = max(0f, size.width - size.height) / 2f * c
    return TrackGeometry(Offset(inset, 0f), Size(size.width - inset * 2f, size.height), size.height / 2f)
}

/** The charge pill inside the track: a thumb-sized circle at 0, the whole inner track at 1. */
private fun fillGeometry(track: TrackGeometry, pad: Float, charge: Float): TrackGeometry {
    val innerH = track.size.height - pad * 2f
    val innerW = track.size.width - pad * 2f
    val width = innerH + (innerW - innerH) * charge.coerceIn(0f, 1f)
    return TrackGeometry(Offset(track.topLeft.x + pad, track.topLeft.y + pad), Size(width, innerH), innerH / 2f)
}

private class CollapsingTrackShape(private val collapse: Animatable<Float, AnimationVector1D>) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val g = trackGeometry(size, collapse.value)
        return Outline.Rounded(
            RoundRect(g.topLeft.x, g.topLeft.y, g.topLeft.x + g.size.width, g.topLeft.y + g.size.height, CornerRadius(g.radius)),
        )
    }
}

// ------------------------------------------------------------------------------------------- burst

private class Spark(val angle: Float, val speed: Float, val size: Float, val colorIndex: Int)

private fun sparks(): List<Spark> {
    val random = Random(SPARK_SEED)
    return List(SPARK_COUNT) { i ->
        val base = (i.toFloat() / SPARK_COUNT) * TWO_PI
        Spark(
            angle = base + random.nextFloat() * SPARK_JITTER,
            speed = 0.6f + random.nextFloat() * 0.9f,
            size = 0.5f + random.nextFloat() * 0.5f,
            colorIndex = i % 3,
        )
    }
}

private fun DrawScope.drawBurst(progress: Float, sparks: List<Spark>, colors: List<Color>, maxSparkPx: Float) {
    if (progress <= 0f || progress >= 1f) return
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.height / 2f
    sparks.forEach { spark ->
        val distance = radius * (1f + spark.speed * BURST_REACH * progress)
        val position = center + Offset(cos(spark.angle) * distance, sin(spark.angle) * distance)
        drawCircle(
            color = colors[spark.colorIndex],
            radius = maxSparkPx * spark.size * (1f - progress * 0.5f),
            center = position,
            alpha = 1f - progress,
        )
    }
}

// ------------------------------------------------------------------------------------------- constants

private class JobSlot(var job: Job? = null)
private class StateSlot(var value: HoldToSendState)

/** Hold duration — the intent check. Scaled by slow-mo, never removed by reduced motion. */
private const val HOLD_MS = 1_200
private const val BURST_MS = 700
private const val TICK_FROM = 0.4f
private const val TICK_STEP = 0.1f
private const val GLOW_REST = 0.35f
private const val FRONT_GLOW_ALPHA = 0.7f
private const val SPINNER_FROM = 0.6f
/** The label is gone by half-collapse, before the track narrows past it. */
private const val LABEL_FADE_RATE = 2f
private const val ICON_GRID = 24f
private const val SPARK_COUNT = 14
private const val SPARK_SEED = 7
private const val SPARK_JITTER = 0.35f
private const val BURST_REACH = 0.6f
private const val TWO_PI = (2.0 * Math.PI).toFloat()

// ------------------------------------------------------------------------------------------- previews

@Preview
@Composable
private fun HaloHoldToSendIdlePreview() = HaloPreview {
    HaloHoldToSend(sats = 21_000, state = HoldToSendState.Idle, onCommit = {})
    HaloHoldToSend(sats = 9_000_000, state = HoldToSendState.Idle, onCommit = {}, enabled = false, label = "Insufficient funds")
}

@Preview
@Composable
private fun HaloHoldToSendLockedPreview() = HaloPreview {
    HaloHoldToSend(sats = 21_000, state = HoldToSendState.Sending, onCommit = {})
    HaloHoldToSend(sats = 21_000, state = HoldToSendState.Success, onCommit = {})
}
