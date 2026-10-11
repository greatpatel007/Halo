package com.halo.feature.showroom.pages.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.halo.core.designsystem.component.HaloLogo
import com.halo.core.designsystem.component.HaloLogoFrame
import com.halo.core.designsystem.component.HaloLogoSize
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.ActionChip
import com.halo.feature.showroom.ui.Knob
import com.halo.feature.showroom.ui.SectionLabel
import com.halo.feature.showroom.ui.ShowroomCard
import com.halo.feature.showroom.ui.SpecLine

@Composable
internal fun LogoPage() {
    val s = HaloTheme.spacing
    val t = HaloTheme.typography
    val colors = HaloTheme.colors
    val surface = colors.bg.surface

    // ---- 1. Playground: the live mark. It plays on appear; tap it (or replay) to send the light round again.
    var size by rememberSaveable { mutableStateOf(HaloLogoSize.Large) }
    var animated by rememberSaveable { mutableStateOf(true) }
    var plays by rememberSaveable { mutableIntStateOf(0) }
    SectionLabel("Playground · tap to push · twist the phone", Modifier.enterRise(0))
    ShowroomCard(Modifier.enterRise(1)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(HaloTheme.sizes.controlXl * 2),
            contentAlignment = Alignment.Center,
        ) {
            // A new key re-creates the mark, so it plays its entrance again.
            key(plays) { HaloLogo(size = size, animated = animated, surfaceColor = surface) }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Knob("Size", HaloLogoSize.entries, size, { size = it }, optionLabel = { if (it == HaloLogoSize.Large) "L" else "M" })
            Spacer(Modifier.width(s.xl))
            Knob("Animated", listOf(true, false), animated, { animated = it }, optionLabel = { if (it) "on" else "off" })
            Spacer(Modifier.weight(1f))
            ActionChip("replay", { plays++ })
        }
    }

    // ---- 2. State matrix: moments of the motion, frozen.
    SectionLabel("States · unlit / steady / pushed / pressed", Modifier.enterRise(2))
    ShowroomCard(Modifier.enterRise(3)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Frame("unlit") { HaloLogoFrame(flowRate = 0f, glow = 0f, surfaceColor = surface) }
            Frame("steady") { HaloLogoFrame(flowRate = 20f, glow = 1f, surfaceColor = surface) }
            Frame("pushed") { HaloLogoFrame(flowRate = 340f, glow = 1f, surfaceColor = surface) }
            Frame("pressed") { HaloLogoFrame(flowRate = 20f, glow = 1.6f, surfaceColor = surface) }
        }
        Spacer(Modifier.height(s.lg))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.lg)) {
            HaloText("medium", style = t.data, color = colors.content.tertiary)
            HaloLogo(size = HaloLogoSize.Medium, animated = false, surfaceColor = surface)
            HaloText("static", style = t.data, color = colors.content.tertiary)
        }
    }

    // ---- 3. Spec
    SectionLabel("Spec", Modifier.enterRise(4))
    ShowroomCard(Modifier.enterRise(5)) {
        SpecLine("box", "logoLg 56 · logoMd 32 · touch ≥ 48")
        SpecLine("tube", "ring 0.36W · tube 0.058W · bore 78%")
        SpecLine("liquid", "laminar · Dean shift · scintillation")
        SpecLine("physics", "τ 1.4s · tap +6 rad/s · gyro")
        SpecLine("light", "key top-left · Fresnel · caustic shadow")
        SpecLine("ignite", "Gentle · press +60% glow")
        SpecLine("haptic", "Tick on tap")
        SpecLine("frames", "~30 fps calm · 0 when hidden")
    }
}

@Composable
private fun Frame(caption: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        content()
        Spacer(Modifier.height(HaloTheme.spacing.sm))
        HaloText(caption, style = HaloTheme.typography.data, color = HaloTheme.colors.content.tertiary)
    }
}
