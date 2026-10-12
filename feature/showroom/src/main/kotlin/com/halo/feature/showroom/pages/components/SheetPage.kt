package com.halo.feature.showroom.pages.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.halo.core.designsystem.component.HaloButton
import com.halo.core.designsystem.component.HaloButtonSize
import com.halo.core.designsystem.component.HaloButtonVariant
import com.halo.core.designsystem.component.HaloSheet
import com.halo.core.designsystem.component.rememberHaloSheetState
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.Knob
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.SpecLine

@Composable
internal fun SheetPage() {
    val s = HaloTheme.spacing
    val t = HaloTheme.typography
    val colors = HaloTheme.colors
    var dismissible by rememberSaveable { mutableStateOf(true) }
    val tray = rememberHaloSheetState()
    val list = rememberHaloSheetState()
    var step by rememberSaveable { mutableIntStateOf(0) }

    // ---- 1. Playground
    SectionLabel("Playground · drag, throw, tap the dim, or go back", Modifier.enterRise(0))
    ShowroomCard(Modifier.enterRise(1)) {
        Column(verticalArrangement = Arrangement.spacedBy(s.md)) {
            HaloButton(
                "Open a tray",
                onClick = { step = 0; tray.show() },
                modifier = Modifier.fillMaxWidth(),
            )
            HaloButton(
                "Open a tall sheet",
                onClick = { list.show() },
                variant = HaloButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(s.xs))
            Knob("Dismissible", listOf(true, false), dismissible, { dismissible = it }, optionLabel = { if (it) "yes" else "no" })
        }
    }

    // ---- 2. States
    SectionLabel("States", Modifier.enterRise(2))
    ShowroomCard(Modifier.enterRise(3)) {
        SpecLine("tray (short content)", "one resting point: its own height")
        SpecLine("tall content", "half open → full; content scrolls when full")
        SpecLine("dragging", "1:1 with the finger; resists past the top")
        SpecLine("thrown", "momentum projected to the nearest point")
        SpecLine("not dismissible", "resists below its lowest point")
        SpecLine("back gesture", "shrinks toward its bottom edge; commit closes")
    }

    // ---- 3. Spec
    SectionLabel("Spec", Modifier.enterRise(4))
    ShowroomCard(Modifier.enterRise(5)) {
        SpecLine("material", "bg.raised slab · hairline · lit top edge")
        SpecLine("radius", "radius.sheet · 32")
        SpecLine("shadow", "contact shadow from the key light")
        SpecLine("open / placed", "Gentle (1.0 / 220)")
        SpecLine("thrown", "Throw (0.82 / 440), only after a fling")
        SpecLine("morph", "Standard (0.9 / 500) to the new height")
        SpecLine("release", "projected · rate 0.998 · nearest point")
        SpecLine("rubber band", "Apple c = 0.55")
        SpecLine("behind", "scale 0.95 · drop 8 · blur · dim scrim")
        SpecLine("haptics", "tick passing a point · Land on rest · tick on close")
    }

    HaloSheet(state = tray, title = "Send", dismissible = dismissible) {
        // A Family-style tray: one sheet whose height morphs between steps.
        Column(verticalArrangement = Arrangement.spacedBy(s.md)) {
            when (step) {
                0 -> {
                    HaloText("Send", style = t.title)
                    HaloText("21 000", style = t.amountLarge)
                    HaloText("sats to bc1q xy2k … hx0w", style = t.data, color = colors.content.secondary)
                    HaloButton("Review", onClick = { step = 1 }, modifier = Modifier.fillMaxWidth())
                }
                1 -> {
                    HaloText("Review", style = t.title)
                    listOf("Amount" to "21 000 sats", "Network fee" to "890 sats", "Arrives" to "about 30 min", "Total" to "21 890 sats")
                        .forEach { (k, v) -> SpecLine(k, v) }
                    HaloText(
                        "On-chain payments can't be reversed once they settle. Check the address before you send.",
                        style = t.bodySmall,
                        color = colors.content.secondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(s.sm), verticalAlignment = Alignment.CenterVertically) {
                        HaloButton("Back", onClick = { step = 0 }, variant = HaloButtonVariant.Secondary, size = HaloButtonSize.Medium)
                        HaloButton("Confirm", onClick = { step = 2 }, size = HaloButtonSize.Medium, modifier = Modifier.weight(1f))
                    }
                }
                else -> {
                    HaloText("Sent", style = t.title)
                    HaloText("Settling on-chain · about 30 min", style = t.body, color = colors.content.secondary)
                    HaloButton("Done", onClick = { tray.hide() }, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(s.lg))
        }
    }

    HaloSheet(state = list, title = "Activity", dismissible = dismissible) {
        HaloText("Activity", style = t.title)
        Spacer(Modifier.height(s.md))
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(s.md)) {
            repeat(ROWS) { i ->
                SpecLine(if (i % 3 == 0) "Received · Lightning" else "Sent · on-chain", if (i % 3 == 0) "+21 000" else "−150 000")
            }
            Spacer(Modifier.height(s.lg))
        }
    }
}

private const val ROWS = 30
