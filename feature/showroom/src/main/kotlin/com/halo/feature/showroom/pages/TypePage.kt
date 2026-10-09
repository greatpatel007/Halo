package com.halo.feature.showroom.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import com.halo.core.designsystem.foundation.HaloDepth
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.Knob
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.haloDepth
import java.util.Locale

private class TypeSpec(val token: String, val style: TextStyle, val spec: String, val sample: String? = null)

@Composable
internal fun TypePage() {
    val t = HaloTheme.typography
    var preview by rememberSaveable { mutableStateOf("") }
    var fontScale by rememberSaveable { mutableFloatStateOf(1f) }
    val specs = listOf(
        TypeSpec("amountHero", t.amountHero, "Paper Mono Light 48/52 −3%", "4 812 350"),
        TypeSpec("amountLarge", t.amountLarge, "Paper Mono Regular 32/38 −2%", "0.04812350"),
        TypeSpec("amountMedium", t.amountMedium, "Paper Mono Regular 20/24 −1%", "$5,016.34"),
        TypeSpec("amountSmall", t.amountSmall, "Paper Mono Medium 15/20", "+21 000 sats"),
        TypeSpec("headline", t.headline, "Satoshi Bold 26/32 −2%", "Send bitcoin"),
        TypeSpec("title", t.title, "Satoshi Bold 20/26 −1%", "Review payment"),
        TypeSpec("subtitle", t.subtitle, "Satoshi Medium 17/22 −0.5%", "Blue Bottle Café"),
        TypeSpec("body", t.body, "Satoshi Regular 15/22", "Lightning payments settle in seconds and cost a fraction of a cent."),
        TypeSpec("bodySmall", t.bodySmall, "Satoshi Regular 13/18", "Network fee is paid to miners, not to Halo."),
        TypeSpec("label", t.label, "Satoshi Bold 15/20", "Continue"),
        TypeSpec("overline", t.overline, "Paper Mono Medium 11/14 +8% CAPS", "RECENT ACTIVITY"),
        TypeSpec("data", t.data, "Paper Mono Regular 13/18", "bc1qar0s…zwf5mdq · 14:02"),
        TypeSpec("micro", t.micro, "Paper Mono SemiBold 10/12 +6%", "PENDING"),
    )

    SectionLabel("Families", Modifier.enterRise(0))
    ShowroomCard(Modifier.enterRise(1)) {
        HaloText("Satoshi", style = t.headline)
        HaloText("UI & body — variable 300–900, pinned to 400 / 500 / 700", style = t.bodySmall, color = HaloTheme.colors.content.secondary)
        Spacer(Modifier.height(HaloTheme.spacing.lg))
        HaloText("Paper Mono", style = t.amountLarge)
        HaloText("Every number, amount, address and label — 8 static weights", style = t.bodySmall, color = HaloTheme.colors.content.secondary)
    }

    SectionLabel("Try it", Modifier.enterRise(2))
    Column(Modifier.enterRise(3)) {
        val fieldShape = RoundedCornerShape(HaloTheme.radius.md)
        BasicTextField(
            value = preview,
            onValueChange = { preview = it },
            singleLine = true,
            textStyle = t.body.copy(color = HaloTheme.colors.content.primary),
            cursorBrush = SolidColor(HaloTheme.colors.accent.fill),
            modifier = Modifier
                .fillMaxWidth()
                .haloDepth(HaloDepth.Surface, fieldShape)
                .padding(HaloTheme.spacing.lg),
            decorationBox = { inner ->
                if (preview.isEmpty()) {
                    HaloText("Type to preview every style…", style = t.body, color = HaloTheme.colors.content.tertiary)
                }
                inner()
            },
        )
        Spacer(Modifier.height(HaloTheme.spacing.lg))
        Knob(
            label = "Font scale",
            options = listOf(1f, 1.3f, 1.5f, 2f),
            selected = fontScale,
            onSelect = { fontScale = it },
            optionLabel = { String.format(Locale.US, "%.1f×", it) },
        )
    }

    SectionLabel("Scale", Modifier.enterRise(4))
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        Column(verticalArrangement = Arrangement.spacedBy(HaloTheme.spacing.sm)) {
            specs.forEachIndexed { i, spec ->
                ShowroomCard(Modifier.enterRise(5 + i)) {
                    HaloText(spec.token, style = t.micro, color = HaloTheme.colors.accent.text)
                    Spacer(Modifier.height(HaloTheme.spacing.md))
                    HaloText(
                        text = preview.ifBlank { spec.sample ?: spec.token },
                        style = spec.style,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(HaloTheme.spacing.md))
                    HaloText(spec.spec, style = t.data, color = HaloTheme.colors.content.tertiary)
                }
            }
        }
    }
}
