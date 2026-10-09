package com.halo.feature.showroom.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.halo.core.designsystem.foundation.HaloDepth
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.icons.HaloIcon
import com.halo.core.designsystem.icons.HaloIcons
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.Knob
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.haloDepth

@Composable
internal fun IconsPage() {
    val sizes = HaloTheme.sizes
    val options = listOf(sizes.iconSm, sizes.iconMd, sizes.iconLg, sizes.iconXl)
    var sizeIndex by rememberSaveable { mutableStateOf(2) }
    Knob(
        label = "Size",
        options = options.indices.toList(),
        selected = sizeIndex,
        onSelect = { sizeIndex = it },
        optionLabel = { "${options[it].value.toInt()}dp" },
        modifier = Modifier.enterRise(0),
    )
    SectionLabel("Set · ${HaloIcons.all.size}", Modifier.enterRise(1))
    Column(Modifier.enterRise(2), verticalArrangement = Arrangement.spacedBy(HaloTheme.spacing.sm)) {
        HaloIcons.all.chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(HaloTheme.spacing.sm)) {
                row.forEach { (name, icon) -> IconTile(name, icon, options[sizeIndex], Modifier.weight(1f)) }
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val COLUMNS = 4

@Composable
private fun IconTile(name: String, icon: ImageVector, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(HaloTheme.radius.md)
    Column(
        modifier
            .aspectRatio(0.9f)
            .haloDepth(HaloDepth.Surface, shape)
            .padding(HaloTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            HaloIcon(icon, contentDescription = name, size = size)
        }
        HaloText(name, style = HaloTheme.typography.micro, color = HaloTheme.colors.content.tertiary, maxLines = 1)
        Spacer(Modifier.height(HaloTheme.spacing.xxs))
    }
}
