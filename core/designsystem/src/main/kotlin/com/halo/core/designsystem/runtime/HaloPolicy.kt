package com.halo.core.designsystem.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

/*
 * The Halo runtime: system-like services behind HaloTheme, so components never re-implement the nitty-gritty
 * (lifecycle, battery saver, thermal state, reduced motion, frame pacing). Components talk to it through
 * HaloTheme.runtime; only the runtime touches the platform services.
 */

/**
 * One answer to "may this run?" for the whole design system. Event-driven (broadcasts and listeners, no polling);
 * every field is snapshot state, so readers react when it changes.
 */
@Stable
class HaloPolicy internal constructor() {
    private var foregroundState by mutableStateOf(false)
    private var reducedState by mutableStateOf(false)
    private var powerSaveState by mutableStateOf(false)
    private var hotState by mutableStateOf(false)
    private var ringerState by mutableStateOf(true)
    private var touchSoundsState by mutableStateOf(true)
    private val listeners = mutableListOf<() -> Unit>()

    /**
     * Called synchronously on every change. Services that hold hardware (sensors, audio, the display's HDR mode) must
     * react even when no frames are produced, e.g. the moment the app goes to the background, so they can't wait for
     * a recomposition.
     */
    internal fun addListener(listener: () -> Unit) {
        listeners += listener
        listener()
    }

    /** The app is at least STARTED (visible to the user). */
    var foreground: Boolean
        get() = foregroundState
        internal set(value) = update(foregroundState != value) { foregroundState = value }

    /** Reduced motion: the system's "remove animations" or the Showroom simulation. */
    var reducedMotion: Boolean
        get() = reducedState
        internal set(value) = update(reducedState != value) { reducedState = value }

    var powerSave: Boolean
        get() = powerSaveState
        internal set(value) = update(powerSaveState != value) { powerSaveState = value }

    /** The phone is hot (thermal status SEVERE or worse): decorative motion stops so it can cool down. */
    var hot: Boolean
        get() = hotState
        internal set(value) = update(hotState != value) { hotState = value }

    /** The ringer is on normal (silent and vibrate mean no UI sounds). */
    var ringerNormal: Boolean
        get() = ringerState
        internal set(value) = update(ringerState != value) { ringerState = value }

    /** The system "touch sounds" setting. */
    var touchSounds: Boolean
        get() = touchSoundsState
        internal set(value) = update(touchSoundsState != value) { touchSoundsState = value }

    private inline fun update(changed: Boolean, write: () -> Unit) {
        if (!changed) return
        write()
        listeners.forEach { it() }
    }

    /** UI sounds may play (iOS etiquette: they follow the ringer and the touch-sounds setting). */
    val soundAllowed: Boolean get() = ringerNormal && touchSounds

    /** Power-hungry extras (HDR emission, long decorative feedback) back off in battery saver or when hot. */
    val conserving: Boolean get() = powerSave || hot

    /** Continuous, decorative motion (ambient effects) and the sensors that drive it may run. */
    val ambientAllowed: Boolean by derivedStateOf { foreground && !reducedMotion && !powerSave && !hot }

    /** Why ambient motion is paused, for the Showroom lab (null when it may run). */
    val pausedBecause: String? by derivedStateOf {
        when {
            !foreground -> "background"
            reducedMotion -> "reduced motion"
            powerSave -> "battery saver"
            hot -> "thermal"
            else -> null
        }
    }
}

/** Keeps [HaloPolicy]'s power, thermal and sound fields current from system events (no polling). */
internal class PolicyWatcher(context: Context, private val policy: HaloPolicy) {
    private val app = context.applicationContext
    private val power = app.getSystemService(PowerManager::class.java)
    private val audio = app.getSystemService(AudioManager::class.java)
    private val ringerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = readSound()
    }
    private val touchSoundsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = readSound()
    }

    private fun readSound() {
        policy.ringerNormal = (audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL) == AudioManager.RINGER_MODE_NORMAL
        policy.touchSounds = Settings.System.getInt(app.contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0
    }
    private val saverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            policy.powerSave = power?.isPowerSaveMode == true
        }
    }
    // Thermal status is reported from Android 10; older phones never count as hot (battery saver still applies).
    private val thermalListener: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        PowerManager.OnThermalStatusChangedListener { status -> policy.hot = status >= PowerManager.THERMAL_STATUS_SEVERE }
    } else {
        null
    }

    fun start() {
        policy.powerSave = power?.isPowerSaveMode == true
        app.registerReceiver(saverReceiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalListener is PowerManager.OnThermalStatusChangedListener) {
            policy.hot = (power?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE) >= PowerManager.THERMAL_STATUS_SEVERE
            power?.addThermalStatusListener(ContextCompat.getMainExecutor(app), thermalListener)
        }
        readSound()
        app.registerReceiver(ringerReceiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))
        app.contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SOUND_EFFECTS_ENABLED), false, touchSoundsObserver)
    }

    fun stop() {
        app.unregisterReceiver(saverReceiver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalListener is PowerManager.OnThermalStatusChangedListener) {
            power?.removeThermalStatusListener(thermalListener)
        }
        app.unregisterReceiver(ringerReceiver)
        app.contentResolver.unregisterContentObserver(touchSoundsObserver)
    }
}
