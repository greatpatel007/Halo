package com.halo.feature.showroom.debug

import com.halo.core.designsystem.runtime.HaloPlatform
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Lets tooling drive the debug dock through deep-link query parameters, so screenshots of motion are
 * reproducible without tapping the dock:
 *
 *     halo://showroom/<page>?slowmo=5&grid=8&rm=1&log=0&legacy=1
 *
 * `slowmo` 1–10 · `grid` 4 | 8 | 0 · `rm` 1 = simulate reduced motion · `log` 0 = hide haptic log ·
 * `legacy` 1 = render as an Android 8–12 phone (no shaders, no blur; applies to effects created afterwards).
 */
object ShowroomDebugCommands {
    private val commands = MutableSharedFlow<Map<String, String>>(replay = 1, extraBufferCapacity = 4)

    val stream: SharedFlow<Map<String, String>> = commands.asSharedFlow()

    fun submit(params: Map<String, String>) {
        // Applied at once, before the page composes: effects pick their rendering path when they are created.
        params["legacy"]?.let { HaloPlatform.simulateLegacy = it == "1" }
        if (params.isNotEmpty()) commands.tryEmit(params)
    }
}

internal fun ShowroomDebugState.apply(params: Map<String, String>) {
    params["slowmo"]?.toFloatOrNull()?.let { slowMo = it.coerceIn(1f, 10f) }
    params["grid"]?.toIntOrNull()?.let { gridStep = it.takeIf { step -> step == 4 || step == 8 } }
    params["rm"]?.let { reducedMotion = it == "1" }
    params["log"]?.let { hapticLog = it != "0" }
}
