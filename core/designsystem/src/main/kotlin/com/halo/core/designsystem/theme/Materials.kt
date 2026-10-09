package com.halo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.halo.core.designsystem.foundation.material.ReliefMaterial

/**
 * Physical material presets (decision D1). Dark = soft-touch matte black with satin graphite parts;
 * light = matte ceramic. Components choose a role here — never their own albedo, gloss or grain.
 */
@Immutable
class HaloMaterials(
    /** Housings and bodies: the surface controls are set into. */
    val body: ReliefMaterial,
    /** Raised, movable parts: switch thumbs, knobs. */
    val knob: ReliefMaterial,
    /** Floor of recessed channels and wells. */
    val recessFloor: Color,
    /** Fibre grain of velvet fills (the texture layer in switch channels). */
    val velvetGrain: Float,
)

fun haloMaterials(dark: Boolean): HaloMaterials = with(HaloPalette) {
    if (dark) {
        HaloMaterials(
            body = ReliefMaterial(base = Ink850, specular = 0.35f, shininess = 12f, grain = 0.035f, ambientOcclusion = 0.45f),
            knob = ReliefMaterial(base = Ink600, specular = 0.85f, shininess = 36f, grain = 0.02f),
            recessFloor = Ink950,
            velvetGrain = 0.10f,
        )
    } else {
        HaloMaterials(
            body = ReliefMaterial(base = Ceramic050, specular = 0.5f, shininess = 18f, grain = 0.018f, ambientOcclusion = 0.30f),
            knob = ReliefMaterial(base = Ceramic025, specular = 0.7f, shininess = 26f, grain = 0.012f),
            recessFloor = Ceramic150,
            velvetGrain = 0.08f,
        )
    }
}
