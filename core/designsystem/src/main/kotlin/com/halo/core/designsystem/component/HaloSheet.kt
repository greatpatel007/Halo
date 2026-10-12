package com.halo.core.designsystem.component

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import com.halo.core.designsystem.foundation.HaloOverlay
import com.halo.core.designsystem.foundation.HaloPreview
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.material.ReliefPainter
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloHaptics
import com.halo.core.designsystem.theme.HaloMotion
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Where a sheet rests. [Partial] exists only when the content is taller than about half the screen. */
enum class HaloSheetValue { Hidden, Partial, Expanded }

/**
 * A sheet's state: where it rests or is heading. Call [show], [expand] or [hide] from events; the sheet animates there.
 */
@Stable
class HaloSheetState internal constructor(initialValue: HaloSheetValue) {
    /** Where the sheet is resting or heading. */
    var targetValue by mutableStateOf(initialValue)
        private set

    /** Where the sheet last came to rest. */
    var currentValue by mutableStateOf(initialValue)
        internal set

    /** True from [show] until the sheet has fully left the screen. */
    val isVisible: Boolean get() = targetValue != HaloSheetValue.Hidden || !atHidden

    /** Opens at the partial height when there is one, otherwise fully. */
    fun show() = move(HaloSheetValue.Partial)

    fun expand() = move(HaloSheetValue.Expanded)

    fun hide() = move(HaloSheetValue.Hidden)

    private fun move(value: HaloSheetValue) {
        targetValue = value
        requests++
    }

    /** Y of the sheet's top edge in the overlay (px); NaN until first laid out. */
    internal val offset = Animatable(Float.NaN)
    internal var anchors by mutableStateOf(SheetAnchors.Unknown)
    internal var requests by mutableIntStateOf(0)
    /** Height of the overlay, so the slab can be drawn down to the screen's bottom edge. */
    internal var containerHeight by mutableFloatStateOf(0f)
    internal var dragging = false

    private val atHidden: Boolean
        get() {
            val y = offset.value
            val a = anchors
            return y.isNaN() || !a.known || y >= a.hidden - HIDDEN_EPSILON
        }

    /** Resting position for [value] on the current anchors ([Partial] falls back to [Expanded]). */
    internal fun positionOf(value: HaloSheetValue): Float = when (value) {
        HaloSheetValue.Hidden -> anchors.hidden
        HaloSheetValue.Partial -> anchors.partial ?: anchors.expanded
        HaloSheetValue.Expanded -> anchors.expanded
    }

    /** Where the sheet is right now; before the first animation, its target's resting place (previews). */
    internal val position: Float
        get() = offset.value.takeUnless { it.isNaN() } ?: positionOf(targetValue)

    /** 0 hidden … 1 at the lowest resting point: drives the scrim and the receding screen. Draw/layout phase only. */
    internal val openness: Float
        get() {
            val a = anchors
            if (!a.known) return 0f
            return ((a.hidden - position) / (a.hidden - a.lowest)).coerceIn(0f, 1f)
        }

    internal fun valueAt(y: Float): HaloSheetValue = when {
        y >= anchors.hidden - HIDDEN_EPSILON -> HaloSheetValue.Hidden
        anchors.partial != null && abs(y - anchors.partial!!) < abs(y - anchors.expanded) -> HaloSheetValue.Partial
        else -> HaloSheetValue.Expanded
    }

    internal fun settledAt(value: HaloSheetValue) {
        currentValue = value
        if (targetValue != value) targetValue = value
    }

    private companion object {
        const val HIDDEN_EPSILON = 0.5f
    }
}

@Composable
fun rememberHaloSheetState(initialValue: HaloSheetValue = HaloSheetValue.Hidden): HaloSheetState =
    remember { HaloSheetState(initialValue) }

/**
 * A tray that slides up over the current screen for a flow or detail (send, receive, a transaction). A soft-touch slab
 * (ceramic in light), lit by the key light, rising over a dimmed screen that recedes behind it. Drag it between its
 * resting points: the release is projected with its momentum to the nearest one (rubber-banding past the top), and a
 * fast throw overshoots a touch before it lands with a soft knock. Dismiss by dragging or throwing it down, tapping
 * the dim, or the back gesture (predictive). Content that grows or shrinks makes the tray morph to its new height.
 *
 * Declare it anywhere: inside `HaloApp` it renders in the window's overlay layer, above everything.
 *
 * Spec: docs/components/HaloSheet.md · Archetype: Container / Overlay · Showroom: halo://showroom/sheet
 *
 * @param state where the sheet rests; open it with [HaloSheetState.show].
 * @param title announced as the pane title by screen readers.
 * @param dismissible false keeps it open: it resists being dragged below its lowest resting point.
 * @param onDismiss called once the sheet has left the screen after a user dismissal (drag, dim tap, back).
 */
@Composable
fun HaloSheet(
    state: HaloSheetState,
    modifier: Modifier = Modifier,
    title: String? = null,
    dismissible: Boolean = true,
    onDismiss: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(horizontal = HaloTheme.spacing.gutter),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!state.isVisible) return
    HaloOverlay(recede = { state.openness }) {
        SheetLayer(state, modifier, title, dismissible, onDismiss, contentPadding, content)
    }
}

@Composable
private fun SheetLayer(
    state: HaloSheetState,
    modifier: Modifier,
    title: String?,
    dismissible: Boolean,
    onDismiss: () -> Unit,
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    val motion = HaloTheme.motion
    val haptics = HaloTheme.haptics
    val colors = HaloTheme.colors
    val radius = HaloTheme.radius.sheet
    val spacing = HaloTheme.spacing
    val stroke = HaloTheme.stroke
    val light = HaloTheme.light
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val latestDismiss by rememberUpdatedState(onDismiss)
    val drag = remember(state) { SheetDrag(state, scope) }
    drag.motion = motion
    drag.haptics = haptics
    drag.dismissible = dismissible
    drag.onDismiss = { latestDismiss() }
    drag.throwVelocity = with(density) { THROW_VELOCITY_DP.toFloat() * this.density }
    drag.dimension = state.containerHeight

    // Programmatic moves (show / expand / hide) land on a placed surface's spring; a change of content height morphs
    // the resting sheet to its new size on Standard.
    var lastRequest by remember { mutableIntStateOf(-1) }
    LaunchedEffect(state.requests, state.anchors) {
        val a = state.anchors
        if (!a.known || state.dragging) return@LaunchedEffect
        val requested = state.requests != lastRequest
        lastRequest = state.requests
        if (state.offset.value.isNaN()) state.offset.snapTo(a.hidden)
        val target = state.targetValue
        val spec = motion.spring<Float>(if (requested) HaloSpring.Gentle else HaloSpring.Standard)
        state.offset.animateTo(state.positionOf(target), spec)
        state.settledAt(target)
        if (target == HaloSheetValue.Hidden) {
            if (requested && drag.userDismissal) latestDismiss()
            drag.userDismissal = false
        } else if (requested) {
            haptics.perform(HaloHaptic.Land)
        }
    }

    // Predictive back: the sheet shrinks toward its bottom edge with the gesture; a commit dismisses.
    val back = remember { Animatable(0f) }
    PredictiveBackHandler(enabled = dismissible && state.targetValue != HaloSheetValue.Hidden) { events ->
        try {
            events.collect { back.snapTo(it.progress) }
            haptics.tick(DISMISS_TICK)
            drag.userDismissal = true
            state.hide()
            back.animateTo(0f, motion.spring(HaloSpring.Quick))
        } catch (e: CancellationException) {
            scope.launch { back.animateTo(0f, motion.spring(HaloSpring.Quick)) }
            throw e
        }
    }

    val painter = remember { ReliefPainter() }
    val slabShape = remember(radius) { RoundedCornerShape(topStart = radius, topEnd = radius) }
    val elevation = with(density) { spacing.x3.toPx() }
    val maxTopGap = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + spacing.x5
    val bottomInset = WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues().calculateBottomPadding()
    val handle = HaloTheme.sizes.sheetHandle
    val closeLabel = "Close"

    Box(Modifier.fillMaxSize()) {
        // The dim behind: fades with how open the sheet is; a tap dismisses.
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(colors.bg.scrim, alpha = state.openness) }
                .then(
                    if (dismissible) {
                        Modifier
                            .pointerInput(state) {
                                detectTapGestures {
                                    haptics.tick(DISMISS_TICK)
                                    drag.userDismissal = true
                                    state.hide()
                                }
                            }
                            .semantics { onClick(label = closeLabel) { drag.userDismissal = true; state.hide(); true } }
                    } else {
                        Modifier
                    },
                ),
        )
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                Column(
                    modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val b = back.value
                            val s = 1f - BACK_SCALE * b
                            scaleX = s
                            scaleY = s
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .drawBehind {
                            // The slab runs down to the screen's bottom edge whatever its content height, so a
                            // throw's overshoot or a shrinking tray never shows a gap.
                            val extra = (state.containerHeight - state.position - size.height).coerceAtLeast(0f)
                            val slab = Size(size.width, size.height + extra)
                            val r = radius.toPx()
                            painter.drawContactShadow(this, Rect(Offset.Zero, slab), r, elevation, light, SHADOW_STRENGTH)
                            val outline = slabShape.createOutline(slab, layoutDirection, this)
                            val path = Path().apply { addOutline(outline) }
                            drawPath(path, colors.bg.raised)
                            drawPath(path, colors.line.default, style = Stroke(stroke.hairline.toPx()))
                            // The top edge catches the key light.
                            clipRect(bottom = r) {
                                drawRoundRect(
                                    colors.line.highlight,
                                    topLeft = Offset(0f, stroke.thin.toPx()),
                                    size = Size(slab.width, r * 2f),
                                    cornerRadius = CornerRadius(r),
                                    style = Stroke(stroke.thin.toPx()),
                                )
                            }
                        }
                        .nestedScroll(drag.nested)
                        .draggable(
                            state = rememberDraggableState { drag.dragBy(it) },
                            orientation = Orientation.Vertical,
                            onDragStarted = { drag.start() },
                            onDragStopped = { drag.release(it) },
                        )
                        .focusGroup()
                        .semantics {
                            isTraversalGroup = true
                            if (title != null) paneTitle = title
                            customActions = buildList {
                                if (dismissible) add(CustomAccessibilityAction(closeLabel) { drag.userDismissal = true; state.hide(); true })
                                if (state.anchors.partial != null) {
                                    add(CustomAccessibilityAction("Expand") { state.expand(); true })
                                }
                            }
                        }
                        .padding(bottom = bottomInset),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(HaloTheme.sizes.minTouch / 2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Spacer(
                            Modifier
                                .size(handle)
                                .background(colors.line.strong, HaloTheme.radius.full),
                        )
                    }
                    Column(Modifier.padding(contentPadding), content = content)
                }
            },
        ) { measurables, constraints ->
            val height = constraints.maxHeight.toFloat()
            val maxSheet = (height - maxTopGap.toPx()).coerceAtLeast(0f)
            val sheet = measurables.first().measure(
                Constraints(minWidth = constraints.maxWidth, maxWidth = constraints.maxWidth, maxHeight = maxSheet.toInt()),
            )
            if (state.containerHeight != height) state.containerHeight = height
            val next = SheetPhysics.anchors(height, sheet.height.toFloat())
            if (next != state.anchors) state.anchors = next
            layout(constraints.maxWidth, constraints.maxHeight) {
                sheet.place(0, state.position.toInt())
            }
        }
    }
}

/** Drag, nested scroll and release for one sheet. Positions are the sheet's top edge in px; +y is down. */
private class SheetDrag(private val state: HaloSheetState, private val scope: CoroutineScope) {
    lateinit var motion: HaloMotion
    lateinit var haptics: HaloHaptics
    var dismissible = true
    var onDismiss: () -> Unit = {}
    var throwVelocity = 0f
    var dimension = 0f
    var userDismissal = false

    /** Unclamped finger position; the sheet shows it through the rubber band. */
    private var raw = 0f
    private var lastShown = 0f

    fun start() {
        state.dragging = true
        raw = state.position
        lastShown = raw
    }

    fun dragBy(delta: Float) {
        if (!state.dragging) start()
        raw += delta
        val a = state.anchors
        val floor = if (dismissible) a.hidden else a.lowest
        val shown = SheetPhysics.rubberBand(raw, a.expanded, floor, dimension)
        // A light tick as the sheet passes one of its resting points.
        val restingPoints = listOfNotNull(a.expanded, a.partial)
        if (restingPoints.any { (lastShown - it) * (shown - it) < 0f }) haptics.tick(PASS_TICK)
        lastShown = shown
        scope.launch { state.offset.snapTo(shown) }
    }

    fun release(velocity: Float) {
        if (!state.dragging) return
        state.dragging = false
        val a = state.anchors
        val candidates = buildList {
            add(a.expanded)
            a.partial?.let(::add)
            if (dismissible) add(a.hidden)
        }
        val target = SheetPhysics.settle(state.position, velocity, candidates)
        val value = state.valueAt(target)
        // Bounce only after a throw, and never into the screen's edge.
        val thrown = abs(velocity) > throwVelocity && value != HaloSheetValue.Hidden
        val spec = motion.spring<Float>(if (thrown) HaloSpring.Throw else HaloSpring.Gentle)
        if (value == HaloSheetValue.Hidden) haptics.tick(DISMISS_TICK)
        scope.launch {
            state.offset.animateTo(target, spec, initialVelocity = velocity)
            state.settledAt(value)
            if (value == HaloSheetValue.Hidden) onDismiss() else haptics.perform(HaloHaptic.Land)
        }
    }

    /** Hands scrolling over between the content and the sheet: content scrolls only once the sheet is fully up. */
    val nested = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            val up = available.y < 0f
            return if (up && state.position > state.anchors.expanded + SNAP_EPSILON) {
                dragBy(available.y)
                Offset(0f, available.y)
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
            dragBy(available.y)
            return Offset(0f, available.y)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!state.dragging) return Velocity.Zero
            release(available.y)
            return available
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (!state.dragging) return Velocity.Zero
            release(available.y)
            return available
        }
    }
}

/** Resting points of a sheet, as the y of its top edge (px). */
internal data class SheetAnchors(val hidden: Float, val partial: Float?, val expanded: Float) {
    val known: Boolean get() = !hidden.isNaN()
    val lowest: Float get() = partial ?: expanded

    companion object {
        val Unknown = SheetAnchors(Float.NaN, null, Float.NaN)
    }
}

/**
 * The sheet's physics, pure and unit-tested (Apple, "Designing Fluid Interfaces"): a release is projected with its
 * momentum the way a scroll view decelerates, the sheet goes to the resting point nearest that landing (so a short,
 * fast flick and a long, slow drag both work), and dragging past an end meets growing resistance.
 */
internal object SheetPhysics {
    /** UIScrollView's normal deceleration rate (per millisecond). */
    const val DECELERATION = 0.998f
    /** Rubber-band stiffness: Apple's constant. */
    const val RUBBER = 0.55f
    /** A sheet taller than this share of the screen also rests half open. */
    const val PARTIAL_SHARE = 0.5f
    /** It takes a partial resting point only if it is this much taller than [PARTIAL_SHARE]. */
    const val PARTIAL_MARGIN = 1.15f

    /** Where a release at [position] with [velocity] (px/s) would come to rest under scroll-view deceleration. */
    fun project(position: Float, velocity: Float): Float =
        position + velocity / 1000f * DECELERATION / (1f - DECELERATION)

    /** The resting point nearest the projected landing. */
    fun settle(position: Float, velocity: Float, candidates: List<Float>): Float {
        val landing = project(position, velocity)
        return candidates.minBy { abs(it - landing) }
    }

    /** Apple's rubber band: the sheet follows [raw] between [top] and [bottom] and resists beyond them. */
    fun rubberBand(raw: Float, top: Float, bottom: Float, dimension: Float): Float = when {
        raw < top -> top - resist(top - raw, dimension)
        raw > bottom -> bottom + resist(raw - bottom, dimension)
        else -> raw
    }

    /** How far the sheet moves for a pull of [overshoot] past an end: approaches [dimension] but never reaches it. */
    fun resist(overshoot: Float, dimension: Float): Float {
        if (overshoot <= 0f || dimension <= 0f) return 0f
        return (1f - 1f / (overshoot * RUBBER / dimension + 1f)) * dimension
    }

    /** Resting points for a sheet of [sheetHeight] in a container of [containerHeight]. */
    fun anchors(containerHeight: Float, sheetHeight: Float): SheetAnchors {
        val expanded = containerHeight - sheetHeight
        val half = containerHeight * PARTIAL_SHARE
        val partial = if (sheetHeight > half * PARTIAL_MARGIN) containerHeight - half else null
        return SheetAnchors(hidden = containerHeight, partial = partial, expanded = expanded)
    }
}

/** A throw faster than this (dp/s) is carried by its momentum, with a hint of overshoot. */
private const val THROW_VELOCITY_DP = 1200
/** Predictive back shrinks the sheet toward its bottom edge by up to this much. */
private const val BACK_SCALE = 0.08f
private const val PASS_TICK = 0.35f
private const val DISMISS_TICK = 0.3f
private const val SHADOW_STRENGTH = 0.6f
private const val SNAP_EPSILON = 0.5f

@Preview(name = "Sheet · open", heightDp = 480)
@Composable
private fun HaloSheetPreviewOpen() = HaloPreview { SheetPreviewContent(HaloSheetValue.Expanded) }

@Preview(name = "Sheet · partial", heightDp = 480)
@Composable
private fun HaloSheetPreviewPartial() = HaloPreview { SheetPreviewContent(HaloSheetValue.Partial) }

@Composable
private fun SheetPreviewContent(value: HaloSheetValue) {
    Box(Modifier.fillMaxSize()) {
        HaloSheet(state = rememberHaloSheetState(value), title = "Receive") {
            Column(verticalArrangement = Arrangement.spacedBy(HaloTheme.spacing.md)) {
                HaloText("Receive", style = HaloTheme.typography.title)
                HaloText("Share your address or invoice.", style = HaloTheme.typography.body, color = HaloTheme.colors.content.secondary)
                Spacer(Modifier.height(HaloTheme.spacing.x4))
            }
        }
    }
}
