package com.halo.core.designsystem.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * The Halo runtime: the services every component shares, like a small operating system behind `HaloTheme`.
 * - [policy]: may ambient motion, sensors and decoration run (foreground, reduced motion, battery saver, thermal)?
 * - [clock]: the single frame clock for continuous effects ([rememberAmbient]).
 * - [sensors]: one shared listener per motion sensor, ref-counted and gated by the policy.
 *
 * Components reach it through `HaloTheme.runtime` and never touch lifecycle, power or thermal APIs themselves.
 */
@Stable
class HaloRuntime internal constructor(sensorSource: SensorSource = NoSensors) {
    val policy = HaloPolicy()
    val clock = HaloClock(policy)
    val sensors = HaloSensors(sensorSource)

    init {
        // Sensors follow the policy at once: off in the background, under reduced motion, battery saver or heat.
        policy.onChange = { sensors.setAllowed(policy.ambientAllowed) }
    }
}

/** Previews and tests: no hardware. */
private object NoSensors : SensorSource {
    override fun start(sensor: HaloSensor, onValues: (FloatArray, Long) -> Unit) = false
    override fun stop(sensor: HaloSensor) = Unit
}

val LocalHaloRuntime = staticCompositionLocalOf { HaloRuntime() }

/**
 * Creates the runtime for a theme root and keeps its policy current. [reducedMotion] comes from the theme's motion
 * (the system setting or the Showroom simulation); [timeScale] is the slow-mo factor for the clock.
 */
@Composable
fun rememberHaloRuntime(reducedMotion: Boolean, timeScale: Float): HaloRuntime {
    val context = LocalContext.current
    val runtime = remember { HaloRuntime(AndroidSensorSource(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    SideEffect { runtime.policy.reducedMotion = reducedMotion }
    // Observed directly, not through recomposition: a backgrounded app produces no frames, so a recomposition-driven
    // update would never arrive and hardware would stay on.
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            runtime.policy.foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            runtime.policy.foreground = false
        }
    }
    DisposableEffect(context) {
        val watcher = PolicyWatcher(context, runtime.policy).also { it.start() }
        onDispose { watcher.stop() }
    }
    runtime.clock.Drive(timeScale)
    return runtime
}
