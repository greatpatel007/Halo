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
import com.halo.core.designsystem.feedback.HapticTier
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.Knob
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.SpecLine
import com.halo.feature.showroom.ui.haloDepth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val usage = mapOf(
    HaloHaptic.Tap to "buttons · click",
    HaloHaptic.Tick to "steps · tick",
    HaloHaptic.ToggleOn to "on · click up",
    HaloHaptic.ToggleOff to "off · click down",
    HaloHaptic.Threshold to "commit · firm click",
    HaloHaptic.Confirm to "success · chime",
    HaloHaptic.Reject to "invalid · thud",
    HaloHaptic.Crescendo to "charge · air",
    HaloHaptic.Celebrate to "sent · arpeggio",
    HaloHaptic.Tide to "theme · water rush",
    HaloHaptic.Ping to "glass halo · tink",
    HaloHaptic.Land to "sheet lands · soft knock",
)

private val tierInfo = mapOf(
    HapticTier.Envelope to "Envelope: intensity + sharpness curves",
    HapticTier.Primitives to "Primitives: OEM-tuned clicks, ticks, rises",
    HapticTier.Amplitude to "Amplitude: synthesised waveforms",
    HapticTier.Basic to "Basic motor: strong moments only",
    HapticTier.None to "No vibrator: sound and log only",
)

@Composable
internal fun HapticsPage() {
    val haptics = HaloTheme.haptics
    val scope = rememberCoroutineScope()
    val s = HaloTheme.spacing

    // ---- This phone: how Halo renders feedback here, and the in-app switches.
    val caps = haptics.capabilities
    SectionLabel("This phone", Modifier.enterRise(0))
    ShowroomCard(Modifier.enterRise(1)) {
        HaloText(tierInfo[haptics.tier].orEmpty(), style = HaloTheme.typography.subtitle)
        Spacer(Modifier.height(s.md))
        SpecLine("tier", haptics.tier.name.lowercase())
        SpecLine("primitives", if (caps.primitives.isEmpty()) "none" else caps.primitives.keys.joinToString(" ") { it.name.lowercase() })
        SpecLine("envelope", if (caps.envelope) "yes · ${caps.minControlPointMs}ms min point" else "no")
        SpecLine("amplitude control", if (caps.amplitudeControl) "yes" else "no")
        SpecLine("q factor", if (caps.qFactor.isNaN()) "unknown" else "%.1f".format(caps.qFactor))
        Spacer(Modifier.height(s.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(s.xl)) {
            Knob("Haptics", listOf(true, false), haptics.hapticsEnabled, { haptics.hapticsEnabled = it }, optionLabel = { if (it) "on" else "off" })
            Knob("Sound", listOf(true, false), haptics.soundEnabled, { haptics.soundEnabled = it }, optionLabel = { if (it) "on" else "off" })
        }
        Spacer(Modifier.height(s.lg))
        Knob(
            "Strength · calibrate per phone",
            listOf(0.6f, 0.8f, 1f, 1.2f, 1.4f),
            haptics.strength,
            { haptics.strength = it },
            optionLabel = { "×%.1f".format(it) },
        )
        Spacer(Modifier.height(s.md))
        HaloText(
            "Sound follows silent mode and the system touch-sounds setting. The emulator can't vibrate: every token also lands in the log (dock → log).",
            style = HaloTheme.typography.bodyMedium,
            color = HaloTheme.colors.content.secondary,
        )
    }

    SectionLabel("Tokens · left tiles play left, right tiles right", Modifier.enterRise(2))
    Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
        HaloHaptic.entries.chunked(2).forEachIndexed { row, pair ->
            Row(Modifier.enterRise(3 + row), horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                pair.forEachIndexed { column, token ->
                    val pan = if (column == 0) -0.6f else 0.6f
                    HapticTile(token.name, usage[token].orEmpty(), Modifier.weight(1f)) { haptics.perform(token, pan) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    SectionLabel("Composed", Modifier.enterRise(8))
    HapticTile("Charge", "10 ticks rising in strength and pitch, then Threshold", Modifier.fillMaxWidth().enterRise(9)) {
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
