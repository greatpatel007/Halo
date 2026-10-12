package com.halo.feature.showroom.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.halo.core.designsystem.foundation.HaloDepth
import com.halo.core.designsystem.foundation.HaloPressable
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.theme.HaloMotion
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.haloDepth
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private const val WINDOW_MS = 800
private const val SAMPLES = 96
private const val Y_MIN = -0.05f
private const val Y_MAX = 1.3f

@Composable
internal fun MotionPage() {
    val s = HaloTheme.spacing
    SectionLabel("Springs · tap a card to retarget mid-flight", Modifier.enterRise(0))
    Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
        HaloSpring.entries.forEachIndexed { i, token -> SpringCard(token, Modifier.enterRise(1 + i)) }
    }

    SectionLabel("Choreography · enter stagger", Modifier.enterRise(8))
    var replay by remember { mutableIntStateOf(0) }
    ShowroomCard(Modifier.enterRise(9)) {
        HaloText(
            "Rise ${HaloTheme.motion.enterRise.value.toInt()}dp + fade on Gentle, ${HaloMotion.STAGGER_STEP_MS}ms per item, capped at ${HaloMotion.STAGGER_CAP}.",
            style = HaloTheme.typography.bodyMedium,
            color = HaloTheme.colors.content.secondary,
        )
        Spacer(Modifier.height(s.lg))
        key(replay) {
            Column(verticalArrangement = Arrangement.spacedBy(s.xs)) {
                repeat(6) { i ->
                    Box(
                        Modifier
                            .enterRise(i)
                            .fillMaxWidth(1f - i * 0.08f)
                            .height(s.md)
                            .background(HaloTheme.colors.bg.raised, HaloTheme.radius.full),
                    )
                }
            }
        }
        Spacer(Modifier.height(s.lg))
        HaloPressable(
            onClick = { replay++ },
            shape = HaloTheme.radius.full,
            modifier = Modifier
                .height(HaloTheme.sizes.controlSm)
                .haloDepth(HaloDepth.Raised, HaloTheme.radius.full)
                .padding(horizontal = s.lg),
        ) {
            Box(contentAlignment = Alignment.Center) { HaloText("Replay", style = HaloTheme.typography.label) }
        }
    }
}

@Composable
private fun SpringCard(token: HaloSpring, modifier: Modifier = Modifier) {
    val colors = HaloTheme.colors
    val s = HaloTheme.spacing
    val motion = HaloTheme.motion
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var target by remember { mutableStateOf(0f) }
    val reference = remember(token) { TargetBasedAnimation(HaloMotion().spring<Float>(token), Float.VectorConverter, 0f, 1f) }
    val curve = remember(token) { FloatArray(SAMPLES) { i -> reference.getValueFromNanos(i * WINDOW_MS * 1_000_000L / (SAMPLES - 1)) } }
    val settleMs = reference.durationNanos / 1_000_000L
    LaunchedEffect(Unit) { target = 1f; progress.animateTo(1f, motion.spring(token)) }
    val shape = RoundedCornerShape(HaloTheme.radius.lg)
    HaloPressable(
        onClick = {
            target = 1f - target
            scope.launch { progress.animateTo(target, motion.spring(token)) }
        },
        onClickLabel = "Play ${token.name}",
        shape = shape,
        pressedScale = 0.99f,
        modifier = modifier
            .fillMaxWidth()
            .haloDepth(HaloDepth.Surface, shape),
    ) {
        Column(Modifier.padding(s.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HaloText(token.name, style = HaloTheme.typography.subtitle)
                Spacer(Modifier.weight(1f))
                HaloText(
                    String.format(Locale.US, "ζ %.2f · k %.0f · ~%dms", token.dampingRatio, token.stiffness, settleMs),
                    style = HaloTheme.typography.data,
                    color = colors.content.tertiary,
                )
            }
            Spacer(Modifier.height(s.md))
            val accent = colors.accent.fill
            val guide = colors.line.default
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(HaloTheme.sizes.controlXl),
            ) {
                fun y(v: Float) = size.height * (1f - (v - Y_MIN) / (Y_MAX - Y_MIN))
                val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
                drawLine(guide, Offset(0f, y(1f)), Offset(size.width, y(1f)), pathEffect = dash)
                drawLine(guide, Offset(0f, y(0f)), Offset(size.width, y(0f)))
                val path = Path()
                curve.forEachIndexed { i, v ->
                    val x = size.width * i / (SAMPLES - 1)
                    if (i == 0) path.moveTo(x, y(v)) else path.lineTo(x, y(v))
                }
                drawPath(path, accent, style = Stroke(width = 2f * density / 1.5f, cap = StrokeCap.Round))
            }
            Spacer(Modifier.height(s.md))
            PuckTrack(progress = { progress.value })
        }
    }
}

@Composable
private fun PuckTrack(progress: () -> Float) {
    val puck = HaloTheme.sizes.iconLg
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(puck)
            .background(HaloTheme.colors.bg.canvas, HaloTheme.radius.full),
    ) {
        val travel = with(LocalDensity.current) { (maxWidth - puck).toPx() }
        Box(
            Modifier
                .offset { IntOffset((progress() * travel).roundToInt(), 0) }
                .size(puck)
                .background(HaloTheme.colors.accent.fill, CircleShape),
        )
    }
}
