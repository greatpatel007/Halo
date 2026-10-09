package com.halo.core.domain

import com.halo.core.domain.repository.WalletRepository
import com.halo.core.domain.usecase.ParsePaymentDestinationUseCase
import com.halo.core.domain.usecase.SendPaymentUseCase
import com.halo.core.model.Network
import com.halo.core.model.PaymentDestination
import com.halo.core.model.PaymentRequest
import com.halo.core.model.Sats
import com.halo.core.model.Transaction
import com.halo.core.model.TransactionDirection
import com.halo.core.model.TransactionStatus
import com.halo.core.model.WalletBalance
import com.halo.core.model.WalletError
import com.halo.core.model.sats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SendPaymentUseCaseTest {

    private class RecordingWallet : WalletRepository {
        var lastRequest: PaymentRequest? = null
        override fun observeBalance(): Flow<WalletBalance> = emptyFlow()
        override fun observeTransactions(): Flow<List<Transaction>> = emptyFlow()
        override suspend fun send(request: PaymentRequest): Result<Transaction> {
            lastRequest = request
            return Result.success(
                Transaction("t", TransactionDirection.Outgoing, request.amount, Sats.ZERO, Network.Lightning,
                    TransactionStatus.Confirmed, request.destination.raw, request.memo, Instant.EPOCH),
            )
        }
    }

    private val wallet = RecordingWallet()
    private val send = SendPaymentUseCase(wallet, ParsePaymentDestinationUseCase())

    @Test
    fun validRequest_isParsedAndForwarded() = runTest {
        val result = send("nora@wallet.example", 21_000.sats, "Coffee")
        assertTrue(result.isSuccess)
        assertEquals(PaymentDestination.LightningAddress("nora@wallet.example"), wallet.lastRequest?.destination)
        assertEquals(21_000.sats, wallet.lastRequest?.amount)
    }

    @Test
    fun invalidDestination_failsTypedWithoutCallingWallet() = runTest {
        val result = send("not a destination", 21_000.sats)
        assertTrue(result.exceptionOrNull() is WalletError.InvalidDestination)
        assertNull(wallet.lastRequest)
    }

    @Test
    fun zeroAmount_isRejected() = runTest {
        assertTrue(send("nora@wallet.example", Sats.ZERO).isFailure)
        assertNull(wallet.lastRequest)
    }
}
