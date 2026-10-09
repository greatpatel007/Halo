package com.halo.core.domain.repository

import com.halo.core.model.FiatRate
import com.halo.core.model.PaymentRequest
import com.halo.core.model.Transaction
import com.halo.core.model.WalletBalance
import kotlinx.coroutines.flow.Flow

/**
 * Wallet state and actions. The app ships a fake implementation (:core:data); a real one (node SDK,
 * custodial API, LDK...) only has to implement this interface and be bound in the DI module.
 */
interface WalletRepository {
    fun observeBalance(): Flow<WalletBalance>

    /** Newest first. */
    fun observeTransactions(): Flow<List<Transaction>>

    /** Executes a payment. Failures are returned, never thrown. */
    suspend fun send(request: PaymentRequest): Result<Transaction>
}

/** Fiat exchange rates. */
interface RatesRepository {
    fun observeRate(currencyCode: String): Flow<FiatRate>
}
