package com.halo.feature.showroom.pages.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halo.core.designsystem.component.HaloRollingAmount
import com.halo.core.designsystem.foundation.AmountFormat
import com.halo.core.designsystem.foundation.AmountUnit
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.core.designsystem.theme.LocalHaloContentColor
import com.halo.feature.showroom.ui.ActionChip
import com.halo.feature.showroom.ui.Knob
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.SpecLine

@Composable
internal fun RollingAmountPage(viewModel: LiveBalanceViewModel = hiltViewModel()) {
    val live by viewModel.uiState.collectAsStateWithLifecycle()
    val s = HaloTheme.spacing
    val t = HaloTheme.typography
    val colors = HaloTheme.colors
    var unit by rememberSaveable { mutableStateOf(AmountUnit.Sats) }
    var hidden by rememberSaveable { mutableStateOf(false) }
    var manual by rememberSaveable { mutableLongStateOf(48_120L) }

    // ---- 1. Playground: live data through data → domain → ViewModel → component.
    SectionLabel("Live · demo backend", Modifier.enterRise(0))
    ShowroomCard(Modifier.enterRise(1)) {
        HaloText("TOTAL BALANCE", style = t.overline, color = colors.content.tertiary)
        Spacer(Modifier.height(s.md))
        val state = live
        if (state == null) {
            HaloText("Connecting…", style = t.data, color = colors.content.tertiary)
        } else {
            HaloRollingAmount(sats = state.sats, unit = unit, fiatPerBtc = state.fiatPerBtc, hidden = hidden)
            Spacer(Modifier.height(s.sm))
            CompositionLocalProvider(LocalHaloContentColor provides colors.content.secondary) {
                HaloRollingAmount(
                    sats = state.sats,
                    unit = if (unit == AmountUnit.Fiat) AmountUnit.Sats else AmountUnit.Fiat,
                    fiatPerBtc = state.fiatPerBtc,
                    style = t.amountSmall,
                    hidden = hidden,
                )
            }
            Spacer(Modifier.height(s.lg))
            HaloText(
                text = "latest  ${state.latest ?: "—"}",
                style = t.data,
                color = colors.content.tertiary,
                maxLines = 1,
            )
            HaloText(
                text = "zap ~12s · rate 4s",
                style = t.data,
                color = colors.content.tertiary,
            )
        }
        Spacer(Modifier.height(s.xl))
        Knob("Unit", AmountUnit.entries, unit, { unit = it }, optionLabel = { it.name })
        Spacer(Modifier.height(s.lg))
        Knob("Privacy", listOf(false, true), hidden, { hidden = it }, optionLabel = { if (it) "hidden" else "shown" })
    }

    // ---- 2. Manual: user-driven changes (these tick).
    SectionLabel("Manual · user-driven, ticks", Modifier.enterRise(2))
    ShowroomCard(Modifier.enterRise(3)) {
        HaloRollingAmount(sats = manual, style = t.amountLarge, tickOnChange = true)
        Spacer(Modifier.height(s.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(s.xs)) {
            ActionChip("+21", { manual += 21 })
            ActionChip("+9 999", { manual += 9_999 })
            ActionChip("−12 345", { manual = (manual - 12_345).coerceAtLeast(0) })
        }
        Spacer(Modifier.height(s.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(s.xs)) {
            ActionChip("×10", { manual = (manual * 10).coerceAtMost(AmountFormat.SATS_PER_BTC * 21_000_000) })
            ActionChip("÷10", { manual /= 10 })
            ActionChip("0", { manual = 0 })
        }
    }

    // ---- 3. State matrix: units × sizes.
    SectionLabel("Units · sizes", Modifier.enterRise(4))
    ShowroomCard(Modifier.enterRise(5)) {
        Column(verticalArrangement = Arrangement.spacedBy(s.lg)) {
            HaloRollingAmount(sats = 4_812_350, style = t.amountLarge)
            HaloRollingAmount(sats = 4_812_350, unit = AmountUnit.Btc, style = t.amountLarge)
            HaloRollingAmount(sats = 4_812_350, unit = AmountUnit.Fiat, fiatPerBtc = 104_250.0, style = t.amountMedium)
            HaloRollingAmount(sats = 100_000_000, unit = AmountUnit.Btc, style = t.amountMedium)
            HaloRollingAmount(sats = 4_812_350, style = t.amountSmall)
            HaloRollingAmount(sats = 4_812_350, style = t.amountMedium, hidden = true)
        }
    }

    // ---- 4. Spec
    SectionLabel("Spec", Modifier.enterRise(6))
    ShowroomCard(Modifier.enterRise(7)) {
        SpecLine("roll", "Digit spring · ζ 0.75 · k 700")
        SpecLine("stagger", "18ms per digit, right → left")
        SpecLine("direction", "up on increase · down on decrease")
        SpecLine("unit switch", "slide ⅓ + fade · Standard")
        SpecLine("privacy", "digit → • · 20ms stagger")
        SpecLine("haptic", "tick 0.5 · user-driven only")
    }
}
