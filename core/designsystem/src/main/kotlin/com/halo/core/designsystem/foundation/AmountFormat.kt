package com.halo.core.designsystem.foundation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Display unit for a Bitcoin amount. */
enum class AmountUnit { Sats, Btc, Fiat }

/**
 * Display formatting for amounts (pure presentation — no business rules). Follows the type rules:
 * sats grouped with narrowed spaces, insignificant trailing BTC zeros dimmed, fiat via the locale.
 *
 * Paper Mono has no thin space (U+2009), so groups use a normal space narrowed with negative tracking.
 */
object AmountFormat {
    const val SATS_PER_BTC = 100_000_000L

    /** Tracking applied to group separators — a full mono space minus ~half an em. */
    private val GroupGapTracking = (-0.3f).em

    /** "4812350" → "4 812 350" (plain spaces; use [satsAnnotated] to narrow them). */
    fun sats(sats: Long): String {
        val digits = kotlin.math.abs(sats).toString()
        val grouped = digits.reversed().chunked(3).joinToString(" ").reversed()
        return if (sats < 0) "-$grouped" else grouped
    }

    /** "0.04812350" with all 8 decimals. */
    fun btc(sats: Long): String =
        BigDecimal.valueOf(sats).divide(BigDecimal.valueOf(SATS_PER_BTC)).setScale(8, RoundingMode.UNNECESSARY)
            .toPlainString()

    fun fiat(sats: Long, fiatPerBtc: Double, currency: Currency = Currency.getInstance("USD"), locale: Locale = Locale.US): String {
        val value = BigDecimal.valueOf(sats)
            .multiply(BigDecimal.valueOf(fiatPerBtc))
            .divide(BigDecimal.valueOf(SATS_PER_BTC), 2, RoundingMode.HALF_EVEN)
        return NumberFormat.getCurrencyInstance(locale).apply { this.currency = currency }.format(value)
    }

    /** Sats with narrowed group gaps. */
    fun satsAnnotated(sats: Long): AnnotatedString = buildAnnotatedString {
        sats(sats).forEach { ch ->
            if (ch == ' ') withStyle(SpanStyle(letterSpacing = GroupGapTracking)) { append(ch) } else append(ch)
        }
    }

    /** "21 000 sats" with every gap (groups and before the unit) narrowed. */
    fun satsWithUnitAnnotated(sats: Long): AnnotatedString = buildAnnotatedString {
        append(satsAnnotated(sats))
        withStyle(SpanStyle(letterSpacing = GroupGapTracking)) { append(' ') }
        append("sats")
    }

    /** BTC without insignificant trailing zeros: 0.04812350 → "0.0481235", 1.00000000 → "1.0". */
    fun btcSignificant(sats: Long): String =
        btc(sats).trimEnd('0').let { if (it.endsWith('.')) "${it}0" else it }

    /** BTC with insignificant trailing zeros drawn in [dim]: 0.0481235 + "0". */
    fun btcAnnotated(sats: Long, dim: Color): AnnotatedString {
        val full = btc(sats)
        val significant = btcSignificant(sats)
        return buildAnnotatedString {
            append(significant)
            withStyle(SpanStyle(color = dim)) { append(full.removePrefix(significant)) }
        }
    }
}
