package com.halo.feature.showroom.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.halo.feature.showroom.ui.Knob
import com.halo.core.designsystem.theme.haloScheme
import com.halo.core.designsystem.theme.HaloContrast
import com.halo.core.designsystem.theme.HaloColorFamily
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.theme.HaloAccentColors
import com.halo.core.designsystem.theme.HaloPalette
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.core.designsystem.theme.contrast
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import java.util.Locale

internal class Swatch(val name: String, val color: Color, val showContrast: Boolean = false)

@Composable
internal fun ColorPage() {
    // Material 3 roles (proposal 0006), previewable at each contrast level.
    var level by rememberSaveable { mutableStateOf(HaloContrast.Standard) }
    val dark = HaloTheme.colors.isDark
    val scheme = remember(dark, level) { haloScheme(dark, level) }
    val groups = listOf(
        "Surfaces" to listOf(
            Swatch("surface", scheme.surface), Swatch("surfaceDim", scheme.surfaceDim), Swatch("surfaceBright", scheme.surfaceBright),
            Swatch("surfaceContainerLowest", scheme.surfaceContainerLowest), Swatch("surfaceContainerLow", scheme.surfaceContainerLow),
            Swatch("surfaceContainer", scheme.surfaceContainer), Swatch("surfaceContainerHigh", scheme.surfaceContainerHigh),
            Swatch("surfaceContainerHighest", scheme.surfaceContainerHighest),
        ),
        "On surface · outlines" to listOf(
            Swatch("onSurface", scheme.onSurface, true), Swatch("onSurfaceVariant", scheme.onSurfaceVariant, true),
            Swatch("outline", scheme.outline, true), Swatch("outlineVariant", scheme.outlineVariant),
        ),
        "Primary · brand blue (container = the brand seed)" to familySwatches("primary", scheme.primary),
        "Secondary · quiet UI" to familySwatches("secondary", scheme.secondary),
        "Tertiary" to familySwatches("tertiary", scheme.tertiary),
        "Error" to familySwatches("error", scheme.error),
        "Bitcoin · on-chain only (fill = seed)" to familySwatches("bitcoin", scheme.bitcoin),
        "Lightning · Lightning only (fill = seed)" to familySwatches("lightning", scheme.lightning),
        "Positive" to familySwatches("positive", scheme.positive),
        "Warning · coral" to familySwatches("warning", scheme.warning),
        "Inverse · scrim" to listOf(
            Swatch("inverseSurface", scheme.inverseSurface), Swatch("inverseOnSurface", scheme.inverseOnSurface),
            Swatch("inversePrimary", scheme.inversePrimary), Swatch("scrim", scheme.scrim),
        ),
    )
    var index = 0
    SectionLabel("Contrast level", Modifier.enterRise(index++))
    ShowroomCard(Modifier.enterRise(index++)) {
        Knob("Android 14+ contrast setting", HaloContrast.entries, level, { level = it }, optionLabel = { it.name.lowercase() })
    }
    groups.forEach { (title, swatches) ->
        SectionLabel(title, Modifier.enterRise(index++))
        ShowroomCard(Modifier.enterRise(index++)) {
            Column(verticalArrangement = Arrangement.spacedBy(HaloTheme.spacing.md)) {
                swatches.forEach { SwatchRow(it, scheme.surface) }
            }
        }
    }

    SectionLabel("Primitive ramps (materials, logo)", Modifier.enterRise(index++))
    Ramp("ink", listOf(
        HaloPalette.Ink950, HaloPalette.Ink900, HaloPalette.Ink850, HaloPalette.Ink800, HaloPalette.Ink700, HaloPalette.Ink600,
        HaloPalette.Ink500, HaloPalette.Ink400, HaloPalette.Ink300, HaloPalette.Ink200, HaloPalette.Ink100,
    ), Modifier.enterRise(index++))
    Spacer(Modifier.height(HaloTheme.spacing.md))
    Ramp("seeds", listOf(HaloPalette.Blue500, HaloPalette.Btc500, HaloPalette.Yellow500, HaloPalette.Green500, HaloPalette.Red500, HaloPalette.Coral500), Modifier.enterRise(index))
}

internal fun familySwatches(prefix: String, f: HaloColorFamily) = listOf(
    Swatch(prefix, f.color, true), Swatch("on${prefix.replaceFirstChar { it.uppercase() }}", f.onColor),
    Swatch("${prefix}Container", f.container), Swatch("on${prefix.replaceFirstChar { it.uppercase() }}Container", f.onContainer),
    Swatch("$prefix.fill (lit control)", f.fill),
)

internal fun accentSwatches(prefix: String, a: HaloAccentColors) = listOf(
    Swatch("$prefix.fill", a.fill), Swatch("$prefix.fillPressed", a.fillPressed),
    Swatch("$prefix.onFill", a.onFill), Swatch("$prefix.text", a.text, true),
    Swatch("$prefix.tint", a.tint), Swatch("$prefix.glow", a.glow),
)

@Composable
internal fun SwatchRow(swatch: Swatch, canvas: Color) {
    val spacing = HaloTheme.spacing
    val chipShape = RoundedCornerShape(HaloTheme.radius.xs)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(HaloTheme.sizes.controlSm)
                .background(canvas, chipShape)
                .background(swatch.color, chipShape)
                .border(HaloTheme.stroke.thin, HaloTheme.colors.line.default, chipShape),
        )
        Spacer(Modifier.width(spacing.md))
        Column(Modifier.weight(1f)) {
            HaloText(swatch.name, style = HaloTheme.typography.data)
            Spacer(Modifier.height(spacing.xxs))
            HaloText(hex(swatch.color), style = HaloTheme.typography.micro, color = HaloTheme.colors.content.tertiary)
        }
        if (swatch.showContrast) {
            val ratio = contrast(swatch.color.compositeOver(canvas), canvas)
            val grade = when {
                ratio >= 7f -> "AAA"
                ratio >= 4.5f -> "AA"
                ratio >= 3f -> "AA·L"
                else -> "—"
            }
            HaloText(
                text = String.format(Locale.US, "%.1f:1 %s", ratio, grade),
                style = HaloTheme.typography.data,
                color = if (ratio >= 4.5f) HaloTheme.colors.content.secondary else HaloTheme.colors.status.warning,
            )
        }
    }
}

@Composable
private fun Ramp(name: String, steps: List<Color>, modifier: Modifier = Modifier) {
    Column(modifier) {
        HaloText(name, style = HaloTheme.typography.data, color = HaloTheme.colors.content.secondary)
        Spacer(Modifier.height(HaloTheme.spacing.sm))
        Row(
            Modifier
                .fillMaxWidth()
                .height(HaloTheme.sizes.controlMd),
        ) {
            steps.forEachIndexed { i, color ->
                val shape = when (i) {
                    0 -> RoundedCornerShape(topStart = HaloTheme.radius.sm, bottomStart = HaloTheme.radius.sm)
                    steps.lastIndex -> RoundedCornerShape(topEnd = HaloTheme.radius.sm, bottomEnd = HaloTheme.radius.sm)
                    else -> RoundedCornerShape(HaloTheme.spacing.none)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(HaloTheme.sizes.controlMd)
                        .background(color, shape),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = HaloTheme.spacing.xs)) {
            HaloText(hex(steps.first()), style = HaloTheme.typography.micro, color = HaloTheme.colors.content.tertiary)
            Spacer(Modifier.weight(1f))
            HaloText(hex(steps.last()), style = HaloTheme.typography.micro, color = HaloTheme.colors.content.tertiary)
        }
    }
}

private fun hex(color: Color): String {
    val argb = color.toArgb()
    val rgb = String.format(Locale.US, "#%06X", argb and 0xFFFFFF)
    val alpha = (color.alpha * 100).toInt()
    return if (alpha < 100) "$rgb · $alpha%" else rgb
}
