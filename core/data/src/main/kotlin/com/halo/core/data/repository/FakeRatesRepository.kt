package com.halo.core.data.repository

import com.halo.core.data.demo.DemoData
import com.halo.core.data.di.ApplicationScope
import com.halo.core.domain.repository.RatesRepository
import com.halo.core.model.FiatRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** A believable price ticker: small random walk every few seconds. USD only in the demo. */
@Singleton
internal class FakeRatesRepository @Inject constructor(
    @ApplicationScope scope: CoroutineScope,
    private val clock: Clock,
) : RatesRepository {

    private val usd = MutableStateFlow(
        FiatRate("USD", BigDecimal.valueOf(DemoData.USD_PER_BTC).setScale(2), clock.instant()),
    )

    init {
        scope.launch {
            while (isActive) {
                delay(TICK_MS)
                val current = usd.value.perBtc
                val drift = current.multiply(BigDecimal.valueOf(Random.nextDouble(-0.0015, 0.0015)))
                usd.value = FiatRate("USD", current.add(drift).setScale(2, RoundingMode.HALF_EVEN), clock.instant())
            }
        }
    }

    override fun observeRate(currencyCode: String): Flow<FiatRate> {
        require(currencyCode == "USD") { "Demo backend only quotes USD" }
        return usd.asStateFlow()
    }

    private companion object {
        const val TICK_MS = 4_000L
    }
}
