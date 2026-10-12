package com.halo.core.designsystem.theme

import android.provider.Settings
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import com.halo.core.designsystem.foundation.HaloIndication
import com.halo.core.designsystem.runtime.HaloRuntime
import com.halo.core.designsystem.runtime.LocalHaloRuntime
import com.halo.core.designsystem.runtime.rememberHaloRuntime

// Tokens that change with the theme are dynamic locals: a theme switch then recomposes only the scopes that read
// them. A static local would invalidate the whole tree below its provider, and the Tide's first frame waits on
// that recomposition. Tokens that never change at runtime stay static (cheapest to read).
val LocalHaloColors = compositionLocalOf { haloDarkColors() }
val LocalHaloTypography = staticCompositionLocalOf { haloTypography() }
val LocalHaloSpacing = staticCompositionLocalOf { HaloSpacing() }
val LocalHaloSizes = staticCompositionLocalOf { HaloSizes() }
val LocalHaloStroke = staticCompositionLocalOf { HaloStroke() }
val LocalHaloRadius = staticCompositionLocalOf { HaloRadius() }
val LocalHaloEffects = compositionLocalOf { HaloEffects() }
val LocalHaloLight = compositionLocalOf { haloKeyLight(dark = true) }
val LocalHaloMaterials = compositionLocalOf { haloMaterials(dark = true) }
val LocalHaloMotion = compositionLocalOf { HaloMotion() }
val LocalHaloHaptics = staticCompositionLocalOf<HaloHaptics> {
    error("HaloHaptics not provided — wrap content in HaloTheme { }")
}

/** Color that text and icons inherit from their container (e.g. onFill inside a blue button). */
val LocalHaloContentColor = compositionLocalOf { Color.Unspecified }

/** Token accessor: `HaloTheme.colors.accent.fill`, `HaloTheme.motion.spring(HaloSpring.Snap)`, ... */
object HaloTheme {
    val colors: HaloColors
        @Composable @ReadOnlyComposable get() = LocalHaloColors.current
    val typography: HaloTypography
        @Composable @ReadOnlyComposable get() = LocalHaloTypography.current
    val spacing: HaloSpacing
        @Composable @ReadOnlyComposable get() = LocalHaloSpacing.current
    val sizes: HaloSizes
        @Composable @ReadOnlyComposable get() = LocalHaloSizes.current
    val stroke: HaloStroke
        @Composable @ReadOnlyComposable get() = LocalHaloStroke.current
    val radius: HaloRadius
        @Composable @ReadOnlyComposable get() = LocalHaloRadius.current
    val effects: HaloEffects
        @Composable @ReadOnlyComposable get() = LocalHaloEffects.current
    /** The scene light every material is lit by. */
    val light: HaloLight
        @Composable @ReadOnlyComposable get() = LocalHaloLight.current
    /** Physical material presets for relief surfaces. */
    val materials: HaloMaterials
        @Composable @ReadOnlyComposable get() = LocalHaloMaterials.current
    val motion: HaloMotion
        @Composable @ReadOnlyComposable get() = LocalHaloMotion.current
    val haptics: HaloHaptics
        @Composable @ReadOnlyComposable get() = LocalHaloHaptics.current
    /** Shared services (policy, frame clock): the only way components reach the system. */
    val runtime: HaloRuntime
        @Composable @ReadOnlyComposable get() = LocalHaloRuntime.current
    /** HDR emission: read `emission` / `headroom` in the draw phase only. */
    val hdr: HaloHdr
        @Composable @ReadOnlyComposable get() = LocalHaloHdr.current
}

/**
 * Root of the Halo design system. Dark and light ship together (decision D1): [colors] defaults to the [brand]'s
 * palette for the configuration's night mode. Apps usually start from `HaloApp`, which wraps this with the
 * appearance choice and the theme transition.
 *
 * @param brand the host app's palettes and fonts ([HaloBrand.Halo] or [haloBrand]).
 * @param light the scene light; null = the world-fixed key light, which turns as the phone turns (decision D6).
 * @param motion defaults to the system: "Remove animations" (animator scale 0) switches to reduced motion.
 * @param hapticObserver sees every haptic token played (Showroom haptic log).
 */
@Composable
fun HaloTheme(
    brand: HaloBrand = HaloBrand.Halo,
    colors: HaloColors = brand.colors(isSystemInDarkTheme()),
    light: HaloLight? = null,
    effects: HaloEffects = remember(colors.isDark) { haloEffects(colors.isDark) },
    materials: HaloMaterials = remember(colors.isDark) { haloMaterials(colors.isDark) },
    motion: HaloMotion = rememberSystemHaloMotion(),
    hapticObserver: ((HaloHaptic, Float) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val typography = remember(brand.fonts) { haloTypography(brand.fonts) }
    val runtime = rememberHaloRuntime(reducedMotion = motion.reduced, timeScale = motion.scale)
    val haptics = rememberHaloHaptics(hapticObserver, runtime)
    val hdr = runtime.scene.hdr
    val sceneLight = light ?: remember(colors.isDark, runtime) { haloKeyLight(colors.isDark, runtime.scene.rig) }
    val selection = remember(colors) {
        TextSelectionColors(
            handleColor = colors.accent.fill,
            backgroundColor = colors.accent.fill.copy(alpha = 0.32f),
        )
    }
    CompositionLocalProvider(
        LocalHaloColors provides colors,
        LocalHaloTypography provides typography,
        LocalHaloLight provides sceneLight,
        LocalHaloEffects provides effects,
        LocalHaloMaterials provides materials,
        LocalHaloMotion provides motion,
        LocalHaloHaptics provides haptics,
        LocalHaloHdr provides hdr,
        LocalHaloRuntime provides runtime,
        LocalHaloContentColor provides colors.content.primary,
        LocalIndication provides HaloIndication(RectangleShape),
        LocalTextSelectionColors provides selection,
        content = content,
    )
}

@Composable
fun rememberSystemHaloMotion(): HaloMotion {
    val context = LocalContext.current
    return remember(context) {
        val animatorScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        HaloMotion(reduced = animatorScale == 0f)
    }
}
