package com.halo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The scene light (decision D4): one key light shared by every surface, so highlights, rims and
 * light-caused shadows agree everywhere. Default position is top-left, as in every reference.
 *
 * Flat faces render exactly at their token colour: ambient + diffuse == 1 in every theme.
 *
 * Coordinates: x → right, y → down (screen space), z → out of the screen towards the viewer. The vector
 * points *towards* the light. Shadows fall the opposite way, scaled by an element's height over the surface.
 *
 * The light is fixed in the world (decision D6): turn the phone and its direction relative to the screen turns the
 * other way, exactly like gravity does. [rig] carries that rotation as snapshot state; [x], [y], [z] read it, so
 * read them **in the draw phase only** (draw lambdas, shader uniforms): a tilt then redraws lit surfaces and never
 * recomposes.
 */
@Immutable
class HaloLight(
    x: Float,
    y: Float,
    z: Float,
    /** Light reaching faces turned away from the key light (0..1). */
    val ambient: Float,
    /** Strength of the Lambert (diffuse) term. */
    val diffuse: Float,
    /** Strength of specular highlights on glossy materials. */
    val specular: Float,
    /** Colour of the specular highlight. */
    val highlight: Color,
    /** Tone of light-caused shadows: black on soft-touch, cool blue-grey on ceramic. */
    val shadow: Color,
    /** Opacity of a contact shadow right under an element resting on the surface. */
    val shadowOpacity: Float,
    /** The device's rotation since the user's holding angle (null = a still light). */
    private val rig: LightRig? = null,
) {
    private val baseX: Float
    private val baseY: Float
    private val baseZ: Float

    init {
        val length = sqrt(x * x + y * y + z * z)
        baseX = x / length
        baseY = y / length
        baseZ = z / length
    }

    val x: Float get() = rotated(0)
    val y: Float get() = rotated(1)
    val z: Float get() = rotated(2)

    /** Rodrigues' rotation of the base direction by the rig's rotation; one component. */
    private fun rotated(component: Int): Float {
        val r = rig?.rotation ?: return when (component) { 0 -> baseX; 1 -> baseY; else -> baseZ }
        if (r.angle == 0f) return when (component) { 0 -> baseX; 1 -> baseY; else -> baseZ }
        val c = cos(r.angle)
        val sn = sin(r.angle)
        val dot = r.ax * baseX + r.ay * baseY + r.az * baseZ
        return when (component) {
            0 -> baseX * c + (r.ay * baseZ - r.az * baseY) * sn + r.ax * dot * (1 - c)
            1 -> baseY * c + (r.az * baseX - r.ax * baseZ) * sn + r.ay * dot * (1 - c)
            else -> (baseZ * c + (r.ax * baseY - r.ay * baseX) * sn + r.az * dot * (1 - c)).coerceAtLeast(MIN_Z)
        }
    }

    /** Screen-space shadow offset per unit of height (px of shift for 1px of elevation). */
    val shadowDx: Float get() = -x / z
    val shadowDy: Float get() = -y / z
}

/** A rotation as a unit axis and an angle (rad), in screen coordinates. */
@Immutable
data class LightRotation(val ax: Float, val ay: Float, val az: Float, val angle: Float) {
    companion object {
        val None = LightRotation(0f, 0f, 1f, 0f)
    }
}

/** Holds the device's rotation for the scene light (see `rememberLightRig`). */
@Stable
class LightRig {
    var rotation: LightRotation by mutableStateOf(LightRotation.None)
        internal set
}

/** The light never drops to the horizon: below this z, shading and shadows would degenerate. */
private const val MIN_Z = 0.2f

/** Key light top-left, tuned per theme. [rig] makes it world-fixed (it turns as the phone turns). */
fun haloKeyLight(dark: Boolean, rig: LightRig? = null): HaloLight = with(HaloPalette) {
    if (dark) {
        HaloLight(
            x = -0.45f, y = -0.62f, z = 0.64f,
            ambient = 0.42f,
            diffuse = 0.58f,
            specular = 0.55f,
            highlight = White,
            shadow = Black,
            shadowOpacity = 0.55f,
            rig = rig,
        )
    } else {
        HaloLight(
            x = -0.45f, y = -0.62f, z = 0.64f,
            ambient = 0.64f,
            diffuse = 0.36f,
            specular = 0.70f,
            highlight = White,
            shadow = Slate900Shadow,
            shadowOpacity = 0.26f,
            rig = rig,
        )
    }
}
