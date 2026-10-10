package com.halo.core.designsystem.foundation

import android.graphics.RuntimeShader
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * Per-pixel film grain (AGSL). Breaks up banding in large dark gradients and gives hero surfaces a
 * tactile, printed feel. Static by design — animated grain costs battery for no real gain.
 */
private const val GRAIN_AGSL = """
uniform float intensity;
uniform float seed;

float hash(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

half4 main(float2 coord) {
    float n = hash(floor(coord) + seed) - 0.5;
    float a = abs(n) * 2.0 * intensity;
    half3 c = n > 0.0 ? half3(1.0) : half3(0.0);
    return half4(c * a, a);
}
"""

/** @param intensity peak grain opacity (0..1). Hero surfaces use ~0.05. */
fun Modifier.haloGrain(intensity: Float = 0.05f, seed: Float = 17f): Modifier = this then GrainElement(intensity, seed)

private data class GrainElement(val intensity: Float, val seed: Float) : ModifierNodeElement<GrainNode>() {
    override fun create() = GrainNode(intensity, seed)
    override fun update(node: GrainNode) = node.update(intensity, seed)
    override fun InspectorInfo.inspectableProperties() {
        name = "haloGrain"
        properties["intensity"] = intensity
        properties["seed"] = seed
    }
}

/**
 * Compiles the shader once per element. Recomposing with the same values keeps the cached draw; new values only
 * update the uniforms.
 */
private class GrainNode(private var intensity: Float, private var seed: Float) : DelegatingNode() {
    private val shader = RuntimeShader(GRAIN_AGSL)
    private val brush = ShaderBrush(shader)
    private val draw = delegate(
        CacheDrawModifierNode {
            shader.setFloatUniform("intensity", intensity)
            shader.setFloatUniform("seed", seed)
            onDrawWithContent {
                drawContent()
                drawRect(brush)
            }
        },
    )

    fun update(intensity: Float, seed: Float) {
        if (intensity == this.intensity && seed == this.seed) return
        this.intensity = intensity
        this.seed = seed
        draw.invalidateDrawCache()
    }
}
