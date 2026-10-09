package com.halo.core.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * An amount of bitcoin in satoshis — the only unit the domain computes in. BTC and fiat are display
 * concerns. Millisatoshi precision (Lightning) can be added as a separate type when a backend needs it.
 */
@JvmInline
value class Sats(val value: Long) : Comparable<Sats> {

    operator fun plus(other: Sats) = Sats(Math.addExact(value, other.value))
    operator fun minus(other: Sats) = Sats(Math.subtractExact(value, other.value))
    operator fun unaryMinus() = Sats(-value)
    override fun compareTo(other: Sats): Int = value.compareTo(other.value)

    val isPositive: Boolean get() = value > 0
    val btc: BigDecimal get() = BigDecimal.valueOf(value).movePointLeft(8)

    /** Fiat value at [rate], rounded half-even to the currency's 2 decimals. */
    fun toFiat(rate: FiatRate): BigDecimal =
        BigDecimal.valueOf(value).multiply(rate.perBtc).divide(BigDecimal.valueOf(SATS_PER_BTC), 2, RoundingMode.HALF_EVEN)

    override fun toString(): String = "$value sat"

    companion object {
        const val SATS_PER_BTC = 100_000_000L
        /** Hard cap: 21M BTC. */
        const val MAX_SUPPLY = 21_000_000L * SATS_PER_BTC
        val ZERO = Sats(0)

        fun fromBtc(btc: BigDecimal): Sats = Sats(btc.movePointRight(8).setScale(0, RoundingMode.DOWN).longValueExact())
    }
}

val Long.sats: Sats get() = Sats(this)
val Int.sats: Sats get() = Sats(toLong())
