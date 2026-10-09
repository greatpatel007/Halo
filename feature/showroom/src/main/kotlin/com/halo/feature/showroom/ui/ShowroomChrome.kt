package com.halo.feature.showroom.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.halo.core.designsystem.foundation.HaloBackdrop
import com.halo.core.designsystem.foundation.HaloDepth
import com.halo.core.designsystem.foundation.HaloPressable
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.backdropSource
import com.halo.core.designsystem.foundation.depth
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.foundation.rememberHaloBackdrop
import com.halo.core.designsystem.icons.HaloIcon
import com.halo.core.designsystem.icons.HaloIcons
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloTheme

/*
 * Showroom chrome — scaffolding for the catalog, built only from design-system foundations.
 * These are not design-system components; real components come from the halo-component skill.
 */

/** Reads theme tokens and applies [depth] — shorthand for showroom code. */
@Composable
internal fun Modifier.haloDepth(
    level: HaloDepth,
    shape: Shape,
    fill: Color = Color.Unspecified,
    backdrop: HaloBackdrop? = null,
): Modifier = depth(level, shape, HaloTheme.colors, HaloTheme.stroke, HaloTheme.effects, fill, backdrop)

/** Height the floating debug dock occupies at the bottom of every page. */
internal val DockClearance = 96.dp

/**
 * Page frame: scrolling content under an edge-to-edge glass top bar that materialises as you scroll.
 */
@Composable
internal fun ShowroomScaffold(
    title: String,
    summary: String?,
    onBack: (() -> Unit)?,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val backdrop = rememberHaloBackdrop()
    val spacing = HaloTheme.spacing
    val barHeight = HaloTheme.sizes.controlLg
    val revealPx = with(LocalDensity.current) { spacing.x4.toPx() }
    Box(
        Modifier
            .fillMaxSize()
            .background(HaloTheme.colors.bg.canvas),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .backdropSource(backdrop)
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.gutter),
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(Modifier.height(barHeight + spacing.lg))
            HaloText(
                text = title,
                style = HaloTheme.typography.headline,
                modifier = Modifier
                    .enterRise(0)
                    .semantics { heading() },
            )
            if (summary != null) {
                Spacer(Modifier.height(spacing.sm))
                HaloText(
                    text = summary,
                    style = HaloTheme.typography.body,
                    color = HaloTheme.colors.content.secondary,
                    modifier = Modifier.enterRise(1),
                )
            }
            Spacer(Modifier.height(spacing.x3))
            content()
            Spacer(Modifier.height(DockClearance))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }

        // Glass top bar — fades in over the first 40dp of scroll.
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = (scrollState.value / revealPx).coerceIn(0f, 1f) }
                .haloDepth(HaloDepth.Glass, RectangleShape, backdrop = backdrop)
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(barHeight),
        ) {
            HaloText(
                text = title,
                style = HaloTheme.typography.subtitle,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (onBack != null) {
            Box(
                Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(barHeight)
                    .padding(start = spacing.md),
                contentAlignment = Alignment.CenterStart,
            ) {
                IconButton(onClick = onBack, label = "Back") {
                    HaloIcon(HaloIcons.Back, contentDescription = null, size = HaloTheme.sizes.iconMd)
                }
            }
        }
    }
}

/** 40dp circular raised button with a 48dp touch target. */
@Composable
internal fun IconButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    HaloPressable(
        onClick = onClick,
        onClickLabel = label,
        shape = CircleShape,
        modifier = modifier
            .size(HaloTheme.sizes.minTouch)
            .padding(HaloTheme.spacing.xs)
            .haloDepth(HaloDepth.Raised, CircleShape),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/** Overline section label. */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    HaloText(
        text = text.uppercase(),
        style = HaloTheme.typography.overline,
        color = HaloTheme.colors.content.tertiary,
        modifier = modifier
            .padding(top = HaloTheme.spacing.xxl, bottom = HaloTheme.spacing.md)
            .semantics { heading() },
    )
}

/** Segmented option picker used for page "knobs". */
@Composable
internal fun <T> Knob(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionLabel: (T) -> String = { it.toString() },
) {
    Column(modifier) {
        HaloText(
            text = label.uppercase(),
            style = HaloTheme.typography.micro,
            color = HaloTheme.colors.content.tertiary,
        )
        Spacer(Modifier.height(HaloTheme.spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(HaloTheme.spacing.xs)) {
            options.forEach { option -> KnobChip(optionLabel(option), option == selected) { onSelect(option) } }
        }
    }
}

@Composable
private fun KnobChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = HaloTheme.colors
    val shape = HaloTheme.radius.full
    HaloPressable(
        onClick = onClick,
        shape = shape,
        haptic = HaloHaptic.Tick,
        modifier = Modifier
            .height(HaloTheme.sizes.controlSm)
            .haloDepth(HaloDepth.Surface, shape, fill = if (selected) colors.accent.tint else Color.Unspecified)
            .padding(horizontal = HaloTheme.spacing.md),
    ) {
        Box(contentAlignment = Alignment.Center) {
            HaloText(
                text = text,
                style = HaloTheme.typography.data,
                color = if (selected) colors.accent.text else colors.content.secondary,
            )
        }
    }
}

/** A one-shot action chip in Paper Mono (for numeric actions like "+21"). */
@Composable
internal fun ActionChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = HaloTheme.radius.full
    HaloPressable(
        onClick = onClick,
        shape = shape,
        haptic = null,
        modifier = modifier
            .height(HaloTheme.sizes.controlSm)
            .haloDepth(HaloDepth.Raised, shape)
            .padding(horizontal = HaloTheme.spacing.md),
    ) {
        Box(contentAlignment = Alignment.Center) {
            HaloText(text = text, style = HaloTheme.typography.data)
        }
    }
}

/** "label ........ value" spec line in Paper Mono. */
@Composable
internal fun SpecLine(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HaloText(label, style = HaloTheme.typography.data, color = HaloTheme.colors.content.tertiary)
        Spacer(Modifier.weight(1f))
        HaloText(value, style = HaloTheme.typography.data, color = HaloTheme.colors.content.secondary)
    }
}

/** A raised card section. */
@Composable
internal fun ShowroomCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(HaloTheme.radius.lg)
    Column(
        modifier
            .fillMaxWidth()
            .haloDepth(HaloDepth.Surface, shape)
            .padding(HaloTheme.spacing.lg),
        content = content,
    )
}

@Composable
internal fun RowScope.Fill() = Spacer(Modifier.weight(1f))
