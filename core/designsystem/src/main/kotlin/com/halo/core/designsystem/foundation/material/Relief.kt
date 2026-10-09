package com.halo.core.designsystem.foundation.material

import android.graphics.RuntimeShader
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import com.halo.core.designsystem.theme.HaloLight

/** How a surface responds to the scene light. Presets live with the components until the material engine lands. */
@Immutable
class ReliefMaterial(
    /** Albedo of flat, lit faces — renders exactly as this token. */
    val base: Color,
    /** Specular strength (multiplied by the light's own specular). */
    val specular: Float,
    /** Highlight tightness: soft-touch ~12, ceramic ~24, satin ~40. */
    val shininess: Float,
    /** Micro-surface grain, peak luma deviation (0..1). */
    val grain: Float,
    /** Ambient-occlusion strength inside recesses. */
    val ambientOcclusion: Float = 0.35f,
)

/** A channel sunk into a body (e.g. a switch track). */
@Immutable
class Recess(
    val bounds: Rect,
    val radius: Float,
    val bevel: Float,
    val depth: Float,
    /** Albedo of the recess floor. */
    val floor: Color,
)

/** A concave dish (finger rest) — parabolic, with a crisp rim. */
@Immutable
class Dish(val center: Offset, val radius: Float, val depth: Float)

/** Velvet fill colours: lit top → shaded bottom, with a warm occluded rim. */
@Immutable
class VelvetColors(val top: Color, val bottom: Color, val rim: Color)

/**
 * Owns the compiled shaders for one element's surfaces. Create once with `remember { ReliefPainter() }`;
 * each surface has its own shader instance so uniforms recorded for one draw can never leak into another.
 */
class ReliefPainter {
    private val body = RuntimeShader(RELIEF_AGSL)
    private val texture = RuntimeShader(VELVET_AGSL)
    private val shadow = RuntimeShader(SHADOW_AGSL)
    private val bodyBrush = ShaderBrush(body)
    private val textureBrush = ShaderBrush(texture)
    private val shadowBrush = ShaderBrush(shadow)

    /** Draws a lit relief body (with optional recess and dishes) into [bounds]. */
    fun drawRelief(
        scope: DrawScope,
        bounds: Rect,
        radius: Float,
        bevel: Float,
        height: Float,
        material: ReliefMaterial,
        light: HaloLight,
        recess: Recess? = null,
        dishA: Dish? = null,
        dishB: Dish? = null,
        seed: Float = 0f,
    ) {
        with(body) {
            setFloatUniform("body", bounds.left, bounds.top, bounds.right, bounds.bottom)
            setFloatUniform("bodyRadius", radius)
            setFloatUniform("bevel", bevel.coerceAtLeast(MIN_BEVEL))
            setFloatUniform("height", height)
            if (recess != null) {
                setFloatUniform("recess", recess.bounds.left, recess.bounds.top, recess.bounds.right, recess.bounds.bottom)
                setFloatUniform("recessRadius", recess.radius)
                setFloatUniform("recessBevel", recess.bevel.coerceAtLeast(MIN_BEVEL))
                setFloatUniform("recessDepth", recess.depth)
                setColorUniform("recessColor", recess.floor.toArgb())
            } else {
                setFloatUniform("recess", 0f, 0f, 0f, 0f)
                setFloatUniform("recessRadius", 0f)
                setFloatUniform("recessBevel", MIN_BEVEL)
                setFloatUniform("recessDepth", 0f)
                setColorUniform("recessColor", material.base.toArgb())
            }
            setDish("dishA", dishA)
            setDish("dishB", dishB)
            setColorUniform("baseColor", material.base.toArgb())
            setColorUniform("highlightColor", light.highlight.toArgb())
            setColorUniform("shadowColor", light.shadow.toArgb())
            setLight(light)
            setFloatUniform("ambient", light.ambient)
            setFloatUniform("diffuse", light.diffuse)
            setFloatUniform("specular", light.specular * material.specular)
            setFloatUniform("shininess", material.shininess)
            setFloatUniform("shadowOpacity", light.shadowOpacity)
            setFloatUniform("aoStrength", material.ambientOcclusion)
            setFloatUniform("grain", material.grain)
            setFloatUniform("seed", seed)
        }
        val area = bounds.inflate(AA_MARGIN)
        scope.drawRect(bodyBrush, topLeft = area.topLeft, size = area.size)
    }

    /** Draws the velvet texture layer inside [channel], horizontally limited to [from]..[to]. */
    fun drawVelvet(
        scope: DrawScope,
        channel: Recess,
        from: Float,
        to: Float,
        colors: VelvetColors,
        grain: Float,
        light: HaloLight,
        seed: Float = 0f,
    ) {
        if (to <= from) return
        with(texture) {
            setFloatUniform("channel", channel.bounds.left, channel.bounds.top, channel.bounds.right, channel.bounds.bottom)
            setFloatUniform("channelRadius", channel.radius)
            setFloatUniform("channelBevel", channel.bevel.coerceAtLeast(MIN_BEVEL))
            setFloatUniform("channelDepth", channel.depth)
            setFloatUniform("extent", from, to)
            setColorUniform("topColor", colors.top.toArgb())
            setColorUniform("bottomColor", colors.bottom.toArgb())
            setColorUniform("rimColor", colors.rim.toArgb())
            setColorUniform("shadowColor", light.shadow.toArgb())
            setLight(light)
            setFloatUniform("shadowOpacity", light.shadowOpacity)
            setFloatUniform("aoStrength", VELVET_RIM_STRENGTH)
            setFloatUniform("grain", grain)
            setFloatUniform("seed", seed)
        }
        val area = channel.bounds.inflate(AA_MARGIN)
        scope.drawRect(textureBrush, topLeft = area.topLeft, size = area.size)
    }

    /**
     * Draws the contact shadow of a rounded caster [elevation] px above the surface beneath it.
     * Offset and softness follow the scene light; [strength] scales the light's shadow opacity.
     */
    fun drawContactShadow(
        scope: DrawScope,
        caster: Rect,
        radius: Float,
        elevation: Float,
        light: HaloLight,
        strength: Float = 1f,
    ) {
        val offset = Offset(light.shadowDx * elevation, light.shadowDy * elevation)
        val blur = elevation * SHADOW_SOFTNESS + SHADOW_MIN_BLUR
        with(shadow) {
            setFloatUniform("caster", caster.left, caster.top, caster.right, caster.bottom)
            setFloatUniform("casterRadius", radius)
            setFloatUniform("offset", offset.x, offset.y)
            setFloatUniform("blur", blur)
            setFloatUniform("opacity", (light.shadowOpacity * strength).coerceIn(0f, 1f))
            setColorUniform("shadowColor", light.shadow.toArgb())
        }
        val area = caster.translate(offset).inflate(blur * 2f)
        scope.drawRect(shadowBrush, topLeft = area.topLeft, size = area.size)
    }

    private fun RuntimeShader.setDish(name: String, dish: Dish?) {
        if (dish == null) setFloatUniform(name, 0f, 0f, 0f, 0f)
        else setFloatUniform(name, dish.center.x, dish.center.y, dish.radius, dish.depth)
    }

    private fun RuntimeShader.setLight(light: HaloLight) {
        setFloatUniform("light", light.x, light.y, light.z)
    }

    private companion object {
        const val MIN_BEVEL = 0.5f
        const val AA_MARGIN = 1f
        const val VELVET_RIM_STRENGTH = 0.55f
        const val SHADOW_SOFTNESS = 0.9f
        const val SHADOW_MIN_BLUR = 1f
    }
}
