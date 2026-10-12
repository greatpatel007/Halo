package com.halo.feature.showroom.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.halo.core.designsystem.component.HaloButton
import com.halo.core.designsystem.component.HaloButtonSize
import com.halo.core.designsystem.component.HaloButtonVariant
import com.halo.core.designsystem.component.HaloToggle
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.core.designsystem.theme.LocalHaloColors
import com.halo.core.designsystem.theme.LocalHaloContentColor
import com.halo.core.designsystem.theme.contrast
import com.halo.core.designsystem.theme.haloBrand
import com.halo.feature.showroom.ui.Knob
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.SpecLine
import java.util.Locale

/** Example accents a host app might bring. Seeds are the dark theme's fill; everything else is derived. */
private enum class Seed(val color: Color) {
    Halo(Color(0xFFD4FF3A)),
    Coral(Color(0xFFFF6B5A)),
    Ocean(Color(0xFF3D7BFF)),
    Violet(Color(0xFF9B6BFF)),
    Mint(Color(0xFF3DDCB0)),
    Sun(Color(0xFFFFD23F)),
}

/**
 * The plug-in proof: one seed colour re-brands every component (`haloBrand(accent = …)` passed to `HaloApp`). Roles are
 * derived in OKLCH with the hue kept and contrast checked; motion, light, materials and feedback stay the system's.
 */
@Composable
internal fun BrandPage() {
    var seed by rememberSaveable { mutableStateOf(Seed.Ocean) }
    val brand = remember(seed) { haloBrand(accent = seed.color) }
    val colors = brand.colors(HaloTheme.colors.isDark)

    SectionLabel("Seed · haloBrand(accent = …)", Modifier.enterRise(0))
    ShowroomCard(Modifier.enterRise(1)) {
        Knob("Accent", Seed.entries, seed, { seed = it }, optionLabel = { it.name.lowercase() })
    }

    // The preview runs under the derived palette, exactly as a host app would.
    CompositionLocalProvider(LocalHaloColors provides colors, LocalHaloContentColor provides colors.content.primary) {
        SectionLabel("Components · re-branded live", Modifier.enterRise(2))
        ShowroomCard(Modifier.enterRise(3)) {
            var on by rememberSaveable { mutableStateOf(true) }
            Column(verticalArrangement = Arrangement.spacedBy(HaloTheme.spacing.md)) {
                HaloButton("Continue", onClick = {}, modifier = Modifier.fillMaxWidth())
                HaloButton("Secondary", onClick = {}, variant = HaloButtonVariant.Secondary, size = HaloButtonSize.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HaloText("Notifications", style = HaloTheme.typography.subtitle, modifier = Modifier.weight(1f))
                    HaloToggle(checked = on, onCheckedChange = { on = it }, contentDescription = "Notifications")
                }
                HaloText("Accent text on the canvas", style = HaloTheme.typography.body, color = colors.accent.text)
            }
        }

        SectionLabel("Derived roles · contrast checked", Modifier.enterRise(4))
        ShowroomCard(Modifier.enterRise(5)) {
            Column(verticalArrangement = Arrangement.spacedBy(HaloTheme.spacing.md)) {
                accentSwatches("accent", colors.accent).forEach { SwatchRow(it, colors.bg.canvas) }
            }
            Spacer(Modifier.height(HaloTheme.spacing.md))
            SpecLine("label on fill", String.format(Locale.US, "%.1f:1", contrast(colors.accent.onFill, colors.accent.fill)))
        }
    }
}
