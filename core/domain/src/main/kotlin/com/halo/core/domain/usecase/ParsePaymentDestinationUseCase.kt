package com.halo.core.domain.usecase

import com.halo.core.model.PaymentDestination
import javax.inject.Inject

/**
 * Classifies scanned / pasted text into a [PaymentDestination]. Shape checks only — full validation
 * (checksums, invoice decoding, LNURL resolution) belongs to the backend implementation.
 */
class ParsePaymentDestinationUseCase @Inject constructor() {

    operator fun invoke(input: String): PaymentDestination? {
        val text = input.trim().removePrefix("bitcoin:").removePrefix("lightning:").removePrefix("BITCOIN:")
            .removePrefix("LIGHTNING:")
        return when {
            text.isEmpty() -> null
            LIGHTNING_ADDRESS.matches(text) -> PaymentDestination.LightningAddress(text.lowercase())
            text.lowercase().let { it.startsWith("lnbc") || it.startsWith("lntb") || it.startsWith("lnbcrt") } ->
                PaymentDestination.LightningInvoice(raw = text, amount = null, memo = null)
            BECH32_ADDRESS.matches(text) || LEGACY_ADDRESS.matches(text) ->
                PaymentDestination.OnchainAddress(text.substringBefore('?'))
            else -> null
        }
    }

    private companion object {
        val LIGHTNING_ADDRESS = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        val BECH32_ADDRESS = Regex("^(bc1|tb1|bcrt1)[a-zA-HJ-NP-Z0-9]{8,87}(\\?.*)?$", RegexOption.IGNORE_CASE)
        val LEGACY_ADDRESS = Regex("^[13][a-km-zA-HJ-NP-Z1-9]{25,34}(\\?.*)?$")
    }
}
