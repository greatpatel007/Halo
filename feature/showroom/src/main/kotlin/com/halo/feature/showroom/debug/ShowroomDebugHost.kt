package com.halo.feature.showroom.debug

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.halo.core.designsystem.foundation.HaloDepth
import com.halo.core.designsystem.foundation.HaloPressable
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.HaloThemeRevealHost
import com.halo.core.designsystem.foundation.backdropSource
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.foundation.rememberHaloBackdrop
import com.halo.core.designsystem.icons.HaloIcon
import com.halo.core.designsystem.icons.HaloIcons
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloMotion
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.core.designsystem.theme.haloDarkColors
import com.halo.core.designsystem.theme.haloLightColors
import com.halo.core.designsystem.theme.rememberSystemHaloMotion
import com.halo.feature.showroom.ui.haloDepth
import kotlinx.coroutines.delay
import java.util.Locale

/** Pixel-perfect review tools for the whole app. Development aid — not part of the wallet product. */
@Stable
class ShowroomDebugState {
    var gridStep by mutableStateOf<Int?>(null)
    var slowMo by mutableFloatStateOf(1f)
    var reducedMotion by mutableStateOf(false)
    var hapticLog by mutableStateOf(true)
    internal val events = mutableStateListOf<HapticEvent>()
    private var nextId = 0L

    internal fun log(haptic: HaloHaptic, intensity: Float) {
        if (!hapticLog) return
        events.add(HapticEvent(nextId++, haptic, intensity))
        while (events.size > MAX_EVENTS) events.removeAt(0)
    }

    private companion object {
        const val MAX_EVENTS = 4
    }
}

internal data class HapticEvent(val id: Long, val haptic: HaloHaptic, val intensity: Float)

private val GRID_STEPS = listOf(null, 4, 8)
private val SLOW_MO = listOf(1f, 2f, 5f, 10f)

/**
 * Wraps the app in [HaloTheme] with debug-controllable motion, plus a floating glass dock:
 * grid overlay (4/8dp), slow-mo (1–10×), reduced-motion simulation and an on-screen haptic log.
 * [dark] picks the palette (the app passes the stored appearance so switches are immediate).
 */
@Composable
fun ShowroomDebugHost(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val state = remember { ShowroomDebugState() }
    LaunchedEffect(state) { ShowroomDebugCommands.stream.collect { state.apply(it) } }
    val system = rememberSystemHaloMotion()
    val motion = remember(state.slowMo, state.reducedMotion, system) {
        HaloMotion(scale = state.slowMo, reduced = state.reducedMotion || system.reduced)
    }
    val colors = remember(dark) { if (dark) haloDarkColors() else haloLightColors() }
    HaloTheme(colors = colors, motion = motion, hapticObserver = state::log) {
        val backdrop = rememberHaloBackdrop()
        // Everything visible (content + dock + overlays) sits inside the reveal so a theme switch is seamless.
        HaloThemeRevealHost(isDark = HaloTheme.colors.isDark, modifier = Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(HaloTheme.colors.bg.canvas),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .backdropSource(backdrop),
                ) { content() }
                state.gridStep?.let { GridOverlay(it) }
                // Just above the dock that controls it: never over a page's header or the control being tested.
                HapticLog(
                    state,
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = HaloTheme.spacing.lg + DockHeight + HaloTheme.spacing.sm),
                )
                Dock(
                    state = state,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = HaloTheme.spacing.lg),
                    backdrop = backdrop,
                )
            }
        }
    }
}

/** Height of the [Dock]: its buttons plus the glass padding around them. */
private val DockHeight: Dp
    @Composable get() = HaloTheme.sizes.controlMd + HaloTheme.spacing.xs * 2

@Composable
private fun Dock(state: ShowroomDebugState, backdrop: com.halo.core.designsystem.foundation.HaloBackdrop, modifier: Modifier) {
    val shape = HaloTheme.radius.full
    Row(
        modifier
            .haloDepth(HaloDepth.Glass, shape, backdrop = backdrop)
            .padding(HaloTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(HaloTheme.spacing.xxs),
    ) {
        DockButton(
            icon = HaloIcons.Grid,
            label = state.gridStep?.let { "$it" } ?: "off",
            active = state.gridStep != null,
            description = "Grid overlay",
        ) { state.gridStep = GRID_STEPS[(GRID_STEPS.indexOf(state.gridStep) + 1) % GRID_STEPS.size] }
        DockButton(
            icon = HaloIcons.Gauge,
            label = String.format(Locale.US, "%.0f×", state.slowMo),
            active = state.slowMo > 1f,
            description = "Slow motion",
        ) { state.slowMo = SLOW_MO[(SLOW_MO.indexOf(state.slowMo) + 1) % SLOW_MO.size] }
        DockButton(
            icon = HaloIcons.Wave,
            label = if (state.reducedMotion) "rm" else "full",
            active = state.reducedMotion,
            description = "Reduced motion",
        ) { state.reducedMotion = !state.reducedMotion }
        DockButton(
            icon = HaloIcons.Info,
            label = if (state.hapticLog) "log" else "off",
            active = state.hapticLog,
            description = "Haptic log",
        ) { state.hapticLog = !state.hapticLog }
    }
}

@Composable
private fun DockButton(icon: ImageVector, label: String, active: Boolean, description: String, onClick: () -> Unit) {
    val colors = HaloTheme.colors
    val shape = HaloTheme.radius.full
    HaloPressable(
        onClick = onClick,
        shape = shape,
        haptic = HaloHaptic.Tick,
        onClickLabel = description,
        modifier = Modifier
            .height(HaloTheme.sizes.controlMd)
            .background(if (active) colors.accent.tint else Color.Transparent, shape)
            .semantics { stateDescription = label }
            .padding(horizontal = HaloTheme.spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HaloIcon(icon, contentDescription = null, size = HaloTheme.sizes.iconMd, tint = if (active) colors.accent.text else colors.content.secondary)
            Spacer(Modifier.width(HaloTheme.spacing.xs))
            HaloText(label, style = HaloTheme.typography.micro, color = if (active) colors.accent.text else colors.content.tertiary)
        }
    }
}

@Composable
private fun HapticLog(state: ShowroomDebugState, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(HaloTheme.spacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
        state.events.forEach { event ->
            key(event.id) {
                LaunchedEffect(Unit) {
                    delay(1_600)
                    state.events.remove(event)
                }
                val shape = HaloTheme.radius.full
                Row(
                    Modifier
                        .enterRise()
                        .haloDepth(HaloDepth.Raised, shape)
                        .padding(horizontal = HaloTheme.spacing.md, vertical = HaloTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(HaloTheme.spacing.sm)
                            .background(HaloTheme.colors.accent.fill.copy(alpha = 0.3f + 0.7f * event.intensity), RoundedCornerShape(HaloTheme.spacing.xs)),
                    )
                    Spacer(Modifier.width(HaloTheme.spacing.sm))
                    val intensity = if (event.intensity < 1f) String.format(Locale.US, " %.1f", event.intensity) else ""
                    HaloText("haptic · ${event.haptic.name}$intensity", style = HaloTheme.typography.data)
                }
            }
        }
    }
}

@Composable
private fun GridOverlay(stepDp: Int) {
    val minor = HaloTheme.colors.status.negative.copy(alpha = 0.10f)
    val major = HaloTheme.colors.status.negative.copy(alpha = 0.22f)
    val gutterColor = HaloTheme.colors.accent.fill.copy(alpha = 0.35f)
    val gutter: Dp = HaloTheme.spacing.gutter
    Canvas(Modifier.fillMaxSize()) {
        val step = stepDp * density
        var i = 0
        var x = 0f
        while (x <= size.width) {
            drawLine(if (i % 4 == 0) major else minor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += step; i++
        }
        i = 0
        var y = 0f
        while (y <= size.height) {
            drawLine(if (i % 4 == 0) major else minor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += step; i++
        }
        val g = gutter.toPx()
        drawLine(gutterColor, Offset(g, 0f), Offset(g, size.height), strokeWidth = 1f)
        drawLine(gutterColor, Offset(size.width - g, 0f), Offset(size.width - g, size.height), strokeWidth = 1f)
    }
}
