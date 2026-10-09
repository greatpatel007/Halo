package com.halo.core.domain.usecase

import com.halo.core.domain.repository.WalletRepository
import com.halo.core.model.PaymentRequest
import com.halo.core.model.Sats
import com.halo.core.model.Transaction
import com.halo.core.model.WalletError
import javax.inject.Inject

/** Validates a destination + amount and sends. Every failure comes back typed in [Result]. */
class SendPaymentUseCase @Inject constructor(
    private val wallet: WalletRepository,
    private val parseDestination: ParsePaymentDestinationUseCase,
) {
    suspend operator fun invoke(destination: String, amount: Sats, memo: String? = null): Result<Transaction> {
        val parsed = parseDestination(destination)
            ?: return Result.failure(WalletError.InvalidDestination(destination))
        if (!amount.isPositive) return Result.failure(IllegalArgumentException("Amount must be positive"))
        if (amount.value > Sats.MAX_SUPPLY) return Result.failure(IllegalArgumentException("Amount exceeds supply"))
        return wallet.send(PaymentRequest(parsed, amount, memo))
    }
}
