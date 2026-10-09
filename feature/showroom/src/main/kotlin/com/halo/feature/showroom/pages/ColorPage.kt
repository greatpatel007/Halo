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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.theme.HaloAccentColors
import com.halo.core.designsystem.theme.HaloPalette
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import java.util.Locale

private class Swatch(val name: String, val color: Color, val showContrast: Boolean = false)

@Composable
internal fun ColorPage() {
    val c = HaloTheme.colors
    val groups = listOf(
        "Background" to listOf(
            Swatch("bg.canvas", c.bg.canvas), Swatch("bg.surface", c.bg.surface),
            Swatch("bg.raised", c.bg.raised), Swatch("bg.glass", c.bg.glass), Swatch("bg.scrim", c.bg.scrim),
        ),
        "Content" to listOf(
            Swatch("content.primary", c.content.primary, true), Swatch("content.secondary", c.content.secondary, true),
            Swatch("content.tertiary", c.content.tertiary, true), Swatch("content.disabled", c.content.disabled, true),
        ),
        "Line" to listOf(
            Swatch("line.subtle", c.line.subtle), Swatch("line.default", c.line.default),
            Swatch("line.strong", c.line.strong), Swatch("line.focus", c.line.focus), Swatch("line.highlight", c.line.highlight),
        ),
        "Accent · volt" to accentSwatches("accent", c.accent),
        "Bitcoin · BTC & Lightning only" to accentSwatches("bitcoin", c.bitcoin),
        "Status" to listOf(
            Swatch("status.positive", c.status.positive, true), Swatch("status.positiveTint", c.status.positiveTint),
            Swatch("status.negative", c.status.negative, true), Swatch("status.negativeTint", c.status.negativeTint),
            Swatch("status.warning", c.status.warning, true), Swatch("status.warningTint", c.status.warningTint),
        ),
    )
    var index = 0
    groups.forEach { (title, swatches) ->
        SectionLabel(title, Modifier.enterRise(index++))
        ShowroomCard(Modifier.enterRise(index++)) {
            Column(verticalArrangement = Arrangement.spacedBy(HaloTheme.spacing.md)) {
                swatches.forEach { SwatchRow(it, c.bg.canvas) }
            }
        }
    }

    SectionLabel("Primitive ramps", Modifier.enterRise(index++))
    Ramp("ink", listOf(
        HaloPalette.Ink950, HaloPalette.Ink900, HaloPalette.Ink850, HaloPalette.Ink800, HaloPalette.Ink700, HaloPalette.Ink600,
        HaloPalette.Ink500, HaloPalette.Ink400, HaloPalette.Ink300, HaloPalette.Ink200, HaloPalette.Ink100,
    ), Modifier.enterRise(index++))
    Spacer(Modifier.height(HaloTheme.spacing.md))
    Ramp("volt", listOf(HaloPalette.Volt900, HaloPalette.Volt600, HaloPalette.Volt500, HaloPalette.Volt400, HaloPalette.Volt300), Modifier.enterRise(index++))
    Spacer(Modifier.height(HaloTheme.spacing.md))
    Ramp("btc", listOf(HaloPalette.Btc900, HaloPalette.Btc600, HaloPalette.Btc500, HaloPalette.Btc400, HaloPalette.Btc300), Modifier.enterRise(index))
}

private fun accentSwatches(prefix: String, a: HaloAccentColors) = listOf(
    Swatch("$prefix.fill", a.fill), Swatch("$prefix.fillPressed", a.fillPressed),
    Swatch("$prefix.onFill", a.onFill), Swatch("$prefix.text", a.text, true),
    Swatch("$prefix.tint", a.tint), Swatch("$prefix.glow", a.glow),
)

@Composable
private fun SwatchRow(swatch: Swatch, canvas: Color) {
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

/** WCAG 2.x contrast ratio. */
private fun contrast(a: Color, b: Color): Float {
    val l1 = a.luminance()
    val l2 = b.luminance()
    return (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)
}
