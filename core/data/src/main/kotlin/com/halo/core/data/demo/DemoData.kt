package com.halo.core.data.demo

import com.halo.core.model.Network
import com.halo.core.model.Transaction
import com.halo.core.model.TransactionDirection
import com.halo.core.model.TransactionStatus
import com.halo.core.model.WalletBalance
import com.halo.core.model.sats
import java.time.Duration
import java.time.Instant

/** Seed data for the fake backend. Every name and address here is fictional. */
internal object DemoData {

    val balance = WalletBalance(
        onchain = 3_612_400.sats,
        lightning = 1_199_950.sats,
    )

    const val USD_PER_BTC = 104_250.00

    val incomingCounterparties = listOf(
        "nora@wallet.example" to "Coffee run",
        "theo@wallet.example" to "Split dinner",
        "mira@wallet.example" to "Zap",
        "Sunday Market" to null,
        "kai@wallet.example" to "Thanks!",
    )

    fun transactions(now: Instant): List<Transaction> = listOf(
        tx("t9", TransactionDirection.Incoming, 21_000, 0, Network.Lightning, "mira@wallet.example", "Zap", now, 4),
        tx("t8", TransactionDirection.Outgoing, 6_400, 3, Network.Lightning, "Blue Bottle Café", "Flat white", now, 52),
        tx("t7", TransactionDirection.Incoming, 250_000, 0, Network.Onchain, "bc1q…7f3k", "Salary top-up", now, 60 * 5),
        tx("t6", TransactionDirection.Outgoing, 48_200, 12, Network.Lightning, "theo@wallet.example", "Concert tickets", now, 60 * 26),
        tx("t5", TransactionDirection.Outgoing, 120_000, 1_840, Network.Onchain, "bc1p…q9xe", "Cold storage", now, 60 * 49),
        tx("t4", TransactionDirection.Incoming, 9_999, 0, Network.Lightning, "nora@wallet.example", "Coffee run", now, 60 * 75),
        tx("t3", TransactionDirection.Outgoing, 15_500, 6, Network.Lightning, "Neon Arcade", null, now, 60 * 98),
        tx("t2", TransactionDirection.Incoming, 1_000_000, 0, Network.Onchain, "bc1q…m2dz", "From exchange", now, 60 * 24 * 6),
    )

    private fun tx(
        id: String,
        direction: TransactionDirection,
        amount: Long,
        fee: Long,
        network: Network,
        counterparty: String,
        memo: String?,
        now: Instant,
        minutesAgo: Long,
    ) = Transaction(
        id = id,
        direction = direction,
        amount = amount.sats,
        fee = fee.sats,
        network = network,
        status = TransactionStatus.Confirmed,
        counterparty = counterparty,
        memo = memo,
        timestamp = now.minus(Duration.ofMinutes(minutesAgo)),
    )
}
