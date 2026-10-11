package com.halo.core.designsystem.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * The Halo runtime: the services every component shares, like a small operating system behind `HaloTheme`.
 * - [policy]: may ambient motion, sensors and decoration run (foreground, reduced motion, battery saver, thermal)?
 * - [clock]: the single frame clock for continuous effects ([rememberAmbient]).
 *
 * Components reach it through `HaloTheme.runtime` and never touch lifecycle, power or thermal APIs themselves.
 */
@Stable
class HaloRuntime internal constructor() {
    val policy = HaloPolicy()
    val clock = HaloClock(policy)
}

val LocalHaloRuntime = staticCompositionLocalOf { HaloRuntime() }

/**
 * Creates the runtime for a theme root and keeps its policy current. [reducedMotion] comes from the theme's motion
 * (the system setting or the Showroom simulation); [timeScale] is the slow-mo factor for the clock.
 */
@Composable
fun rememberHaloRuntime(reducedMotion: Boolean, timeScale: Float): HaloRuntime {
    val runtime = remember { HaloRuntime() }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by lifecycle.currentStateFlow.collectAsState()
    SideEffect {
        runtime.policy.foreground = state.isAtLeast(Lifecycle.State.STARTED)
        runtime.policy.reducedMotion = reducedMotion
    }
    DisposableEffect(context) {
        val watcher = PolicyWatcher(context, runtime.policy).also { it.start() }
        onDispose { watcher.stop() }
    }
    runtime.clock.Drive(timeScale)
    return runtime
}
