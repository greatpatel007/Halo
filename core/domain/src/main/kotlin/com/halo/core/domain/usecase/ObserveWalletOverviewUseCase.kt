package com.halo.core.domain.usecase

import com.halo.core.domain.repository.RatesRepository
import com.halo.core.domain.repository.WalletRepository
import com.halo.core.model.WalletOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Balance + live rate + the latest transactions, as one stream for the home screen. */
class ObserveWalletOverviewUseCase @Inject constructor(
    private val wallet: WalletRepository,
    private val rates: RatesRepository,
) {
    operator fun invoke(currencyCode: String = "USD", recentCount: Int = 5): Flow<WalletOverview> =
        combine(
            wallet.observeBalance(),
            rates.observeRate(currencyCode),
            wallet.observeTransactions(),
        ) { balance, rate, transactions ->
            WalletOverview(balance = balance, rate = rate, recent = transactions.take(recentCount))
        }
}
