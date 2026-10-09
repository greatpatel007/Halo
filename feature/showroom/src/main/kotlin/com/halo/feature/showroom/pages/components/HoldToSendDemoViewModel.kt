package com.halo.feature.showroom.pages.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.core.designsystem.component.HoldToSendState
import com.halo.core.designsystem.foundation.HaloPaymentNetwork
import com.halo.core.domain.usecase.ObserveWalletOverviewUseCase
import com.halo.core.domain.usecase.SendPaymentUseCase
import com.halo.core.model.WalletError
import com.halo.core.model.sats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Fictional Lightning address used by the showroom send demo. */
private const val DEMO_LIGHTNING = "nora@wallet.example"

/** BIP-173 example address (documentation vector, not a real wallet). */
private const val DEMO_ONCHAIN = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"

data class HoldToSendDemoUiState(
    val amountSats: Long = 21_000,
    val network: HaloPaymentNetwork = HaloPaymentNetwork.Lightning,
    val sendState: HoldToSendState = HoldToSendState.Idle,
    val lightningBalance: Long? = null,
    val onchainBalance: Long? = null,
    val error: String? = null,
) {
    val recipient: String get() = if (network == HaloPaymentNetwork.Lightning) DEMO_LIGHTNING else DEMO_ONCHAIN
    val spendable: Long? get() = if (network == HaloPaymentNetwork.Lightning) lightningBalance else onchainBalance
    val canAfford: Boolean get() = spendable == null || amountSats <= (spendable ?: 0)
}

sealed interface HoldToSendAction {
    data class SelectAmount(val sats: Long) : HoldToSendAction
    data class SelectNetwork(val network: HaloPaymentNetwork) : HoldToSendAction
    data object Commit : HoldToSendAction
}

/**
 * The Send flow's commit step in miniature — the exact UDF pattern a real feature uses:
 * use cases in, one immutable UiState out, one onAction entry point.
 */
@HiltViewModel
class HoldToSendDemoViewModel @Inject constructor(
    observeWalletOverview: ObserveWalletOverviewUseCase,
    private val sendPayment: SendPaymentUseCase,
) : ViewModel() {

    private val local = MutableStateFlow(HoldToSendDemoUiState())

    val uiState: StateFlow<HoldToSendDemoUiState> =
        combine(local, observeWalletOverview()) { state, overview ->
            state.copy(
                lightningBalance = overview.balance.lightning.value,
                onchainBalance = overview.balance.onchain.value,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

    fun onAction(action: HoldToSendAction) {
        when (action) {
            is HoldToSendAction.SelectAmount -> local.update {
                if (it.sendState == HoldToSendState.Idle) it.copy(amountSats = action.sats, error = null) else it
            }
            is HoldToSendAction.SelectNetwork -> local.update {
                if (it.sendState == HoldToSendState.Idle) it.copy(network = action.network, error = null) else it
            }
            HoldToSendAction.Commit -> commit()
        }
    }

    private fun commit() {
        val snapshot = local.value
        if (snapshot.sendState != HoldToSendState.Idle) return
        local.update { it.copy(sendState = HoldToSendState.Sending, error = null) }
        viewModelScope.launch {
            val result = sendPayment(snapshot.recipient, snapshot.amountSats.sats, memo = "Showroom demo")
            result.fold(
                onSuccess = {
                    local.update { it.copy(sendState = HoldToSendState.Success) }
                    delay(RESET_AFTER_MS)
                    local.update { it.copy(sendState = HoldToSendState.Idle) }
                },
                onFailure = { error ->
                    local.update {
                        it.copy(
                            sendState = HoldToSendState.Idle,
                            error = when (error) {
                                is WalletError.InsufficientFunds -> "Not enough Lightning balance for this payment."
                                is WalletError.InvalidDestination -> "That destination doesn't look right."
                                else -> "Payment failed. Try again."
                            },
                        )
                    }
                },
            )
        }
    }

    private companion object {
        const val RESET_AFTER_MS = 2_800L
    }
}
