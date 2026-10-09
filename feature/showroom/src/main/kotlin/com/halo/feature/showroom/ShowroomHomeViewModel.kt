package com.halo.feature.showroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halo.core.domain.usecase.ObserveWalletOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface ShowroomHomeUiState {
    data object Loading : ShowroomHomeUiState

    /** Live values from the (fake) backend, proving the full data → domain → UI pipeline. */
    data class Ready(
        val balanceSats: Long,
        val fiatPerBtc: Double,
        val transactionCount: Int,
    ) : ShowroomHomeUiState
}

@HiltViewModel
class ShowroomHomeViewModel @Inject constructor(
    observeWalletOverview: ObserveWalletOverviewUseCase,
) : ViewModel() {

    val uiState: StateFlow<ShowroomHomeUiState> = observeWalletOverview(recentCount = Int.MAX_VALUE)
        .map { overview ->
            ShowroomHomeUiState.Ready(
                balanceSats = overview.balance.total.value,
                fiatPerBtc = overview.rate.perBtc.toDouble(),
                transactionCount = overview.recent.size,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ShowroomHomeUiState.Loading,
        )
}
