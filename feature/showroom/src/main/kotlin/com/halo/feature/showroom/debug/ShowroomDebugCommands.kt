package com.halo.feature.showroom.debug

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Lets tooling drive the debug dock through deep-link query parameters, so screenshots of motion are
 * reproducible without tapping the dock:
 *
 *     halo://showroom/<page>?slowmo=5&grid=8&rm=1&log=0
 *
 * `slowmo` 1–10 · `grid` 4 | 8 | 0 · `rm` 1 = simulate reduced motion · `log` 0 = hide haptic log.
 */
object ShowroomDebugCommands {
    private val commands = MutableSharedFlow<Map<String, String>>(replay = 1, extraBufferCapacity = 4)

    val stream: SharedFlow<Map<String, String>> = commands.asSharedFlow()

    fun submit(params: Map<String, String>) {
        if (params.isNotEmpty()) commands.tryEmit(params)
    }
}

internal fun ShowroomDebugState.apply(params: Map<String, String>) {
    params["slowmo"]?.toFloatOrNull()?.let { slowMo = it.coerceIn(1f, 10f) }
    params["grid"]?.toIntOrNull()?.let { gridStep = it.takeIf { step -> step == 4 || step == 8 } }
    params["rm"]?.let { reducedMotion = it == "1" }
    params["log"]?.let { hapticLog = it != "0" }
}
