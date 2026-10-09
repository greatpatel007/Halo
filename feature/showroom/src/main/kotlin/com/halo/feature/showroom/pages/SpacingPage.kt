package com.halo.feature.showroom.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.halo.core.designsystem.foundation.HaloDepth
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.SpecLine
import com.halo.feature.showroom.ui.haloDepth

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpacingPage() {
    val s = HaloTheme.spacing
    val r = HaloTheme.radius
    val z = HaloTheme.sizes
    val st = HaloTheme.stroke

    SectionLabel("Spacing · 4dp grid", Modifier.enterRise(0))
    ShowroomCard(Modifier.enterRise(1)) {
        Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
            listOf(
                "xxs" to s.xxs, "xs" to s.xs, "sm" to s.sm, "md" to s.md, "lg" to s.lg, "xl" to s.xl,
                "xxl" to s.xxl, "x3" to s.x3, "x4" to s.x4, "x5" to s.x5, "x6" to s.x6,
            ).forEach { (name, value) -> SpaceBar(name, value) }
        }
        Spacer(Modifier.height(s.lg))
        SpecLine("gutter", "${s.gutter.value.toInt()}dp")
    }

    SectionLabel("Radius", Modifier.enterRise(2))
    FlowRow(
        Modifier.enterRise(3),
        horizontalArrangement = Arrangement.spacedBy(s.sm),
        verticalArrangement = Arrangement.spacedBy(s.sm),
    ) {
        listOf("xs" to r.xs, "sm" to r.sm, "md" to r.md, "lg" to r.lg, "xl" to r.xl, "sheet" to r.sheet).forEach { (name, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(z.controlXl + s.lg)
                        .haloDepth(HaloDepth.Raised, RoundedCornerShape(value)),
                )
                Spacer(Modifier.height(s.xs))
                HaloText("$name ${value.value.toInt()}", style = HaloTheme.typography.micro, color = HaloTheme.colors.content.tertiary)
            }
        }
    }
    Spacer(Modifier.height(s.md))
    ShowroomCard(Modifier.enterRise(4)) {
        HaloText("Nested radius = outer − padding", style = HaloTheme.typography.subtitle)
        Spacer(Modifier.height(s.md))
        val outer = r.xl
        Box(
            Modifier
                .fillMaxWidth()
                .haloDepth(HaloDepth.Raised, RoundedCornerShape(outer)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(z.controlXl)
                    .padding(s.sm)
                    .background(HaloTheme.colors.accent.fill, RoundedCornerShape(r.nested(outer, s.sm))),
            )
        }
        Spacer(Modifier.height(s.sm))
        SpecLine("outer xl / pad sm", "${outer.value.toInt()} − ${s.sm.value.toInt()} = ${r.nested(outer, s.sm).value.toInt()}dp")
    }

    SectionLabel("Sizes", Modifier.enterRise(5))
    ShowroomCard(Modifier.enterRise(6)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(s.sm)) {
            listOf("sm" to z.controlSm, "md" to z.controlMd, "lg" to z.controlLg, "xl" to z.controlXl).forEach { (name, h) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(h)
                            .haloDepth(HaloDepth.Raised, HaloTheme.radius.full),
                    )
                    Spacer(Modifier.height(s.xs))
                    HaloText("$name ${h.value.toInt()}", style = HaloTheme.typography.micro, color = HaloTheme.colors.content.tertiary)
                }
            }
        }
        Spacer(Modifier.height(s.lg))
        SpecLine("min touch target", "${z.minTouch.value.toInt()}dp")
        SpecLine("icons", "${z.iconSm.value.toInt()} / ${z.iconMd.value.toInt()} / ${z.iconLg.value.toInt()} / ${z.iconXl.value.toInt()}")
    }

    SectionLabel("Stroke", Modifier.enterRise(7))
    ShowroomCard(Modifier.enterRise(8)) {
        listOf("hairline (1px)" to st.hairline, "thin" to st.thin, "icon" to st.icon, "focus" to st.focus).forEach { (name, w) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(z.controlSm)) {
                HaloText(name, style = HaloTheme.typography.data, color = HaloTheme.colors.content.secondary, modifier = Modifier.width(z.controlXl * 2))
                Box(
                    Modifier
                        .weight(1f)
                        .height(w)
                        .background(HaloTheme.colors.content.primary),
                )
            }
        }
    }
}

@Composable
private fun SpaceBar(name: String, value: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HaloText(name, style = HaloTheme.typography.data, color = HaloTheme.colors.content.secondary, modifier = Modifier.width(HaloTheme.sizes.controlXl))
        Box(
            Modifier
                .width(value * 3)
                .height(HaloTheme.spacing.md)
                .background(HaloTheme.colors.accent.fill, RoundedCornerShape(HaloTheme.spacing.xxs)),
        )
        Spacer(Modifier.weight(1f))
        HaloText("${value.value.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() }}dp", style = HaloTheme.typography.data, color = HaloTheme.colors.content.tertiary)
    }
}
