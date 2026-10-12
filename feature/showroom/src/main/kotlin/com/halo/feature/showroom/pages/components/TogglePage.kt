package com.halo.feature.showroom.pages.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.halo.core.designsystem.host.HaloThemeMode
import com.halo.core.designsystem.host.HaloThemeSwitch
import com.halo.core.designsystem.host.LocalHaloAppearance
import com.halo.core.designsystem.component.HaloToggle
import com.halo.core.designsystem.component.HaloToggleSize
import com.halo.core.designsystem.component.HaloToggleTone
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.ActionChip
import com.halo.feature.showroom.ui.Knob
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.SpecLine

@Composable
internal fun TogglePage() {
    val appearance = checkNotNull(LocalHaloAppearance.current)
    val s = HaloTheme.spacing
    val t = HaloTheme.typography
    val colors = HaloTheme.colors
    val mode = appearance.mode

    // ---- 0. The real thing: the app's appearance switch.
    SectionLabel("Appearance · this switch themes the app", Modifier.enterRise(0))
    ShowroomCard(Modifier.enterRise(1)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                HaloText("Dark mode", style = t.subtitle)
                Spacer(Modifier.height(s.xxs))
                HaloText(
                    text = if (colors.isDark) "Soft-touch black · graphite thumb" else "Matte ceramic · white thumb",
                    style = t.bodySmall,
                    color = colors.content.secondary,
                )
            }
            Spacer(Modifier.width(s.md))
            HaloThemeSwitch()
        }
        Spacer(Modifier.height(s.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HaloText("mode ${mode.name.lowercase()}", style = t.data, color = colors.content.tertiary, modifier = Modifier.weight(1f))
            ActionChip("follow system", { appearance.setMode(HaloThemeMode.System) })
        }
    }

    // ---- 1. Playground
    var checked by rememberSaveable { mutableStateOf(true) }
    var tone by rememberSaveable { mutableStateOf(HaloToggleTone.Accent) }
    var size by rememberSaveable { mutableStateOf(HaloToggleSize.Large) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    SectionLabel("Playground · tap, press-and-hold, or drag", Modifier.enterRise(2))
    ShowroomCard(Modifier.enterRise(3)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(HaloTheme.sizes.controlXl * 2),
            contentAlignment = Alignment.Center,
        ) {
            HaloToggle(
                checked = checked,
                onCheckedChange = { checked = it },
                tone = tone,
                size = size,
                enabled = enabled,
                contentDescription = "Playground switch",
            )
        }
        Knob("Tone", HaloToggleTone.entries, tone, { tone = it }, optionLabel = { it.name })
        Spacer(Modifier.height(s.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(s.xl)) {
            Knob("Size", HaloToggleSize.entries, size, { size = it }, optionLabel = { if (it == HaloToggleSize.Large) "L" else "M" })
            Knob("Enabled", listOf(true, false), enabled, { enabled = it }, optionLabel = { if (it) "on" else "off" })
        }
    }

    // ---- 2. State matrix (static)
    SectionLabel("States · off / on / disabled", Modifier.enterRise(4))
    ShowroomCard(Modifier.enterRise(5)) {
        Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
            HaloToggleTone.entries.forEach { rowTone ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.lg)) {
                    HaloText(rowTone.name.lowercase(), style = t.data, color = colors.content.tertiary, modifier = Modifier.width(HaloTheme.sizes.controlXl + s.lg))
                    HaloToggle(checked = false, onCheckedChange = {}, tone = rowTone)
                    HaloToggle(checked = true, onCheckedChange = {}, tone = rowTone)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.lg)) {
                HaloText("disabled", style = t.data, color = colors.content.tertiary, modifier = Modifier.width(HaloTheme.sizes.controlXl + s.lg))
                HaloToggle(checked = false, onCheckedChange = {}, enabled = false)
                HaloToggle(checked = true, onCheckedChange = {}, enabled = false)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.lg)) {
                HaloText("rtl", style = t.data, color = colors.content.tertiary, modifier = Modifier.width(HaloTheme.sizes.controlXl + s.lg))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(horizontalArrangement = Arrangement.spacedBy(s.lg)) {
                        HaloToggle(checked = false, onCheckedChange = {})
                        HaloToggle(checked = true, onCheckedChange = {})
                    }
                }
            }
        }
    }

    // ---- 3. Spec
    SectionLabel("Spec", Modifier.enterRise(6))
    ShowroomCard(Modifier.enterRise(7)) {
        SpecLine("travel", "Mechanical · ζ 1.0 · k 400 · ~330ms")
        SpecLine("press", "lean 15% · sink 0.16H → 0.07H")
        SpecLine("drag", "1:1 · velocity × 0.12s projection")
        SpecLine("shadow", "scene light · top-left key")
        SpecLine("texture", "velvet grain · warm AO rim")
        SpecLine("haptic", "ToggleOn / ToggleOff")
    }
}
