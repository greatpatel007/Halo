package com.halo.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.halo.core.designsystem.foundation.AmountFormat
import com.halo.core.designsystem.foundation.AmountUnit
import com.halo.core.designsystem.foundation.HaloPreview
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.icons.HaloIcon
import com.halo.core.designsystem.icons.HaloIcons
import com.halo.core.designsystem.theme.HaloFade
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.core.designsystem.theme.LocalHaloContentColor
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * A Bitcoin amount whose digits roll individually, like a precision odometer: up when the value grows,
 * down when it shrinks, staggered right-to-left on the Digit spring. Units glide between sats, BTC and
 * fiat; [hidden] masks every digit to a dot without changing width.
 *
 * Spec: docs/components/HaloRollingAmount.md · Archetype: Value display · Showroom: halo://showroom/rolling-amount
 *
 * @param sats the amount — the only number the component trusts. Formatting happens here, maths doesn't.
 * @param fiatPerBtc display rate for [AmountUnit.Fiat].
 * @param tickOnChange play a tick haptic on every change. Only for user-driven changes (keypad), never
 * for live balance updates.
 */
@Composable
fun HaloRollingAmount(
    sats: Long,
    modifier: Modifier = Modifier,
    unit: AmountUnit = AmountUnit.Sats,
    fiatPerBtc: Double = 0.0,
    style: TextStyle = HaloTheme.typography.amountHero,
    hidden: Boolean = false,
    tickOnChange: Boolean = false,
) {
    val motion = HaloTheme.motion
    val haptics = HaloTheme.haptics
    val display = remember(sats, unit, fiatPerBtc) { AmountDisplay.of(sats, unit, fiatPerBtc) }
    val tracker = remember { DirectionTracker(display.numeric) }
    val rising = remember(display.numeric) { tracker.update(display.numeric) }
    val firstValue = remember { SeenFlag() }
    LaunchedEffect(display.numeric) {
        if (firstValue.seen && tickOnChange) haptics.tick(TICK_INTENSITY)
        firstValue.seen = true
    }
    val spoken = if (hidden) "Amount hidden" else display.spoken
    AnimatedContent(
        targetState = unit,
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
        contentAlignment = Alignment.BottomStart,
        transitionSpec = {
            val slide = motion.spring(HaloSpring.Standard, IntOffset.VisibilityThreshold)
            (slideInVertically(slide) { it / 3 } + fadeIn(motion.fade(HaloFade.Fast)))
                .togetherWith(slideOutVertically(slide) { -it / 3 } + fadeOut(motion.fade(HaloFade.Fast)))
                .using(SizeTransform(clip = false) { _, _ -> motion.spring(HaloSpring.Standard, IntSize.VisibilityThreshold) })
        },
        label = "amountUnit",
    ) { shownUnit ->
        val shown = remember(sats, shownUnit, fiatPerBtc) { AmountDisplay.of(sats, shownUnit, fiatPerBtc) }
        AmountRow(shown, rising, style, hidden)
    }
}

@Composable
private fun AmountRow(display: AmountDisplay, rising: Boolean, style: TextStyle, hidden: Boolean) {
    val colors = HaloTheme.colors
    val density = LocalDensity.current
    Row {
        if (display.unit == AmountUnit.Btc) {
            val glyph = with(density) { (style.fontSize * BTC_GLYPH_SCALE).toDp() }
            HaloIcon(
                icon = HaloIcons.Bitcoin,
                contentDescription = null,
                tint = colors.bitcoin.text,
                size = glyph,
                // Put the glyph's ink bottom (y = 21 of the 24 grid) on the digits' baseline.
                modifier = Modifier.alignBy { (it.measuredHeight * BTC_INK_BOTTOM).roundToInt() },
            )
            Spacer(Modifier.width(HaloTheme.spacing.sm))
        }
        Odometer(display.text, display.dimFrom, rising, style, hidden, Modifier.alignByBaseline())
        if (display.unit == AmountUnit.Sats) {
            Spacer(Modifier.width(HaloTheme.spacing.sm))
            HaloText(
                text = "sats",
                style = if (style.fontSize.value >= LARGE_STYLE_SP) HaloTheme.typography.amountMedium else HaloTheme.typography.data,
                color = colors.content.secondary,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

@Composable
private fun Odometer(
    text: String,
    dimFrom: Int,
    rising: Boolean,
    style: TextStyle,
    hidden: Boolean,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val glyphs = remember(measurer, style) { GlyphCache(measurer, style) }
    val zero = glyphs['0']
    val density = LocalDensity.current
    val cellWidth = with(density) { zero.size.width.toDp() }
    val narrowWidth = cellWidth * NARROW_CELL
    val cellHeight = with(density) { zero.size.height.toDp() }
    val ink = LocalHaloContentColor.current.takeOrElse { HaloTheme.colors.content.primary }
    val dim = HaloTheme.colors.content.tertiary
    val risingState = rememberUpdatedState(rising)
    val composed = remember { SeenFlag() }
    SideEffect { composed.seen = true }
    Row(
        modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(
                placeable.width,
                placeable.height,
                mapOf(FirstBaseline to zero.firstBaseline.roundToInt(), LastBaseline to zero.lastBaseline.roundToInt()),
            ) { placeable.place(0, 0) }
        },
    ) {
        text.forEachIndexed { index, ch ->
            val fromRight = text.length - 1 - index
            val tint = if (index >= dimFrom) dim else ink
            key(fromRight, ch.isDigit()) {
                if (ch.isDigit()) {
                    DigitCell(ch - '0', glyphs, cellWidth, cellHeight, tint, risingState, hidden, fromRight, composed.seen)
                } else {
                    GlyphCell(ch, glyphs, if (ch in NARROW_CHARS) narrowWidth else cellWidth, cellHeight, tint, hidden, composed.seen)
                }
            }
        }
    }
}

@Composable
private fun DigitCell(
    digit: Int,
    glyphs: GlyphCache,
    width: Dp,
    height: Dp,
    tint: Color,
    rising: State<Boolean>,
    hidden: Boolean,
    fromRight: Int,
    animateIn: Boolean,
) {
    val motion = HaloTheme.motion
    val position = remember { Animatable(digit.toFloat()) }
    val mask = remember { Animatable(if (hidden) 1f else 0f) }
    val appear = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) { if (appear.value < 1f) appear.animateTo(1f, motion.fade(HaloFade.Base)) }
    LaunchedEffect(digit) {
        val target = rollTarget(position.value, digit, rising.value)
        if (target == position.value) return@LaunchedEffect
        delay(motion.millis(fromRight * DIGIT_STAGGER_MS))
        position.animateTo(target, motion.spring(HaloSpring.Digit))
        position.snapTo(wrapDigit(target))
    }
    LaunchedEffect(hidden) {
        delay(motion.millis(fromRight * MASK_STAGGER_MS))
        mask.animateTo(if (hidden) 1f else 0f, motion.spring(HaloSpring.Standard))
    }
    Canvas(
        Modifier
            .size(width, height)
            .graphicsLayer { alpha = appear.value },
    ) {
        val m = mask.value.coerceIn(0f, 1f)
        val h = size.height
        clipRect {
            if (m < 1f) {
                val p = position.value
                val base = floor(p)
                val frac = p - base
                for (k in 0..1) {
                    val d = (((base.toInt() + k) % 10) + 10) % 10
                    val distance = abs(k - frac)
                    val a = (1f - distance).coerceIn(0f, 1f) * (1f - m)
                    if (a > 0f) {
                        drawText(glyphs['0' + d], color = tint, topLeft = Offset(0f, (k - frac) * h - m * h * MASK_RISE), alpha = a)
                    }
                }
            }
            if (m > 0f) {
                val dot = glyphs[MASK_CHAR]
                drawText(dot, color = tint, topLeft = Offset((size.width - dot.size.width) / 2f, (1f - m) * h * MASK_RISE), alpha = m)
            }
        }
    }
}

@Composable
private fun GlyphCell(
    ch: Char,
    glyphs: GlyphCache,
    width: Dp,
    height: Dp,
    tint: Color,
    hidden: Boolean,
    animateIn: Boolean,
) {
    val motion = HaloTheme.motion
    val appear = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) { if (appear.value < 1f) appear.animateTo(1f, motion.fade(HaloFade.Base)) }
    val presence by animateFloatAsState(if (hidden) HIDDEN_SEPARATOR_ALPHA else 1f, motion.fade(HaloFade.Base), label = "separator")
    Canvas(
        Modifier
            .size(width, height)
            .graphicsLayer { alpha = appear.value * presence },
    ) {
        val glyph = glyphs[ch]
        drawText(glyph, color = tint, topLeft = Offset((size.width - glyph.size.width) / 2f, 0f))
    }
}

// ------------------------------------------------------------------------------------------- model

private class AmountDisplay(
    val unit: AmountUnit,
    val text: String,
    /** Index from which glyphs are drawn dimmed (insignificant BTC zeros). */
    val dimFrom: Int,
    /** Numeric value in the shown unit — decides roll direction. */
    val numeric: Double,
    val spoken: String,
) {
    companion object {
        fun of(sats: Long, unit: AmountUnit, fiatPerBtc: Double): AmountDisplay = when (unit) {
            AmountUnit.Sats -> {
                val text = AmountFormat.sats(sats)
                AmountDisplay(unit, text, text.length, sats.toDouble(), "${NumberFormat.getIntegerInstance(Locale.US).format(sats)} sats")
            }
            AmountUnit.Btc -> {
                val significant = AmountFormat.btcSignificant(sats)
                AmountDisplay(unit, AmountFormat.btc(sats), significant.length, sats.toDouble(), "$significant bitcoin")
            }
            AmountUnit.Fiat -> {
                val text = AmountFormat.fiat(sats, fiatPerBtc)
                AmountDisplay(unit, text, text.length, sats * fiatPerBtc, text)
            }
        }
    }
}

/** Remembers the previous value so each change knows whether it rolls up or down. */
private class DirectionTracker(private var last: Double) {
    fun update(value: Double): Boolean {
        val rising = value >= last
        last = value
        return rising
    }
}

private class SeenFlag(var seen: Boolean = false)

/** Measures each glyph once per style; digits and the mask dot are measured up front. */
private class GlyphCache(private val measurer: TextMeasurer, private val style: TextStyle) {
    private val cache = HashMap<Char, TextLayoutResult>()

    init {
        ('0'..'9').forEach { get(it) }
        get(MASK_CHAR)
    }

    operator fun get(c: Char): TextLayoutResult =
        cache.getOrPut(c) { measurer.measure(c.toString(), style, softWrap = false, maxLines = 1) }
}

/** Continuous target ≡ [digit] (mod 10) in the roll direction; tiny reversals take the short way. */
private fun rollTarget(current: Float, digit: Int, rising: Boolean): Float {
    val currentMod = ((current % 10f) + 10f) % 10f
    val forward = (digit - currentMod + 10f) % 10f
    val backward = (currentMod - digit + 10f) % 10f
    return when {
        forward < SNAP_EPSILON || backward < SNAP_EPSILON -> current
        forward < SHORT_WAY -> current + forward
        backward < SHORT_WAY -> current - backward
        rising -> current + forward
        else -> current - backward
    }
}

private fun wrapDigit(value: Float): Float = ((value.roundToInt() % 10 + 10) % 10).toFloat()

private const val DIGIT_STAGGER_MS = 18
private const val MASK_STAGGER_MS = 20
private const val MASK_RISE = 0.3f
private const val MASK_CHAR = '•'
private const val NARROW_CELL = 0.5f
private val NARROW_CHARS = setOf(' ', ',', '.')
private const val HIDDEN_SEPARATOR_ALPHA = 0.5f
private const val TICK_INTENSITY = 0.5f
private const val SNAP_EPSILON = 0.001f
private const val SHORT_WAY = 0.5f
private const val BTC_GLYPH_SCALE = 0.72f
private const val BTC_INK_BOTTOM = 21f / 24f
private const val LARGE_STYLE_SP = 32f

// ------------------------------------------------------------------------------------------- previews

@Preview
@Composable
private fun HaloRollingAmountUnitsPreview() = HaloPreview {
    HaloRollingAmount(4_812_350)
    HaloRollingAmount(4_812_350, unit = AmountUnit.Btc, style = HaloTheme.typography.amountLarge)
    HaloRollingAmount(4_812_350, unit = AmountUnit.Fiat, fiatPerBtc = 104_250.0, style = HaloTheme.typography.amountMedium)
}

@Preview
@Composable
private fun HaloRollingAmountHiddenPreview() = HaloPreview {
    HaloRollingAmount(4_812_350, hidden = true)
}
