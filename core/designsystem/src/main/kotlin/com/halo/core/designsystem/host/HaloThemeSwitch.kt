package com.halo.core.designsystem.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.halo.core.designsystem.component.HaloToggle
import com.halo.core.designsystem.component.HaloToggleSize
import com.halo.core.designsystem.foundation.LocalHaloThemeReveal
import com.halo.core.designsystem.theme.HaloTheme
import kotlinx.coroutines.launch

/**
 * The app's dark-mode switch (decision D2): a Large [HaloToggle] whose value is the current theme. Flipping it sets
 * the [appearance] to Dark or Light and runs the Tide transition, growing from the switch itself. Drop it anywhere
 * inside [HaloApp].
 *
 * Response first (Apple, fluid interfaces): the thumb takes the new position on the very next frame, from local
 * state, instead of waiting for the app-wide theme change to land; the Tide then grows out of the switch.
 */
@Composable
fun HaloThemeSwitch(
    modifier: Modifier = Modifier,
    appearance: HaloAppearance = checkNotNull(LocalHaloAppearance.current) { "HaloThemeSwitch must be inside HaloApp { }" },
    contentDescription: String = "Dark mode",
) {
    val dark = HaloTheme.colors.isDark
    val motion = HaloTheme.motion
    val reveal = LocalHaloThemeReveal.current
    val scope = rememberCoroutineScope()
    var bounds by remember { mutableStateOf(Rect.Zero) }
    // The choice the user just made, shown until the theme catches up (or reverts, if it never does).
    var pending by remember { mutableStateOf<Boolean?>(null) }
    var taps by remember { mutableIntStateOf(0) }
    LaunchedEffect(dark) { if (pending == dark) pending = null }
    // The switch's visible housing (its touch target is taller): the part of the screen that changes first.
    val housing = with(LocalDensity.current) { HaloTheme.sizes.switchLg.toSize() }
    HaloToggle(
        checked = pending ?: dark,
        onCheckedChange = { toDark ->
            val mode = if (toDark) HaloThemeMode.Dark else HaloThemeMode.Light
            pending = toDark
            val tap = ++taps
            val trigger = Rect(bounds.center - Offset(housing.width / 2f, housing.height / 2f), housing)
            scope.launch {
                try {
                    if (reveal != null) reveal.reveal(trigger, motion) { appearance.setMode(mode) }
                    else appearance.setMode(mode)
                } finally {
                    if (tap == taps) pending = null
                }
            }
        },
        size = HaloToggleSize.Large,
        contentDescription = contentDescription,
        modifier = modifier.onGloballyPositioned { bounds = it.boundsInRoot() },
    )
}
