package com.halo.feature.showroom.pages.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.core.domain.usecase.ObserveWalletOverviewUseCase
import com.halo.core.model.TransactionDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject

/** UI state for components demoed against the live (fake) backend. */
data class LiveBalanceUiState(
    val sats: Long,
    val fiatPerBtc: Double,
    /** e.g. "+21 000 · mira@wallet.example" — the newest transaction. */
    val latest: String?,
)

@HiltViewModel
class LiveBalanceViewModel @Inject constructor(
    observeWalletOverview: ObserveWalletOverviewUseCase,
) : ViewModel() {

    val uiState: StateFlow<LiveBalanceUiState?> = observeWalletOverview(recentCount = 1)
        .map { overview ->
            val newest = overview.recent.firstOrNull()
            LiveBalanceUiState(
                sats = overview.balance.total.value,
                fiatPerBtc = overview.rate.perBtc.toDouble(),
                latest = newest?.let {
                    val sign = if (it.direction == TransactionDirection.Incoming) "+" else "−"
                    "$sign${"%,d".format(Locale.US, it.amount.value).replace(',', ' ')} · ${it.counterparty}"
                },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
