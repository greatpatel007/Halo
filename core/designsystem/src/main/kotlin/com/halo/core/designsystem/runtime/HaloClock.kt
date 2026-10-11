package com.halo.core.designsystem.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.min

/** A continuous effect's frame step: advance by [dt] seconds; return true while busy (it then gets every frame). */
fun interface AmbientStep {
    fun step(dt: Float): Boolean
}

/**
 * Decides who steps on a frame and by how much (pure, unit-tested). Only visible effects step. If any was busy on its
 * last step, everyone steps every frame; when all are calm, they step at ~30 fps with the accumulated time, and the
 * frames in between write nothing (so nothing redraws).
 */
internal class AmbientScheduler {
    private class Entry(val step: AmbientStep) {
        var visible = false
    }

    private val entries = mutableListOf<Entry>()
    private var pending = 0f
    private var busy = true

    val visibleCount: Int get() = entries.count { it.visible }

    fun add(step: AmbientStep): Any = Entry(step).also {
        entries += it
        busy = true
    }

    fun remove(handle: Any) {
        entries.remove(handle)
    }

    fun setVisible(handle: Any, visible: Boolean) {
        val e = handle as Entry
        if (visible && !e.visible) busy = true
        e.visible = visible
    }

    /** The clock was paused: don't hand the pause to the next step as elapsed time. */
    fun reset() {
        pending = 0f
        busy = true
    }

    /** One frame of [dt] seconds; returns how many effects stepped. */
    fun frame(dt: Float): Int {
        val active = entries.filter { it.visible }
        if (active.isEmpty()) {
            pending = 0f
            return 0
        }
        pending += dt
        if (!busy && pending < CALM_STEP_S) return 0
        val step = min(pending, MAX_STEP_S)
        pending = 0f
        var anyBusy = false
        for (e in active) anyBusy = e.step.step(step) || anyBusy
        busy = anyBusy
        return active.size
    }

    companion object {
        const val CALM_STEP_S = 1f / 30f
        /** Longest step, so a dropped frame never makes an effect jump. */
        const val MAX_STEP_S = 0.05f
    }
}

/**
 * The design system's one frame clock for continuous effects. It runs only while some visible effect is registered
 * and [HaloPolicy.ambientAllowed]; otherwise no frames are requested at all.
 */
@Stable
class HaloClock internal constructor(private val policy: HaloPolicy) {
    internal val scheduler = AmbientScheduler()

    /** Visible continuous effects right now (the Showroom lab shows it). */
    var activeEffects: Int by mutableIntStateOf(0)
        private set

    internal fun refreshCount() {
        activeEffects = scheduler.visibleCount
    }

    /** Runs the loop from the theme root; [timeScale] is the slow-mo factor (physics time = real time / scale). */
    @Composable
    internal fun Drive(timeScale: Float) {
        val scale by rememberUpdatedState(timeScale)
        LaunchedEffect(this) {
            snapshotFlow { activeEffects > 0 && policy.ambientAllowed }.collectLatest { run ->
                if (!run) return@collectLatest
                scheduler.reset()
                var last = withFrameNanos { it }
                while (true) {
                    withFrameNanos { now ->
                        scheduler.frame((now - last) / NANOS_PER_S / scale)
                        last = now
                    }
                }
            }
        }
    }

    private companion object {
        const val NANOS_PER_S = 1_000_000_000f
    }
}

/** A registered continuous effect: attach [visibility] to the element, read [running] to gate sensors and extras. */
@Stable
class Ambient internal constructor() {
    internal var visibleState by mutableStateOf(false)
    internal var runningState: () -> Boolean = { false }
    internal var onVisibilityChanged: (Boolean) -> Unit = {}

    /** True while the effect is visible, enabled and allowed by the policy (the clock is stepping it). */
    val running: Boolean get() = runningState()

    /** Reports whether the element is on screen (scroll containers clip it), so hidden effects stop. */
    val visibility: Modifier = Modifier.onGloballyPositioned {
        val visible = !it.boundsInWindow().isEmpty
        if (visible != visibleState) {
            visibleState = visible
            onVisibilityChanged(visible)
        }
    }
}

/**
 * Registers a continuous effect with the runtime's clock. [step] is called with physics time (slow-mo applied) only
 * while the effect is visible, [enabled] and allowed by the policy; it returns true while the effect is busy.
 */
@Composable
fun rememberAmbient(runtime: HaloRuntime, enabled: Boolean, step: AmbientStep): Ambient {
    val ambient = remember { Ambient() }
    val currentStep by rememberUpdatedState(step)
    val clock = runtime.clock
    val running = remember(enabled) {
        derivedStateOf { enabled && ambient.visibleState && runtime.policy.ambientAllowed }
    }
    ambient.runningState = { running.value }
    DisposableEffect(clock, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val handle = clock.scheduler.add { dt -> currentStep.step(dt) }
        clock.scheduler.setVisible(handle, ambient.visibleState)
        clock.refreshCount()
        ambient.onVisibilityChanged = { visible ->
            clock.scheduler.setVisible(handle, visible)
            clock.refreshCount()
        }
        onDispose {
            ambient.onVisibilityChanged = {}
            clock.scheduler.remove(handle)
            clock.refreshCount()
        }
    }
    return ambient
}
