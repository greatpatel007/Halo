package com.halo.core.designsystem.foundation

import android.graphics.Bitmap
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import com.halo.core.designsystem.runtime.HaloPlatform
import kotlin.math.abs
import kotlin.random.Random

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
 * Compiles the shader once per element (Android 13+); older phones tile a pre-computed grain bitmap, which looks the
 * same at one grain per pixel. Recomposing with the same values keeps the cached draw; new values only update the
 * uniforms.
 */
private class GrainNode(private var intensity: Float, private var seed: Float) : DelegatingNode() {
    private val shader = if (HaloPlatform.shaders) RuntimeShader(GRAIN_AGSL) else null
    private val shaderBrush = shader?.let(::ShaderBrush)
    private val draw = delegate(
        CacheDrawModifierNode {
            val brush = if (shader != null && shaderBrush != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                shader.setFloatUniform("intensity", intensity)
                shader.setFloatUniform("seed", seed)
                shaderBrush
            } else {
                GrainTile.brush(intensity, seed)
            }
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

/** The fallback grain: a repeating tile of per-pixel noise, built once per (intensity, seed) and shared. */
private object GrainTile {
    private const val SIZE = 128
    private val cache = HashMap<Pair<Float, Float>, Brush>()

    fun brush(intensity: Float, seed: Float): Brush = cache.getOrPut(intensity to seed) {
        val random = Random(seed.toBits())
        val pixels = IntArray(SIZE * SIZE) {
            val n = random.nextFloat() - 0.5f
            val alpha = (abs(n) * 2f * intensity * CHANNEL_MAX).toInt().coerceIn(0, CHANNEL_MAX.toInt())
            val tone = if (n > 0f) CHANNEL_MAX.toInt() else 0
            (alpha shl 24) or (tone shl 16) or (tone shl 8) or tone
        }
        val bitmap = Bitmap.createBitmap(pixels, SIZE, SIZE, Bitmap.Config.ARGB_8888)
        ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }

    private const val CHANNEL_MAX = 255f
}
