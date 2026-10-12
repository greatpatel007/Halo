package com.halo.core.designsystem.host

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.halo.core.designsystem.foundation.HaloOverlayHost
import com.halo.core.designsystem.foundation.HaloThemeRevealHost
import com.halo.core.designsystem.theme.HaloBrand
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloMotion
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.core.designsystem.theme.rememberSystemHaloMotion
import kotlinx.coroutines.flow.drop

/**
 * The one call a host app makes: `setContent { HaloApp(brand) { YourNavHost() } }`. It sets up everything a Halo
 * screen relies on:
 * - [HaloTheme] with your [brand] (palettes and fonts) and the shared runtime (policy, clock, sensors, light, HDR,
 *   haptics and sound);
 * - the [appearance] choice, provided as [LocalHaloAppearance], and the Tide theme transition around the whole
 *   window, which also flips the system-bar icons as the wave crosses them;
 * - the overlay layer, where sheets, dialogs and toasts render above everything (the screen behind recedes);
 * - edge-to-edge drawing ([edgeToEdge]) on the canvas colour;
 * - the platform's per-app night mode, kept in step with the choice so the splash screen and system dialogs match
 *   (only when the activity handles `uiMode` changes itself; otherwise a switch would restart it mid-wave).
 *
 * @param motion defaults to the system ("Remove animations" switches to reduced motion).
 * @param hapticObserver sees every feedback token played (e.g. for an on-screen log in debug builds).
 */
@Composable
fun HaloApp(
    brand: HaloBrand = HaloBrand.Halo,
    appearance: HaloAppearance = rememberHaloAppearance(),
    modifier: Modifier = Modifier,
    motion: HaloMotion = rememberSystemHaloMotion(),
    edgeToEdge: Boolean = true,
    hapticObserver: ((HaloHaptic, Float) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (edgeToEdge && !view.isInEditMode) {
        LaunchedEffect(view) { view.context.findActivity()?.window?.let(WindowCompat::enableEdgeToEdge) }
    }
    PlatformNightMode(appearance)
    HaloTheme(brand = brand, colors = brand.colors(appearance.isDark()), motion = motion, hapticObserver = hapticObserver) {
        CompositionLocalProvider(LocalHaloAppearance provides appearance) {
            HaloThemeRevealHost(isDark = HaloTheme.colors.isDark, modifier = modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(HaloTheme.colors.bg.canvas),
                ) {
                    // Sheets, dialogs and toasts render in this layer, above the content and inside the Tide.
                    HaloOverlayHost { content() }
                }
            }
        }
    }
}

/**
 * Mirrors later changes of the choice into the platform's per-app night mode (Android 12+), which persists it for
 * the splash screen and system UI. The first value is not applied: the platform already persisted it.
 */
@Composable
private fun PlatformNightMode(appearance: HaloAppearance) {
    val context = LocalContext.current
    val handlesUiMode = remember(context) { context.findActivity()?.handlesUiMode() == true }
    LaunchedEffect(appearance, handlesUiMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@LaunchedEffect
        val uiModes = context.getSystemService(UiModeManager::class.java) ?: return@LaunchedEffect
        if (!handlesUiMode) {
            Log.w(TAG, "Add android:configChanges=\"uiMode\" to the activity so the splash screen follows the theme.")
            return@LaunchedEffect
        }
        snapshotFlow { appearance.mode }.drop(1).collect { mode ->
            uiModes.setApplicationNightMode(
                when (mode) {
                    HaloThemeMode.System -> UiModeManager.MODE_NIGHT_AUTO
                    HaloThemeMode.Light -> UiModeManager.MODE_NIGHT_NO
                    HaloThemeMode.Dark -> UiModeManager.MODE_NIGHT_YES
                },
            )
        }
    }
}

private fun Activity.handlesUiMode(): Boolean = runCatching {
    packageManager.getActivityInfo(componentName, 0).configChanges and ActivityInfo.CONFIG_UI_MODE != 0
}.getOrDefault(false)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val TAG = "HaloApp"
