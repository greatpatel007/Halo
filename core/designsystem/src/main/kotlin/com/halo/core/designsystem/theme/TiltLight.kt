package com.halo.core.designsystem.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The scene light, fixed in the world (decision D6). A light in a room doesn't move when you turn your phone, so its
 * direction *relative to the screen* turns by the phone's rotation, exactly as gravity does. Tracking gravity gives
 * that rotation; applying it to the key light makes every highlight, rim and light-caused shadow shift together.
 *
 * Practicalities, not physics: the holding angle you settle into becomes "neutral" again over a few seconds (so the
 * light isn't permanently off when the phone lies flat), the swing is capped, and it is smoothed. The sensor runs
 * only while the app is in the foreground, at ~30 Hz, and never under reduced motion or battery saver.
 */
@Composable
fun rememberLightRig(): LightRig {
    val context = LocalContext.current
    val rig = remember { LightRig() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by lifecycle.currentStateFlow.collectAsState()
    val reduced = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val running = state.isAtLeast(Lifecycle.State.STARTED) && !reduced
    DisposableEffect(running) {
        val tracker = if (running) GravityTracker(context, rig).also { it.start() } else null
        onDispose {
            tracker?.stop()
            rig.rotation = LightRotation.None
        }
    }
    return rig
}

/** Gravity → the rotation since the holding angle → [LightRig.rotation], smoothed. */
private class GravityTracker(context: Context, private val rig: LightRig) : SensorEventListener {
    private val sensors = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val power = context.getSystemService(PowerManager::class.java)
    private val sensor = sensors?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var reference: FloatArray? = null
    // The smoothed rotation as a rotation vector (axis × angle).
    private val current = FloatArray(3)
    private var lastNanos = 0L
    private var sincePowerCheck = POWER_CHECK_S
    private var saving = false

    fun start() {
        val s = sensor ?: return
        sensors?.registerListener(this, s, SAMPLE_PERIOD_US)
    }

    fun stop() {
        sensors?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val dt = if (lastNanos == 0L) 0f else min((event.timestamp - lastNanos) / 1e9f, MAX_DT_S)
        lastNanos = event.timestamp
        sincePowerCheck += dt
        if (sincePowerCheck >= POWER_CHECK_S) {
            sincePowerCheck = 0f
            saving = power?.isPowerSaveMode == true
        }
        if (saving) {
            if (rig.rotation != LightRotation.None) rig.rotation = LightRotation.None
            return
        }
        // Sensor axes (x right, y up, z out) → screen axes (x right, y down, z out).
        val g = normalised(event.values[0], -event.values[1], event.values[2]) ?: return
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
        val moved = kotlin.math.abs(next.angle - prev.angle) > EPSILON ||
            kotlin.math.abs(next.ax - prev.ax) + kotlin.math.abs(next.ay - prev.ay) + kotlin.math.abs(next.az - prev.az) > EPSILON
        if (moved) rig.rotation = next
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        /** ~30 Hz. */
        private const val SAMPLE_PERIOD_US = 33_333
        private const val MAX_DT_S = 0.1f
        /** Smoothing of the light's motion (s). */
        private const val SMOOTH_S = 0.12f
        /** How long a new holding angle takes to become neutral (s). */
        private const val RECENTRE_S = 4f
        private const val EPSILON = 0.002f
        private const val POWER_CHECK_S = 1f

        fun normalised(x: Float, y: Float, z: Float): FloatArray? {
            val l = sqrt(x * x + y * y + z * z)
            return if (l < 1e-3f) null else floatArrayOf(x / l, y / l, z / l)
        }

    }
}

/** The theme's key light, world-fixed through [rememberLightRig]. */
@Composable
fun rememberKeyLight(dark: Boolean): HaloLight {
    val rig = rememberLightRig()
    return remember(dark, rig) { haloKeyLight(dark, rig) }
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
