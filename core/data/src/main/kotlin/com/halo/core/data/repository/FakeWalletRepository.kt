package com.halo.core.data.repository

import com.halo.core.data.demo.DemoData
import com.halo.core.data.di.ApplicationScope
import com.halo.core.domain.repository.WalletRepository
import com.halo.core.model.Network
import com.halo.core.model.PaymentDestination
import com.halo.core.model.PaymentRequest
import com.halo.core.model.Sats
import com.halo.core.model.Transaction
import com.halo.core.model.TransactionDirection
import com.halo.core.model.TransactionStatus
import com.halo.core.model.WalletBalance
import com.halo.core.model.WalletError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * In-memory wallet for the visual showcase. Behaves like a live backend: latency on sends, typed
 * failures, and a trickle of incoming Lightning payments so balance animations have something to do.
 *
 * Replace with a real implementation by binding another [WalletRepository] in `DataModule`.
 */
@Singleton
internal class FakeWalletRepository @Inject constructor(
    @ApplicationScope scope: CoroutineScope,
    private val clock: Clock,
) : WalletRepository {

    private val balance = MutableStateFlow(DemoData.balance)
    private val transactions = MutableStateFlow(DemoData.transactions(clock.instant()))

    init {
        scope.launch {
            while (isActive) {
                delay(INCOMING_INTERVAL_MS)
                receiveDemoPayment()
            }
        }
    }

    override fun observeBalance(): Flow<WalletBalance> = balance.asStateFlow()

    override fun observeTransactions(): Flow<List<Transaction>> = transactions.asStateFlow()

    override suspend fun send(request: PaymentRequest): Result<Transaction> {
        delay(SEND_LATENCY_MS)
        val network = when (request.destination) {
            is PaymentDestination.OnchainAddress -> Network.Onchain
            else -> Network.Lightning
        }
        val fee = if (network == Network.Onchain) Sats(1_840) else Sats(maxOf(1, request.amount.value / 1000))
        val available = if (network == Network.Onchain) balance.value.onchain else balance.value.lightning
        if (request.amount + fee > available) {
            return Result.failure(WalletError.InsufficientFunds(available, request.amount + fee))
        }
        val tx = Transaction(
            id = UUID.randomUUID().toString(),
            direction = TransactionDirection.Outgoing,
            amount = request.amount,
            fee = fee,
            network = network,
            status = if (network == Network.Onchain) TransactionStatus.Pending else TransactionStatus.Confirmed,
            counterparty = request.destination.raw,
            memo = request.memo,
            timestamp = clock.instant(),
        )
        balance.update {
            if (network == Network.Onchain) it.copy(onchain = it.onchain - request.amount - fee)
            else it.copy(lightning = it.lightning - request.amount - fee)
        }
        transactions.update { listOf(tx) + it }
        return Result.success(tx)
    }

    private fun receiveDemoPayment() {
        val (who, memo) = DemoData.incomingCounterparties.random()
        val amount = Sats(Random.nextLong(210, 25_000))
        val tx = Transaction(
            id = UUID.randomUUID().toString(),
            direction = TransactionDirection.Incoming,
            amount = amount,
            fee = Sats.ZERO,
            network = Network.Lightning,
            status = TransactionStatus.Confirmed,
            counterparty = who,
            memo = memo,
            timestamp = clock.instant(),
        )
        balance.update { it.copy(lightning = it.lightning + amount) }
        transactions.update { listOf(tx) + it }
    }

    private companion object {
        const val SEND_LATENCY_MS = 1_200L
        const val INCOMING_INTERVAL_MS = 12_000L
    }
}
