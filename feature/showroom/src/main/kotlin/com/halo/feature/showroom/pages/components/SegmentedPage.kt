package com.halo.feature.showroom.pages.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.halo.core.designsystem.component.HaloSegment
import com.halo.core.designsystem.component.HaloSegmentedControl
import com.halo.core.designsystem.component.HaloSegmentedSize
import com.halo.core.designsystem.foundation.HaloPaymentNetwork
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.icons.HaloIcons
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.Knob
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.SpecLine

private val UNITS = listOf(HaloSegment("sats"), HaloSegment("BTC"), HaloSegment("USD"))
private val NETWORKS = listOf(
    HaloSegment("On-chain", HaloIcons.Bitcoin, HaloPaymentNetwork.Onchain),
    HaloSegment("Lightning", HaloIcons.Bolt, HaloPaymentNetwork.Lightning),
)
private val FEES = listOf(HaloSegment("Slow"), HaloSegment("Normal"), HaloSegment("Fast"))
private val RANGES = listOf(HaloSegment("1D"), HaloSegment("1W"), HaloSegment("1M"), HaloSegment("1Y"), HaloSegment("All"))

private enum class Demo(val label: String, val segments: List<HaloSegment>) {
    Unit("unit", UNITS),
    Network("network", NETWORKS),
    Fee("fee", FEES),
    Range("range", RANGES),
}

@Composable
internal fun SegmentedPage() {
    val s = HaloTheme.spacing
    val t = HaloTheme.typography
    val colors = HaloTheme.colors

    // ---- 1. Playground
    var demo by rememberSaveable { mutableStateOf(Demo.Network) }
    var size by rememberSaveable { mutableStateOf(HaloSegmentedSize.Large) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    SectionLabel("Playground · tap, or grab the thumb and throw it", Modifier.enterRise(0))
    ShowroomCard(Modifier.enterRise(1)) {
        HaloSegmentedControl(
            segments = demo.segments,
            selectedIndex = selected.coerceAtMost(demo.segments.lastIndex),
            onSelect = { selected = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            size = size,
        )
        Spacer(Modifier.height(s.sm))
        HaloText(
            text = "selected ${demo.segments[selected.coerceAtMost(demo.segments.lastIndex)].label}",
            style = t.data,
            color = colors.content.tertiary,
        )
        Spacer(Modifier.height(s.lg))
        Knob("Options", Demo.entries, demo, { demo = it; selected = 0 }, optionLabel = { it.label })
        Spacer(Modifier.height(s.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(s.xl)) {
            Knob("Size", HaloSegmentedSize.entries, size, { size = it }, optionLabel = { if (it == HaloSegmentedSize.Large) "L" else "M" })
            Knob("Enabled", listOf(true, false), enabled, { enabled = it }, optionLabel = { if (it) "on" else "off" })
        }
    }

    // ---- 2. State matrix (static)
    SectionLabel("States · accent / network / disabled / rtl", Modifier.enterRise(2))
    ShowroomCard(Modifier.enterRise(3)) {
        Column(verticalArrangement = Arrangement.spacedBy(s.md)) {
            MatrixRow("units · M") { HaloSegmentedControl(UNITS, 1, {}, Modifier.fillMaxWidth()) }
            MatrixRow("on-chain · L") {
                HaloSegmentedControl(NETWORKS, 0, {}, Modifier.fillMaxWidth(), size = HaloSegmentedSize.Large)
            }
            MatrixRow("lightning · L") {
                HaloSegmentedControl(NETWORKS, 1, {}, Modifier.fillMaxWidth(), size = HaloSegmentedSize.Large)
            }
            MatrixRow("range · 5") { HaloSegmentedControl(RANGES, 4, {}, Modifier.fillMaxWidth()) }
            MatrixRow("disabled") { HaloSegmentedControl(FEES, 1, {}, Modifier.fillMaxWidth(), enabled = false) }
            MatrixRow("rtl") {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    HaloSegmentedControl(FEES, 0, {}, Modifier.fillMaxWidth())
                }
            }
        }
    }

    // ---- 3. Spec
    SectionLabel("Spec", Modifier.enterRise(4))
    ShowroomCard(Modifier.enterRise(5)) {
        SpecLine("travel", "Mechanical · ζ 1.0 · k 400 · ~330ms")
        SpecLine("press", "lean 15% · sink 0.14H → 0.06H")
        SpecLine("drag", "on the thumb · 1:1 · 0.12s projection")
        SpecLine("ends", "rubber band · ≤ 12% squash")
        SpecLine("colour", "accent · orange · yellow")
        SpecLine("haptic", "Tick per detent · passing 0.45")
    }
}

@Composable
private fun MatrixRow(label: String, content: @Composable () -> Unit) {
    Column {
        HaloText(label, style = HaloTheme.typography.data, color = HaloTheme.colors.content.tertiary)
        Spacer(Modifier.height(HaloTheme.spacing.xs))
        content()
    }
}
