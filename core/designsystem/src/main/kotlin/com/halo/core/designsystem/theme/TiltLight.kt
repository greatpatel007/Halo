package com.halo.core.designsystem.theme

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sqrt

/*
 * The scene light, fixed in the world (decision D6). A light in a room doesn't move when you turn your phone, so its
 * direction *relative to the screen* turns by the phone's rotation, exactly as gravity does. Tracking gravity gives
 * that rotation; applying it to the key light makes every highlight, rim and light-caused shadow shift together.
 *
 * Practicalities, not physics: the holding angle you settle into becomes "neutral" again over a few seconds (so the
 * light isn't permanently off when the phone lies flat), the swing is capped, and it is smoothed. Gravity comes from
 * the runtime's shared sensors, which already stop in the background, under reduced motion, battery saver or heat;
 * when they stop, the light returns to its designed direction. The runtime's scene (`HaloScene`) owns the rig and
 * feeds it through [GravityFilter].
 */

/** Gravity → the rotation since the holding angle → [LightRig.rotation], smoothed. Pure: no platform calls. */
internal class GravityFilter(private val rig: LightRig) {
    private var reference: FloatArray? = null
    // The smoothed rotation as a rotation vector (axis × angle).
    private val current = FloatArray(3)
    private var lastNanos = 0L

    fun reset() {
        reference = null
        current.fill(0f)
        lastNanos = 0L
    }

    fun onGravity(values: FloatArray, timestampNanos: Long) {
        val dt = if (lastNanos == 0L) 0f else min((timestampNanos - lastNanos) / 1e9f, MAX_DT_S)
        lastNanos = timestampNanos
        // Device axes (x right, y up, z out) → screen axes (x right, y down, z out).
        val g = normalised(values[0], -values[1], values[2]) ?: return
        val ref = reference ?: g.copyOf().also { reference = it }
        val target = lightRotationVector(ref, g)
        // Smooth towards the target rotation; slowly re-centre the reference on the current holding angle.
        val a = 1f - exp(-dt / SMOOTH_S)
        for (i in 0..2) current[i] += (target[i] - current[i]) * a
        val r = 1f - exp(-dt / RECENTRE_S)
        for (i in 0..2) ref[i] += (g[i] - ref[i]) * r
        normalised(ref[0], ref[1], ref[2])?.copyInto(ref)
        publish()
    }

    /** Only writes the state when the light actually moved, so a still phone causes no redraws. */
    private fun publish() {
        val angle = sqrt(current[0] * current[0] + current[1] * current[1] + current[2] * current[2])
        val next = if (angle < 1e-4f) LightRotation.None
        else LightRotation(current[0] / angle, current[1] / angle, current[2] / angle, angle)
        val prev = rig.rotation
        val moved = abs(next.angle - prev.angle) > EPSILON ||
            abs(next.ax - prev.ax) + abs(next.ay - prev.ay) + abs(next.az - prev.az) > EPSILON
        if (moved) rig.rotation = next
    }

    private companion object {
        const val MAX_DT_S = 0.1f
        /** Smoothing of the light's motion (s). */
        const val SMOOTH_S = 0.12f
        /** How long a new holding angle takes to become neutral (s). */
        const val RECENTRE_S = 4f
        const val EPSILON = 0.002f

        fun normalised(x: Float, y: Float, z: Float): FloatArray? {
            val l = sqrt(x * x + y * y + z * z)
            return if (l < 1e-3f) null else floatArrayOf(x / l, y / l, z / l)
        }
    }
}

/** Largest swing of the light (rad, ≈30°). */
internal const val MAX_LIGHT_SWING = 0.52f

/** The rotation taking unit vector [from] to [to] (axis × angle), capped at [MAX_LIGHT_SWING]. */
internal fun lightRotationVector(from: FloatArray, to: FloatArray): FloatArray {
    val cx = from[1] * to[2] - from[2] * to[1]
    val cy = from[2] * to[0] - from[0] * to[2]
    val cz = from[0] * to[1] - from[1] * to[0]
    val sinA = sqrt(cx * cx + cy * cy + cz * cz)
    if (sinA < 1e-5f) return floatArrayOf(0f, 0f, 0f)
    val angle = min(asin(sinA.coerceAtMost(1f)), MAX_LIGHT_SWING)
    return floatArrayOf(cx / sinA * angle, cy / sinA * angle, cz / sinA * angle)
}
