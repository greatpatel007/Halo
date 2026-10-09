package com.halo.feature.showroom.pages.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.halo.core.designsystem.component.HaloButton
import com.halo.core.designsystem.component.HaloButtonSize
import com.halo.core.designsystem.component.HaloButtonVariant
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.icons.HaloIcons
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.Knob
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.SpecLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ButtonPage() {
    val s = HaloTheme.spacing
    var variant by rememberSaveable { mutableStateOf(HaloButtonVariant.Primary) }
    var size by rememberSaveable { mutableStateOf(HaloButtonSize.XLarge) }
    var withIcon by rememberSaveable { mutableStateOf(true) }
    var fill by rememberSaveable { mutableStateOf(false) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ---- 1. Playground
    SectionLabel("Playground · tap = 2s fake request", Modifier.enterRise(0))
    ShowroomCard(Modifier.enterRise(1)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(HaloTheme.sizes.controlXl * 2),
            contentAlignment = Alignment.Center,
        ) {
            HaloButton(
                text = labelFor(variant),
                onClick = {
                    loading = true
                    scope.launch {
                        delay(2_000)
                        loading = false
                    }
                },
                modifier = if (fill) Modifier.fillMaxWidth() else Modifier,
                variant = variant,
                size = size,
                enabled = enabled,
                loading = loading,
                leadingIcon = if (withIcon) iconFor(variant) else null,
            )
        }
        Knob("Variant", HaloButtonVariant.entries, variant, { variant = it }, optionLabel = { it.name })
        Spacer(Modifier.height(s.lg))
        Knob("Size", HaloButtonSize.entries, size, { size = it }, optionLabel = { it.short() })
        Spacer(Modifier.height(s.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(s.xl)) {
            Knob("Icon", listOf(true, false), withIcon, { withIcon = it }, optionLabel = { if (it) "on" else "off" })
            Knob("Width", listOf(false, true), fill, { fill = it }, optionLabel = { if (it) "fill" else "wrap" })
        }
        Spacer(Modifier.height(s.lg))
        Knob("Enabled", listOf(true, false), enabled, { enabled = it }, optionLabel = { if (it) "on" else "off" })
    }

    // ---- 2. State matrix
    SectionLabel("States · default / disabled / loading", Modifier.enterRise(2))
    ShowroomCard(Modifier.enterRise(3)) {
        Column(verticalArrangement = Arrangement.spacedBy(s.md)) {
            HaloButtonVariant.entries.forEach { v ->
                Row(horizontalArrangement = Arrangement.spacedBy(s.sm), verticalAlignment = Alignment.CenterVertically) {
                    HaloButton(v.name, onClick = {}, variant = v, size = HaloButtonSize.Medium)
                    HaloButton("Off", onClick = {}, variant = v, size = HaloButtonSize.Medium, enabled = false)
                    HaloButton("Wait", onClick = {}, variant = v, size = HaloButtonSize.Medium, loading = true)
                }
            }
        }
    }

    SectionLabel("Sizes", Modifier.enterRise(4))
    ShowroomCard(Modifier.enterRise(5)) {
        Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
            HaloButtonSize.entries.forEach { sz ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HaloText(sz.short(), style = HaloTheme.typography.data, color = HaloTheme.colors.content.tertiary, modifier = Modifier.weight(0.2f))
                    HaloButton("Receive", onClick = {}, variant = HaloButtonVariant.Secondary, size = sz, leadingIcon = HaloIcons.Receive)
                }
            }
        }
    }

    // ---- 3. Spec
    SectionLabel("Spec", Modifier.enterRise(6))
    ShowroomCard(Modifier.enterRise(7)) {
        SpecLine("height", "36 · 48 · 56 · 64")
        SpecLine("press", "0.97 · corners → 12/16/16/20")
        SpecLine("press spring", "Snap in · Quick out")
        SpecLine("loading", "width → circle · Standard")
        SpecLine("spinner", "comet tail · 900ms/rev")
        SpecLine("glow", "0.55 → 1.0 with press")
        SpecLine("disabled", "alpha 0.38 · no haptic")
        SpecLine("haptic", "Tap on click")
    }
}

private fun HaloButtonSize.short() = when (this) {
    HaloButtonSize.Small -> "S"
    HaloButtonSize.Medium -> "M"
    HaloButtonSize.Large -> "L"
    HaloButtonSize.XLarge -> "XL"
}

private fun labelFor(variant: HaloButtonVariant) = when (variant) {
    HaloButtonVariant.Primary -> "Continue"
    HaloButtonVariant.Secondary -> "Receive"
    HaloButtonVariant.Ghost -> "Not now"
    HaloButtonVariant.Bitcoin -> "Send on-chain"
    HaloButtonVariant.Lightning -> "Pay invoice"
}

private fun iconFor(variant: HaloButtonVariant) = when (variant) {
    HaloButtonVariant.Primary -> HaloIcons.Send
    HaloButtonVariant.Secondary -> HaloIcons.Receive
    HaloButtonVariant.Ghost -> HaloIcons.Close
    HaloButtonVariant.Bitcoin -> HaloIcons.Bitcoin
    HaloButtonVariant.Lightning -> HaloIcons.Bolt
}
