package com.halo.core.designsystem.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
    /** The app is at least STARTED (visible to the user). */
    var foreground: Boolean by mutableStateOf(false)
        internal set

    /** Reduced motion: the system's "remove animations" or the Showroom simulation. */
    var reducedMotion: Boolean by mutableStateOf(false)
        internal set

    var powerSave: Boolean by mutableStateOf(false)
        internal set

    /** The phone is hot (thermal status SEVERE or worse): decorative motion stops so it can cool down. */
    var hot: Boolean by mutableStateOf(false)
        internal set

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

/** Keeps [HaloPolicy]'s power and thermal fields current from system events. */
internal class PolicyWatcher(context: Context, private val policy: HaloPolicy) {
    private val app = context.applicationContext
    private val power = app.getSystemService(PowerManager::class.java)
    private val saverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            policy.powerSave = power?.isPowerSaveMode == true
        }
    }
    private val thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
        policy.hot = status >= PowerManager.THERMAL_STATUS_SEVERE
    }

    fun start() {
        policy.powerSave = power?.isPowerSaveMode == true
        policy.hot = (power?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE) >= PowerManager.THERMAL_STATUS_SEVERE
        app.registerReceiver(saverReceiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        power?.addThermalStatusListener(app.mainExecutor, thermalListener)
    }

    fun stop() {
        app.unregisterReceiver(saverReceiver)
        power?.removeThermalStatusListener(thermalListener)
    }
}
