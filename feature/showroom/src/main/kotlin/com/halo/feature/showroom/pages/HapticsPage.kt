package com.halo.feature.showroom.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.halo.core.designsystem.foundation.HaloDepth
import com.halo.core.designsystem.foundation.HaloPressable
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.haloDepth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val usage = mapOf(
    HaloHaptic.Tap to "Buttons, taps",
    HaloHaptic.Tick to "Keypad, sliders, digit roll",
    HaloHaptic.ToggleOn to "Switch on",
    HaloHaptic.ToggleOff to "Switch off",
    HaloHaptic.Threshold to "Swipe / hold commit point",
    HaloHaptic.Confirm to "Success",
    HaloHaptic.Reject to "Invalid input + shake",
    HaloHaptic.Crescendo to "Hold-to-send charge",
    HaloHaptic.Celebrate to "Payment sent",
)

@Composable
internal fun HapticsPage() {
    val haptics = HaloTheme.haptics
    val scope = rememberCoroutineScope()
    val s = HaloTheme.spacing

    ShowroomCard(Modifier.enterRise(0)) {
        HaloText(
            "The emulator can't vibrate — every token also lands in the haptic log (dock → log). Feel-test on an Android 13+ device.",
            style = HaloTheme.typography.bodySmall,
            color = HaloTheme.colors.content.secondary,
        )
    }

    SectionLabel("Tokens", Modifier.enterRise(1))
    Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
        HaloHaptic.entries.chunked(2).forEachIndexed { row, pair ->
            Row(Modifier.enterRise(2 + row), horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                pair.forEach { token ->
                    HapticTile(token.name, usage[token].orEmpty(), Modifier.weight(1f)) { haptics.perform(token) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    SectionLabel("Composed", Modifier.enterRise(8))
    HapticTile("Charge", "10 ticks rising 0.1 → 1.0, then Threshold", Modifier.fillMaxWidth().enterRise(9)) {
        scope.launch {
            for (i in 1..10) {
                haptics.tick(i / 10f)
                delay(90)
            }
            haptics.perform(HaloHaptic.Threshold)
        }
    }
}

@Composable
private fun HapticTile(title: String, caption: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(HaloTheme.radius.lg)
    HaloPressable(
        onClick = onClick,
        haptic = null,
        shape = shape,
        onClickLabel = "Play $title",
        modifier = modifier
            .height(HaloTheme.sizes.controlXl + HaloTheme.spacing.xxl)
            .haloDepth(HaloDepth.Surface, shape),
    ) {
        Box(Modifier.padding(HaloTheme.spacing.lg), contentAlignment = Alignment.TopStart) {
            Column {
                HaloText(title, style = HaloTheme.typography.subtitle)
                Spacer(Modifier.weight(1f))
                HaloText(caption, style = HaloTheme.typography.data, color = HaloTheme.colors.content.tertiary, maxLines = 2)
            }
        }
    }
}
