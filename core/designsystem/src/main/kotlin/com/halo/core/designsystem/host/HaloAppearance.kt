package com.halo.core.designsystem.host

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

/** The user's appearance choice. */
enum class HaloThemeMode { System, Light, Dark }

/**
 * Where the appearance choice lives. [HaloApp] reads it to pick the palette; [HaloThemeSwitch] (or any control, via
 * [LocalHaloAppearance]) changes it.
 *
 * Use [rememberHaloAppearance] for the built-in store, or the overload that adapts your own (a DataStore, a
 * ViewModel, a settings repository).
 */
@Stable
interface HaloAppearance {
    /** The current choice. Snapshot state: reading it recomposes on change. */
    val mode: HaloThemeMode

    /** Changes the choice. Must take effect in [mode] at once (the theme transition waits for it). */
    fun setMode(mode: HaloThemeMode)
}

/** Whether the app is dark right now: the choice, or the system's when following it. */
@Composable
@ReadOnlyComposable
fun HaloAppearance.isDark(): Boolean = when (mode) {
    HaloThemeMode.System -> isSystemInDarkTheme()
    HaloThemeMode.Light -> false
    HaloThemeMode.Dark -> true
}

/** The app's appearance, provided by [HaloApp]; null outside it. */
val LocalHaloAppearance = staticCompositionLocalOf<HaloAppearance?> { null }

/** The built-in store: the choice is kept in the app's private preferences and survives restarts. */
@Composable
fun rememberHaloAppearance(): HaloAppearance {
    val context = LocalContext.current
    return remember(context) { StoredAppearance.get(context) }
}

/**
 * Adapts an appearance you already store. [mode] is your stored value; [onModeChange] stores a new one. The choice
 * shows on the next frame, before your store confirms it (response first), and your store's value wins once it
 * arrives.
 */
@Composable
fun rememberHaloAppearance(mode: HaloThemeMode, onModeChange: (HaloThemeMode) -> Unit): HaloAppearance {
    val appearance = remember { HostedAppearance(mode) }
    SideEffect {
        appearance.onChange = onModeChange
        appearance.sync(mode)
    }
    return appearance
}

private class HostedAppearance(initial: HaloThemeMode) : HaloAppearance {
    private var stored by mutableStateOf(initial)
    /** The user's latest choice, shown until the store reports a new value. */
    private var chosen by mutableStateOf<HaloThemeMode?>(null)
    var onChange: (HaloThemeMode) -> Unit = {}

    override val mode: HaloThemeMode get() = chosen ?: stored

    override fun setMode(mode: HaloThemeMode) {
        chosen = mode
        onChange(mode)
    }

    fun sync(value: HaloThemeMode) {
        if (value == stored) return
        stored = value
        chosen = null
    }
}

private class StoredAppearance private constructor(context: Context) : HaloAppearance {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private var current by mutableStateOf(
        prefs.getString(KEY_MODE, null)?.let { name -> HaloThemeMode.entries.firstOrNull { it.name == name } }
            ?: HaloThemeMode.System,
    )

    override val mode: HaloThemeMode get() = current

    override fun setMode(mode: HaloThemeMode) {
        current = mode
        prefs.edit { putString(KEY_MODE, mode.name) }
    }

    companion object {
        private const val PREFS = "halo_appearance"
        private const val KEY_MODE = "mode"

        @Volatile private var instance: StoredAppearance? = null

        fun get(context: Context): StoredAppearance =
            instance ?: synchronized(this) {
                instance ?: StoredAppearance(context.applicationContext).also { instance = it }
            }
    }
}
