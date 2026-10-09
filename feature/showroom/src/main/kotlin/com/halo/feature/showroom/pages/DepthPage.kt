package com.halo.feature.showroom.pages

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import com.halo.core.designsystem.foundation.HaloDepth
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.backdropSource
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.foundation.haloGlow
import com.halo.core.designsystem.foundation.haloGrain
import com.halo.core.designsystem.foundation.rememberHaloBackdrop
import com.halo.core.designsystem.icons.HaloIcon
import com.halo.core.designsystem.icons.HaloIcons
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.haloDepth
import kotlin.math.roundToInt

@Composable
internal fun DepthPage() {
    val s = HaloTheme.spacing
    val colors = HaloTheme.colors

    SectionLabel("Levels", Modifier.enterRise(0))
    Column(Modifier.enterRise(1), verticalArrangement = Arrangement.spacedBy(s.sm)) {
        listOf(
            HaloDepth.Canvas to "L0 · canvas — no edge",
            HaloDepth.Surface to "L1 · surface + line.subtle",
            HaloDepth.Raised to "L2 · raised + line.default + top light",
            HaloDepth.Glass to "L3 · glass + blur + line.strong",
        ).forEach { (level, label) ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(HaloTheme.sizes.controlXl)
                    .haloDepth(level, RoundedCornerShape(HaloTheme.radius.lg))
                    .padding(horizontal = s.lg),
                contentAlignment = Alignment.CenterStart,
            ) {
                HaloText(label, style = HaloTheme.typography.data, color = colors.content.secondary)
            }
        }
    }

    SectionLabel("Glass · drag the card", Modifier.enterRise(2))
    GlassPlayground(Modifier.enterRise(3))

    SectionLabel("Glow replaces shadow", Modifier.enterRise(4))
    ShowroomCard(Modifier.enterRise(5)) {
        val pulse by rememberInfiniteTransition(label = "glow").animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
            label = "glowPulse",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(s.xxl), verticalAlignment = Alignment.CenterVertically) {
            GlowChip("accent", colors.accent.fill, colors.accent.glow, colors.accent.onFill) { pulse }
            GlowChip("bitcoin", colors.bitcoin.fill, colors.bitcoin.glow, colors.bitcoin.onFill) { pulse }
        }
    }

    SectionLabel("Grain", Modifier.enterRise(6))
    Row(Modifier.enterRise(7), horizontalArrangement = Arrangement.spacedBy(s.sm)) {
        listOf(false, true).forEach { grain ->
            val shape = RoundedCornerShape(HaloTheme.radius.lg)
            Box(
                Modifier
                    .weight(1f)
                    .height(HaloTheme.sizes.controlXl * 2)
                    .background(Brush.verticalGradient(listOf(colors.bg.raised, colors.bg.canvas)), shape)
                    .then(if (grain) Modifier.haloGrain(HaloTheme.effects.grain) else Modifier)
                    .haloDepth(HaloDepth.Canvas, shape, fill = Color.Transparent)
                    .padding(s.md),
                contentAlignment = Alignment.BottomStart,
            ) {
                HaloText(if (grain) "grain ${HaloTheme.effects.grain}" else "no grain", style = HaloTheme.typography.micro, color = colors.content.tertiary)
            }
        }
    }
}

@Composable
private fun GlowChip(label: String, fill: Color, glow: Color, onFill: Color, intensity: () -> Float) {
    val shape = HaloTheme.radius.full
    Box(
        Modifier
            .haloGlow(glow, shape, HaloTheme.effects.glowRadius, HaloTheme.effects.glowSpread, intensity)
            .background(fill, shape)
            .height(HaloTheme.sizes.controlMd)
            .padding(horizontal = HaloTheme.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        HaloText(label, style = HaloTheme.typography.label, color = onFill)
    }
}

@Composable
private fun GlassPlayground(modifier: Modifier = Modifier) {
    val backdrop = rememberHaloBackdrop()
    val colors = HaloTheme.colors
    val s = HaloTheme.spacing
    val shape = RoundedCornerShape(HaloTheme.radius.xl)
    var offset by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(HaloTheme.sizes.controlXl * 4)
            .haloDepth(HaloDepth.Surface, shape),
    ) {
        val density = LocalDensity.current
        val cardW = maxWidth * 0.62f
        val cardH = HaloTheme.sizes.controlXl * 1.6f
        val maxX = with(density) { (maxWidth - cardW).toPx() }
        val maxY = with(density) { (maxHeight - cardH).toPx() }
        // Backdrop source: light blobs + type, so the blur has something to bend.
        Box(
            Modifier
                .fillMaxSize()
                .backdropSource(backdrop)
                .drawBehind {
                    drawCircle(Brush.radialGradient(listOf(colors.accent.fill, Color.Transparent), Offset(size.width * 0.2f, size.height * 0.3f), size.minDimension * 0.45f), size.minDimension * 0.45f, Offset(size.width * 0.2f, size.height * 0.3f))
                    drawCircle(Brush.radialGradient(listOf(colors.bitcoin.fill, Color.Transparent), Offset(size.width * 0.8f, size.height * 0.7f), size.minDimension * 0.5f), size.minDimension * 0.5f, Offset(size.width * 0.8f, size.height * 0.7f))
                }
                .padding(s.xl),
        ) {
            HaloText("0.04812", style = HaloTheme.typography.amountLarge, modifier = Modifier.align(Alignment.TopEnd))
            HaloText("LIGHT · DEPTH · GLASS", style = HaloTheme.typography.overline, modifier = Modifier.align(Alignment.BottomStart))
        }
        Box(
            Modifier
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .size(cardW, cardH)
                .haloDepth(HaloDepth.Glass, RoundedCornerShape(HaloTheme.radius.lg), backdrop = backdrop)
                .semantics { contentDescription = "Draggable glass card" }
                .pointerInput(maxX, maxY) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        offset = Offset(
                            (offset.x + drag.x).coerceIn(0f, maxX),
                            (offset.y + drag.y).coerceIn(0f, maxY),
                        )
                    }
                }
                .padding(s.lg),
        ) {
            Column {
                HaloIcon(HaloIcons.Bolt, contentDescription = null, size = HaloTheme.sizes.iconMd, tint = colors.bitcoin.text)
                Spacer(Modifier.weight(1f))
                HaloText("Glass · L3", style = HaloTheme.typography.subtitle)
                HaloText("blur ${HaloTheme.effects.glassBlur.value.toInt()}dp", style = HaloTheme.typography.data, color = colors.content.secondary)
            }
        }
    }
}
