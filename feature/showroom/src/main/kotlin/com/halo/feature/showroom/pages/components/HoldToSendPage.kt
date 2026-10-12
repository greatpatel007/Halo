package com.halo.feature.showroom.pages.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halo.core.designsystem.component.HaloHoldToSend
import com.halo.core.designsystem.component.HaloRollingAmount
import com.halo.core.designsystem.component.HoldToSendState
import com.halo.core.designsystem.foundation.AmountFormat
import com.halo.core.designsystem.foundation.HaloPaymentNetwork
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.Knob
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.SpecLine

@Composable
internal fun HoldToSendPage(viewModel: HoldToSendDemoViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = HaloTheme.spacing
    val t = HaloTheme.typography
    val colors = HaloTheme.colors
    val haptics = HaloTheme.haptics
    // Failure feedback belongs to the screen, not the commit control.
    LaunchedEffect(state.error) { if (state.error != null) haptics.perform(HaloHaptic.Reject) }

    // ---- 1. Playground: a real send through use case → fake backend (balance updates everywhere).
    SectionLabel("Playground · real send, demo backend", Modifier.enterRise(0))
    ShowroomCard(Modifier.enterRise(1)) {
        HaloText("TO", style = t.overline, color = colors.content.tertiary)
        Spacer(Modifier.height(s.xs))
        HaloText(state.recipient, style = t.data)
        Spacer(Modifier.height(s.lg))
        HaloText(
            text = if (state.network == HaloPaymentNetwork.Lightning) "LIGHTNING BALANCE" else "ON-CHAIN BALANCE",
            style = t.overline,
            color = colors.content.tertiary,
        )
        Spacer(Modifier.height(s.xs))
        state.spendable?.let { HaloRollingAmount(sats = it, style = t.amountMedium) }
        Spacer(Modifier.height(s.xxl))
        HaloHoldToSend(
            sats = state.amountSats,
            state = state.sendState,
            onCommit = { viewModel.onAction(HoldToSendAction.Commit) },
            enabled = state.canAfford,
            label = if (state.canAfford) "Hold to send" else "Insufficient funds",
            network = state.network,
        )
        state.error?.let {
            Spacer(Modifier.height(s.sm))
            HaloText(it, style = t.bodyMedium, color = colors.status.negative)
        }
        Spacer(Modifier.height(s.xl))
        Knob(
            label = "Network",
            options = HaloPaymentNetwork.entries,
            selected = state.network,
            onSelect = { viewModel.onAction(HoldToSendAction.SelectNetwork(it)) },
            optionLabel = { if (it == HaloPaymentNetwork.Lightning) "Lightning" else "On-chain" },
        )
        Spacer(Modifier.height(s.lg))
        Knob(
            label = "Amount",
            options = listOf(21_000L, 250_000L, 2_500_000L),
            selected = state.amountSats,
            onSelect = { viewModel.onAction(HoldToSendAction.SelectAmount(it)) },
            optionLabel = { AmountFormat.sats(it) },
        )
    }

    // ---- 2. State matrix (static)
    SectionLabel("States", Modifier.enterRise(2))
    ShowroomCard(Modifier.enterRise(3)) {
        Column(verticalArrangement = Arrangement.spacedBy(s.md)) {
            Caption("idle · on-chain")
            HaloHoldToSend(sats = 21_000, state = HoldToSendState.Idle, onCommit = {})
            Caption("idle · lightning")
            HaloHoldToSend(sats = 21_000, state = HoldToSendState.Idle, onCommit = {}, network = HaloPaymentNetwork.Lightning)
            Caption("disabled")
            HaloHoldToSend(sats = 9_000_000, state = HoldToSendState.Idle, onCommit = {}, enabled = false, label = "Insufficient funds")
            Caption("sending")
            HaloHoldToSend(sats = 21_000, state = HoldToSendState.Sending, onCommit = {})
            Caption("success")
            HaloHoldToSend(sats = 21_000, state = HoldToSendState.Success, onCommit = {})
        }
    }

    // ---- 3. Spec
    SectionLabel("Spec", Modifier.enterRise(4))
    ShowroomCard(Modifier.enterRise(5)) {
        SpecLine("hold", "1200ms linear · resumes")
        SpecLine("release early", "Quick spring back · cancel()")
        SpecLine("lock", "Threshold → onCommit → collapse")
        SpecLine("haptics", "Crescendo · tick 0.4→0.9 · Celebrate")
        SpecLine("success", "check draw-on · 14 sparks · 700ms")
        SpecLine("a11y", "click / \"Send\" action")
    }
}

@Composable
private fun Caption(text: String) {
    HaloText(text.uppercase(), style = HaloTheme.typography.micro, color = HaloTheme.colors.content.tertiary)
}
