package com.halo.core.designsystem.runtime

import androidx.compose.runtime.Stable
import com.halo.core.designsystem.theme.GravityFilter
import com.halo.core.designsystem.theme.HaloHdr
import com.halo.core.designsystem.theme.LightRig
import com.halo.core.designsystem.theme.LightRotation

/**
 * The scene every surface is rendered in: the world-fixed key light's rotation ([rig]) and the display's HDR state
 * ([hdr]). Driven by the runtime's sensors and policy; components read it only through `HaloTheme.light` and
 * `HaloTheme.hdr`, in the draw phase.
 */
@Stable
class HaloScene internal constructor(policy: HaloPolicy, sensors: HaloSensors) {
    val rig = LightRig()
    val hdr = HaloHdr()

    init {
        // The light turns with the phone: gravity from the shared hub; when the hub stops it, the light settles back.
        val filter = GravityFilter(rig)
        sensors.subscribe(
            HaloSensor.Gravity,
            onStop = {
                filter.reset()
                rig.rotation = LightRotation.None
            },
        ) { values, time -> filter.onGravity(values, time) }
        // HDR emission costs power and heat: back to standard range in battery saver or when hot.
        policy.addListener { hdr.limited = policy.conserving }
    }
}
