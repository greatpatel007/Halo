package com.halo.feature.showroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halo.core.designsystem.host.HaloThemeSwitch
import com.halo.core.designsystem.component.HaloLogo
import com.halo.core.designsystem.foundation.AmountFormat
import com.halo.core.designsystem.foundation.HaloDepth
import com.halo.core.designsystem.foundation.HaloPressable
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.backdropSource
import com.halo.core.designsystem.foundation.rememberHaloBackdrop
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.foundation.haloGlow
import com.halo.core.designsystem.foundation.haloGrain
import com.halo.core.designsystem.icons.HaloIcon
import com.halo.core.designsystem.icons.HaloIcons
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.catalog.ShowroomCatalog
import com.halo.feature.showroom.catalog.ShowroomPage
import com.halo.feature.showroom.catalog.ShowroomSection
import com.halo.feature.showroom.ui.DockClearance
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.haloDepth

@Composable
fun ShowroomHomeScreen(
    onOpenPage: (String) -> Unit,
    viewModel: ShowroomHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ShowroomHomeContent(
        uiState = uiState,
        pages = ShowroomCatalog.pages,
        onOpenPage = onOpenPage,
        appearanceSwitch = { HaloThemeSwitch() },
    )
}

@Composable
internal fun ShowroomHomeContent(
    uiState: ShowroomHomeUiState,
    pages: List<ShowroomPage>,
    onOpenPage: (String) -> Unit,
    appearanceSwitch: @Composable () -> Unit = {},
) {
    val spacing = HaloTheme.spacing
    val backdrop = rememberHaloBackdrop()
    val scrollState = rememberScrollState()
    val revealPx = with(LocalDensity.current) { spacing.x4.toPx() }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(HaloTheme.colors.bg.canvas)
                .backdropSource(backdrop)
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.gutter),
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(Modifier.height(spacing.xxl))
            Hero(uiState, appearanceSwitch, Modifier.enterRise(0))
            var index = 1
            ShowroomSection.entries.forEach { section ->
                val sectionPages = pages.filter { it.section == section }
                SectionLabel(section.title, Modifier.enterRise(index++))
                if (sectionPages.isEmpty()) {
                    HaloText(
                        text = "Nothing here yet — build one with /halo-component.",
                        style = HaloTheme.typography.bodyMedium,
                        color = HaloTheme.colors.content.tertiary,
                        modifier = Modifier.enterRise(index++),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    sectionPages.forEachIndexed { i, page ->
                        PageRow(number = i + 1, page = page, onClick = { onOpenPage(page.id) }, modifier = Modifier.enterRise(index++))
                    }
                }
            }
            Spacer(Modifier.height(DockClearance))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
        // Status-bar protection: content scrolls under a glass strip, never under bare system icons. It fades in
        // over the first 40dp of scroll (like ShowroomScaffold's bar), so at rest the canvas runs edge to edge.
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = (scrollState.value / revealPx).coerceIn(0f, 1f) }
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .haloDepth(HaloDepth.Glass, RectangleShape, backdrop = backdrop),
        )
    }
}

@Composable
private fun Hero(uiState: ShowroomHomeUiState, appearanceSwitch: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val colors = HaloTheme.colors
    val spacing = HaloTheme.spacing
    val shape = RoundedCornerShape(HaloTheme.radius.xl)
    Column(
        modifier
            .fillMaxWidth()
            .haloDepth(HaloDepth.Raised, shape)
            .haloGrain(HaloTheme.effects.grain)
            .padding(spacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .haloGlow(colors.accent.glow, HaloTheme.radius.full, HaloTheme.effects.glowRadius, HaloTheme.effects.glowSpread)
                    .background(colors.accent.fill, HaloTheme.radius.full)
                    .width(spacing.sm)
                    .height(spacing.sm),
            )
            Spacer(Modifier.width(spacing.sm))
            HaloText("DESIGN SYSTEM  ·  V0.1", style = HaloTheme.typography.overline, color = colors.content.secondary)
            Spacer(Modifier.weight(1f))
            appearanceSwitch()
        }
        Spacer(Modifier.height(spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HaloLogo()
            Spacer(Modifier.width(spacing.md))
            HaloText(
                text = "HALO",
                style = HaloTheme.typography.amountHero.copy(fontWeight = FontWeight.ExtraLight, fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = 0.sp),
            )
        }
        Spacer(Modifier.height(spacing.md))
        HaloText(
            text = "Hand-built Compose components for a Bitcoin wallet. Every pixel, spring and haptic is a token.",
            style = HaloTheme.typography.body,
            color = colors.content.secondary,
        )
        Spacer(Modifier.height(spacing.xl))
        LiveBackendLine(uiState)
    }
}

@Composable
private fun LiveBackendLine(uiState: ShowroomHomeUiState) {
    val colors = HaloTheme.colors
    when (uiState) {
        ShowroomHomeUiState.Loading -> HaloText("Connecting demo backend…", style = HaloTheme.typography.data, color = colors.content.tertiary)
        is ShowroomHomeUiState.Ready -> Column {
            HaloText("DEMO BACKEND · LIVE", style = HaloTheme.typography.micro, color = colors.accent.text)
            Spacer(Modifier.height(HaloTheme.spacing.sm))
            HaloText(AmountFormat.satsAnnotated(uiState.balanceSats), style = HaloTheme.typography.amountMedium)
            Spacer(Modifier.height(HaloTheme.spacing.xs))
            HaloText(
                text = "≈ ${AmountFormat.fiat(uiState.balanceSats, uiState.fiatPerBtc)} · ${uiState.transactionCount} tx",
                style = HaloTheme.typography.data,
                color = colors.content.tertiary,
            )
        }
    }
}

@Composable
private fun PageRow(number: Int, page: ShowroomPage, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = HaloTheme.spacing
    val shape = RoundedCornerShape(HaloTheme.radius.lg)
    HaloPressable(
        onClick = onClick,
        onClickLabel = "Open ${page.title}",
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .haloDepth(HaloDepth.Surface, shape),
    ) {
        Row(
            Modifier.padding(horizontal = spacing.lg, vertical = spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HaloText(
                text = number.toString().padStart(2, '0'),
                style = HaloTheme.typography.data,
                color = HaloTheme.colors.content.tertiary,
            )
            Spacer(Modifier.width(spacing.lg))
            Column(Modifier.weight(1f)) {
                HaloText(page.title, style = HaloTheme.typography.subtitle)
                Spacer(Modifier.height(spacing.xxs))
                HaloText(page.summary, style = HaloTheme.typography.bodyMedium, color = HaloTheme.colors.content.secondary, maxLines = 1)
            }
            Spacer(Modifier.width(spacing.sm))
            HaloIcon(HaloIcons.ChevronRight, contentDescription = null, size = HaloTheme.sizes.iconMd, tint = HaloTheme.colors.content.tertiary)
        }
    }
}
