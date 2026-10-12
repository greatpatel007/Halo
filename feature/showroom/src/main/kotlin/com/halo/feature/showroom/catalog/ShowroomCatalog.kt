package com.halo.feature.showroom.catalog

import androidx.compose.runtime.Composable
import com.halo.feature.showroom.pages.BrandPage
import com.halo.feature.showroom.pages.ColorPage
import com.halo.feature.showroom.pages.DepthPage
import com.halo.feature.showroom.pages.SurfacePage
import com.halo.feature.showroom.pages.HapticsPage
import com.halo.feature.showroom.pages.IconsPage
import com.halo.feature.showroom.pages.RuntimePage
import com.halo.feature.showroom.pages.MotionPage
import com.halo.feature.showroom.pages.SpacingPage
import com.halo.feature.showroom.pages.TypePage
import com.halo.feature.showroom.pages.components.ButtonPage
import com.halo.feature.showroom.pages.components.HoldToSendPage
import com.halo.feature.showroom.pages.components.LogoPage
import com.halo.feature.showroom.pages.components.SheetPage
import com.halo.feature.showroom.pages.components.RollingAmountPage
import com.halo.feature.showroom.pages.components.SegmentedPage
import com.halo.feature.showroom.pages.components.TogglePage

enum class ShowroomSection(val title: String) {
    Foundations("Foundations"),
    Components("Components"),
}

/**
 * A catalog entry. Adding a component page = one entry here (the halo-component skill does this).
 * [id] is the deep-link path segment: halo://showroom/<id>.
 */
class ShowroomPage(
    val id: String,
    val title: String,
    val summary: String,
    val section: ShowroomSection,
    val content: @Composable () -> Unit,
)

object ShowroomCatalog {
    val pages: List<ShowroomPage> = listOf(
        ShowroomPage("brand", "Brand", "One seed colour re-brands everything · plug-in proof", ShowroomSection.Foundations) { BrandPage() },
        ShowroomPage("color", "Color", "Material 3 roles from Halo seeds · contrast levels", ShowroomSection.Foundations) { ColorPage() },
        ShowroomPage("type", "Typography", "Material 3 type roles · Satoshi × Paper Mono", ShowroomSection.Foundations) { TypePage() },
        ShowroomPage("spacing", "Space & shape", "4dp grid, radii, strokes", ShowroomSection.Foundations) { SpacingPage() },
        ShowroomPage("depth", "Depth & light", "Surfaces, glass, glow, grain", ShowroomSection.Foundations) { DepthPage() },
        ShowroomPage("surface", "Surface lab", "Proof · clean glass on a matte base · light as information", ShowroomSection.Foundations) { SurfacePage() },
        ShowroomPage("motion", "Motion", "Spring tokens, curves, choreography", ShowroomSection.Foundations) { MotionPage() },
        ShowroomPage("haptics", "Haptics", "Haptics & sound lab · tiers per phone", ShowroomSection.Foundations) { HapticsPage() },
        ShowroomPage("icons", "Icons", "24dp grid, 1.75 stroke", ShowroomSection.Foundations) { IconsPage() },
        ShowroomPage("runtime", "Runtime", "Policy · clock · sensors · scene · feedback, live", ShowroomSection.Foundations) { RuntimePage() },
        // halo-component: component pages are registered below this line.
        ShowroomPage("button", "Button", "4 variants · press morph · loading collapse", ShowroomSection.Components) { ButtonPage() },
        ShowroomPage("rolling-amount", "Rolling amount", "Odometer digits · sats / BTC / fiat · privacy", ShowroomSection.Components) { RollingAmountPage() },
        ShowroomPage("hold-to-send", "Hold to send", "Charge · lock · check · spark burst", ShowroomSection.Components) { HoldToSendPage() },
        ShowroomPage("toggle", "Toggle", "Ceramic / graphite switch · the theme switch", ShowroomSection.Components) { TogglePage() },
        ShowroomPage("segmented", "Segmented control", "Thumb in a sunken track · detents · throw and land", ShowroomSection.Components) { SegmentedPage() },
        ShowroomPage("sheet", "Sheet", "Tray that is thrown, carried and lands · morphs between steps", ShowroomSection.Components) { SheetPage() },
        ShowroomPage("logo", "Logo", "Glass halo · radium liquid · real fluid physics", ShowroomSection.Components) { LogoPage() },
    )

    fun find(id: String): ShowroomPage? = pages.firstOrNull { it.id == id }
}
