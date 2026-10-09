package com.halo.core.model

import java.math.BigDecimal
import java.time.Instant

/** Price of 1 BTC in a fiat currency. */
data class FiatRate(
    /** ISO 4217, e.g. "USD". */
    val currencyCode: String,
    val perBtc: BigDecimal,
    val updatedAt: Instant,
)

/** Where funds live / move. */
enum class Network { Onchain, Lightning }

data class WalletBalance(
    val onchain: Sats,
    val lightning: Sats,
    /** Incoming on-chain funds not yet confirmed. */
    val pending: Sats = Sats.ZERO,
) {
    val total: Sats get() = onchain + lightning
}

enum class TransactionDirection { Incoming, Outgoing }

enum class TransactionStatus { Pending, Confirmed, Failed }

data class Transaction(
    val id: String,
    val direction: TransactionDirection,
    val amount: Sats,
    val fee: Sats,
    val network: Network,
    val status: TransactionStatus,
    /** Display name, Lightning address or truncated on-chain address. */
    val counterparty: String,
    val memo: String?,
    val timestamp: Instant,
)

/** Everything the home screen needs, combined in the domain layer. */
data class WalletOverview(
    val balance: WalletBalance,
    val rate: FiatRate,
    val recent: List<Transaction>,
)

/** A destination the user can pay. Parsed from a scan, paste or contact. */
sealed interface PaymentDestination {
    val raw: String

    data class OnchainAddress(override val raw: String) : PaymentDestination
    data class LightningInvoice(override val raw: String, val amount: Sats?, val memo: String?) : PaymentDestination
    data class LightningAddress(override val raw: String) : PaymentDestination
}

data class PaymentRequest(
    val destination: PaymentDestination,
    val amount: Sats,
    val memo: String? = null,
)
