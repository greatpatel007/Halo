package com.halo.core.domain

import com.halo.core.domain.usecase.ParsePaymentDestinationUseCase
import com.halo.core.model.PaymentDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsePaymentDestinationUseCaseTest {

    private val parse = ParsePaymentDestinationUseCase()

    @Test
    fun lightningAddress_isLowercased() {
        val result = parse("Nora@Wallet.Example")
        assertEquals(PaymentDestination.LightningAddress("nora@wallet.example"), result)
    }

    @Test
    fun bip21Uri_stripsSchemeAndParams() {
        val result = parse("bitcoin:bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq?amount=0.001")
        assertEquals(PaymentDestination.OnchainAddress("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"), result)
    }

    @Test
    fun bolt11Invoice_isDetected() {
        assertTrue(parse("lightning:lnbc2500u1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzq") is PaymentDestination.LightningInvoice)
    }

    @Test
    fun garbage_isRejected() {
        assertNull(parse("hello world"))
        assertNull(parse("   "))
    }
}
