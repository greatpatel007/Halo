package com.halo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.sqrt

/**
 * The scene light (decision D4): one key light shared by every surface, so highlights, rims and
 * light-caused shadows agree everywhere. Default position is top-left, as in every reference.
 *
 * Flat faces render exactly at their token colour: ambient + diffuse == 1 in every theme.
 *
 * Coordinates: x → right, y → down (screen space), z → out of the screen towards the viewer. The vector
 * points *towards* the light. Shadows fall the opposite way, scaled by an element's height over the surface.
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
) {
    val x: Float
    val y: Float
    val z: Float

    init {
        val length = sqrt(x * x + y * y + z * z)
        this.x = x / length
        this.y = y / length
        this.z = z / length
    }

    /** Screen-space shadow offset per unit of height (px of shift for 1px of elevation). */
    val shadowDx: Float get() = -x / z
    val shadowDy: Float get() = -y / z
}

/** Key light top-left, tuned per theme. */
fun haloKeyLight(dark: Boolean): HaloLight = with(HaloPalette) {
    if (dark) {
        HaloLight(
            x = -0.45f, y = -0.62f, z = 0.64f,
            ambient = 0.42f,
            diffuse = 0.58f,
            specular = 0.55f,
            highlight = White,
            shadow = Black,
            shadowOpacity = 0.55f,
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
        )
    }
}
