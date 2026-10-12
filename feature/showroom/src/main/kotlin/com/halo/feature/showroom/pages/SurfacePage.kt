package com.halo.feature.showroom.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import com.halo.core.designsystem.foundation.rememberHaloBackdrop
import com.halo.core.designsystem.foundation.backdropSource
import com.halo.core.designsystem.foundation.backdropGlass
import com.halo.core.designsystem.foundation.HaloBackdrop
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.halo.core.designsystem.component.HaloRollingAmount
import com.halo.core.designsystem.component.HaloSegment
import com.halo.core.designsystem.component.HaloSegmentedControl
import com.halo.core.designsystem.foundation.AmountUnit
import com.halo.core.designsystem.foundation.HaloPressable
import com.halo.core.designsystem.foundation.HaloText
import com.halo.core.designsystem.foundation.enterRise
import com.halo.core.designsystem.foundation.material.ReliefMaterial
import com.halo.core.designsystem.foundation.material.ReliefPainter
import com.halo.core.designsystem.host.HaloThemeMode
import com.halo.core.designsystem.host.LocalHaloAppearance
import com.halo.core.designsystem.icons.HaloIcon
import com.halo.core.designsystem.icons.HaloIcons
import com.halo.core.designsystem.runtime.rememberAmbient
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.ui.Knob
import com.halo.feature.showroom.ui.SectionLabel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Proof for the surface direction (2026-10-12): clean glass on a matte base, with information as emitted light.
 * - Base: flat soft-touch or ceramic under a pool of room light (no relief).
 * - Panes: thin clean glass (frost, a whisper of tint, a lit edge line, a short contact shadow). See GlassLab.kt.
 * - Controls: solid glass (the tab thumb, the keys): thick, a caustic beneath, their state as light inside.
 * - Information: emitted light (the chart filament); its pulse is a separate moving layer.
 * Not design-system code: a lab to judge the direction before any tokens.
 */
@Composable
internal fun SurfacePage() {
    var finish by rememberSaveable { mutableStateOf(Finish.SoftTouch) }
    val s = HaloTheme.spacing
    val t = HaloTheme.typography
    val colors = HaloTheme.colors
    val scheme = rememberM3()
    val type = rememberM3Type()
    // The base is Material's surface role, with Halo's grain.
    val ground = finishMaterial(finish, scheme.surface)
    val tune = remember { GlassTuning() }
    val appearance = checkNotNull(LocalHaloAppearance.current)
    CompositionLocalProvider(LocalGlassTuning provides tune, LocalM3 provides scheme, LocalM3Type provides type) {
    
    // Lab controls (flat, they are the lab, not the proof).
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.enterRise(0)) {
        TonalTabs(
            tabs = Finish.entries.map { GlassTab(it.label) },
            selected = finish.ordinal,
            onSelect = { finish = Finish.entries[it] },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(s.md))
        TonalSwitch(
            checked = colors.isDark,
            onCheckedChange = { appearance.setMode(if (it) HaloThemeMode.Dark else HaloThemeMode.Light) },
            contentDescription = "Dark mode",
        )
    }
    Spacer(Modifier.height(s.lg))
    TuningKnobs(tune)
    Spacer(Modifier.height(s.xl))

    // ---- The proof: smoked glass on a matte base, full bleed. The base is the glass's backdrop.
    val backdrop = rememberLabBackdrop()
    Box(Modifier.bleed(s.gutter)) {
        // The content layer is what the floating glass refracts.
        Box(Modifier.labSource(backdrop)) {
            Spacer(Modifier.matchParentSize().ground(ground, finish))
        ProofContent()
        }
        // Navigation: Material 3 Expressive's flexible navigation bar, docked; content scrolls beneath it.
        var nav by rememberSaveable { mutableStateOf(0) }
        M3NavBar(
            backdrop = backdrop,
            items = listOf(NavItem("Home", HaloIcons.Grid), NavItem("Activity", HaloIcons.Clock), NavItem("Scan", HaloIcons.Scan), NavItem("Settings", HaloIcons.Settings)),
            selected = nav,
            onSelect = { nav = it },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        )
    }
    }
}

/** Live glass tuning: each knob scales one optical property; the panes and controls redraw, nothing recomposes. */
@Composable
private fun TuningKnobs(tune: GlassTuning) {
    val s = HaloTheme.spacing
    val steps = listOf(0.5f, 0.75f, 1f, 1.5f, 2f)
    fun label(v: Float) = if (v == 1f) "1×" else "$v"
    Column(verticalArrangement = Arrangement.spacedBy(s.md)) {
        Knob("Frost", steps, tune.frost, { tune.frost = it }, optionLabel = ::label)
        Knob("Tint", steps, tune.tint, { tune.tint = it }, optionLabel = ::label)
        Knob("Rim", steps, tune.rim, { tune.rim = it }, optionLabel = ::label)
        Knob("Inner light", steps, tune.inner, { tune.inner = it }, optionLabel = ::label)
        Knob("Caustic", listOf(0f, 0.5f, 1f, 1.5f, 2f), tune.caustic, { tune.caustic = it }, optionLabel = ::label)
    }
}

@Composable
private fun ProofContent() {
    val s = HaloTheme.spacing
    val t = HaloTheme.typography
    val colors = HaloTheme.colors
    Column(
        Modifier.padding(start = s.gutter, end = s.gutter, top = s.xxl, bottom = s.xxl),
        verticalArrangement = Arrangement.spacedBy(s.lg),
    ) {
        // Balance + chart: the hero container, the highest tone. The data is light on it.
        Column(
            Modifier
                .fillMaxWidth()
                .enterRise(1)
                .tonal(Tone.High, HaloTheme.radius.xl),
        ) {
            Column(Modifier.padding(start = s.xl, end = s.xl, top = s.xl)) {
                var unit by rememberSaveable { mutableStateOf(0) }
                TonalTabs(
                    tabs = listOf(GlassTab("sats"), GlassTab("BTC"), GlassTab("USD")),
                    selected = unit,
                    onSelect = { unit = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(s.xl))
                HaloText("Balance", style = m3type.titleMedium, color = m3.onSurfaceVariant)
                Spacer(Modifier.height(s.sm))
                HaloRollingAmount(sats = 2_418_550L, unit = AmountUnit.entries[unit], fiatPerBtc = 72_034.0)
                Spacer(Modifier.height(s.sm))
                HaloText("≈ 1,742.18 USD", style = t.data, color = m3.onSurfaceVariant)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(HaloTheme.sizes.controlXl * 2 + s.x3),
            ) {
                ChartLight(Modifier.fillMaxSize().padding(start = s.xl, end = s.xl, top = s.xl, bottom = s.xxl))
            }
        }

        // Actions: one key action is larger and coloured (Material 3 Expressive emphasis), the others tonal.
        Row(
            Modifier
                .fillMaxWidth()
                .enterRise(2)
                .tonal(Tone.Low, HaloTheme.radius.xl)
                .padding(vertical = s.lg),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            TonalKey("Receive", HaloIcons.Receive)
            TonalKey("Send", HaloIcons.Send, primary = true)
            TonalKey("Scan", HaloIcons.Scan)
        }

        // Network: here colour carries meaning (orange on-chain, yellow Lightning).
        var network by rememberSaveable { mutableStateOf(0) }
        TonalTabs(
            tabs = listOf(GlassTab("On-chain") { HaloTheme.colors.bitcoin }, GlassTab("Lightning") { HaloTheme.colors.lightning }),
            selected = network,
            onSelect = { network = it },
            modifier = Modifier.fillMaxWidth().enterRise(3),
            families = listOf(m3.bitcoin, m3.lightning),
        )

        // Settings: switches on a tonal container.
        var hide by rememberSaveable { mutableStateOf(false) }
        var ln by rememberSaveable { mutableStateOf(true) }
        Column(
            Modifier
                .fillMaxWidth()
                .enterRise(3)
                .tonal(Tone.Low, HaloTheme.radius.xl)
                .padding(horizontal = s.xl, vertical = s.xs),
        ) {
            ToggleRow("Hide balance", hide, { hide = it }, m3.primary)
            Hairline()
            ToggleRow("Lightning payments", ln, { ln = it }, m3.lightning)
        }

        // Activity: a tonal container; the floating glass bar passes over its last rows.
        Column(Modifier.enterRise(3)) {
            HaloText("Activity", style = m3type.titleMedium, color = m3.onSurface, modifier = Modifier.padding(bottom = s.md))
            Column(
                Modifier
                    .fillMaxWidth()
                    .tonal(Tone.Low, HaloTheme.radius.xl)
                    .padding(horizontal = s.xl, vertical = s.xs),
            ) {
                ActivityRow("Received", "Today · 09:41", "+ 120,000", m3.positive.color)
                Hairline()
                ActivityRow("Coffee · Lightning", "Yesterday", "− 4,200", m3.lightning.color)
                Hairline()
                ActivityRow("Sent to cold storage", "Mon", "− 500,000", m3.bitcoin.color)
                Hairline()
                ActivityRow("Received · Lightning", "Sun", "+ 21,000", m3.positive.color)
            }
        }
        Spacer(Modifier.height(HaloTheme.sizes.controlXl))
    }
}

/** The floating layer: the only glass. A pill that doesn't span the screen, its sides showing the content. */
@Composable
private fun FloatingBar(backdrop: LabBackdrop, modifier: Modifier) {
    val s = HaloTheme.spacing
    var tab by rememberSaveable { mutableStateOf(0) }
    val items = listOf(HaloIcons.Grid to "Home", HaloIcons.Clock to "Activity", HaloIcons.Scan to "Scan", HaloIcons.Settings to "Settings")
    Row(
        modifier
            .floatingGlass(backdrop, HaloTheme.sizes.controlLg / 2 + s.xs)
            .padding(horizontal = s.sm, vertical = s.xs),
        horizontalArrangement = Arrangement.spacedBy(s.xs),
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val selected = i == tab
            HaloPressable(
                onClick = { tab = i },
                shape = CircleShape,
                onClickLabel = label,
                modifier = Modifier.size(HaloTheme.sizes.controlLg),
            ) {
                HaloIcon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.wrapContentSize(),
                    size = HaloTheme.sizes.iconLg,
                    tint = if (selected) HaloTheme.colors.accent.text else HaloTheme.colors.content.primary,
                )
            }
        }
    }
}

private enum class Finish(val label: String) { SoftTouch("Soft-touch"), Ceramic("Ceramic") }

/** The two candidate finishes on the theme's tones: rubbery matte with fine grain, or a smooth glaze with a tight highlight. */
@Composable
private fun finishMaterial(finish: Finish, base: Color): ReliefMaterial {
    val dark = HaloTheme.colors.isDark
    return remember(finish, base, dark) {
        when (finish) {
            Finish.SoftTouch -> ReliefMaterial(base, specular = 0.22f, shininess = 10f, grain = if (dark) 0.035f else 0.022f, ambientOcclusion = 0.45f)
            Finish.Ceramic -> ReliefMaterial(base, specular = if (dark) 0.9f else 1.1f, shininess = 56f, grain = 0.006f, ambientOcclusion = 0.35f)
        }
    }
}

// ------------------------------------------------------------------------------------------- surfaces

/** Widens the element by [by] on both sides (to reach the screen edges past the page gutter). */
private fun Modifier.bleed(by: Dp) = layout { measurable, constraints ->
    val px = by.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + px * 2, maxWidth = constraints.maxWidth + px * 2),
    )
    layout(placeable.width - px * 2, placeable.height) { placeable.place(-px, 0) }
}

/**
 * The matte base: a flat face of the material (lit grain, no relief) under a large, soft pool of room light from the
 * key light's side. Soft-touch scatters it broadly; ceramic's glaze adds a tighter sheen inside it.
 */
@Composable
private fun Modifier.ground(material: ReliefMaterial, finish: Finish): Modifier {
    val painter = remember { ReliefPainter() }
    val light = HaloTheme.light
    val colors = HaloTheme.colors
    val pool = if (colors.isDark) POOL_DARK else POOL_LIGHT
    val bloom = colors.accent.glow
    val canvas = colors.bg.canvas
    val d = LocalDensity.current
    val bloomY = with(d) { (HaloTheme.sizes.controlXl * 2).toPx() }
    val bloomRadius = with(d) { (HaloTheme.sizes.controlXl * 3).toPx() }
    val fade = with(d) { HaloTheme.spacing.x5.toPx() }
    return drawBehind {
        painter.drawRelief(this, Rect(Offset.Zero, size), 0f, MIN_EDGE, 0f, material, light, seed = GROUND_SEED)
        val dir = towardLight(light.x, light.y)
        val source = Offset(size.width / 2f, 0f) + Offset(dir.x * size.width, dir.y * size.width * POOL_LIFT)
        val broad = size.width * POOL_REACH
        drawRect(Brush.radialGradient(listOf(light.highlight.copy(alpha = pool), Color.Transparent), source, broad))
        if (finish == Finish.Ceramic) {
            drawRect(Brush.radialGradient(listOf(light.highlight.copy(alpha = pool * GLAZE), Color.Transparent), source, broad * GLAZE_REACH))
        }
        // The halo's own light, resting on the base behind the balance pane: what the smoked glass frosts.
        val bloomAt = Offset(size.width * BLOOM_X, bloomY)
        drawRect(Brush.radialGradient(listOf(bloom, Color.Transparent), bloomAt, bloomRadius))
        // Ease in from the page above (flat canvas) so the base has no seam.
        drawRect(Brush.verticalGradient(listOf(canvas, canvas.copy(alpha = 0f)), 0f, fade), size = Size(size.width, fade))
    }
}

/**
 * A thin pane of smoked glass resting on the base. Depth comes from crisp light, never from soft extrusion:
 * - a short, tight contact shadow (a 2–3 mm sheet sits close),
 * - a smoked body, a touch brighter on the side facing the light,
 * - a faint sheen entering from the lit edge,
 * - the pane's thickness as a dark inner line, and a 1 px Fresnel rim, bright towards the light, faint opposite.
 * Everything reads the world-fixed light in draw, so a tilt moves the sheen and rims without recomposing.
 */
@Composable
private fun Modifier.smokedGlass(radius: Dp, backdrop: HaloBackdrop): Modifier {
    val painter = remember { ReliefPainter() }
    val light = HaloTheme.light
    val colors = HaloTheme.colors
    val tint = colors.bg.glass
    val dark = colors.isDark
    val d = LocalDensity.current
    val r = with(d) { radius.toPx() }
    val rim = with(d) { HaloTheme.stroke.thin.toPx() }
    val elevation = with(d) { HaloTheme.spacing.xxs.toPx() }
    val blur = HaloTheme.effects.glassBlur
    val shape = RoundedCornerShape(radius)
    return drawBehind { painter.drawContactShadow(this, Rect(Offset.Zero, size), r, elevation, light, GLASS_SHADOW) }
        .backdropGlass(backdrop, shape, blur)
        .drawBehind {
        val body = Rect(Offset.Zero, size)
        val dir = towardLight(light.x, light.y)
        val reach = hypot(size.width, size.height) / 2f
        val lit = center + dir * reach
        val far = center - dir * reach
        val corner = CornerRadius(r)
        drawRoundRect(
            Brush.linearGradient(listOf(tint.copy(alpha = tint.alpha * SMOKE), tint.copy(alpha = tint.alpha * SMOKE * BODY_FAR)), lit, far),
            cornerRadius = corner,
        )
        drawRoundRect(
            Brush.linearGradient(listOf(light.highlight.copy(alpha = if (dark) SHEEN_DARK else SHEEN_LIGHT), Color.Transparent), lit, center),
            cornerRadius = corner,
        )
        val edge = body.deflate(rim * EDGE_INSET)
        drawRoundRect(
            light.shadow.copy(alpha = light.shadowOpacity * if (dark) EDGE_DARK else EDGE_LIGHT),
            edge.topLeft, edge.size, CornerRadius(r - rim * EDGE_INSET), style = Stroke(rim),
        )
        val outer = body.deflate(rim / 2f)
        drawRoundRect(
            Brush.linearGradient(
                listOf(
                    light.highlight.copy(alpha = if (dark) RIM_LIT_DARK else RIM_LIT_LIGHT),
                    light.highlight.copy(alpha = RIM_MID),
                    light.highlight.copy(alpha = RIM_FAR),
                ),
                lit, far,
            ),
            outer.topLeft, outer.size, CornerRadius(r - rim / 2f), style = Stroke(rim),
        )
    }
}

/** Unit vector toward the key light in screen space (top-left by default). */
private fun towardLight(x: Float, y: Float): Offset {
    val len = hypot(x, y)
    return if (len < MIN_LEAN) Offset(-0.7f, -0.7f) else Offset(x / len, y / len)
}

@Composable
private fun Hairline() {
    val color = m3.outlineVariant
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(HaloTheme.stroke.hairline)
            .drawBehind { drawRect(color) },
    )
}

// ------------------------------------------------------------------------------------------- content

/** A physical key set into the glass: a round relief knob that sinks under the finger, label below. */
@Composable
private fun ActionKey(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val source = remember { MutableInteractionSource() }
    val sink = remember { Animatable(0f) }
    val motion = HaloTheme.motion
    val painter = remember { ReliefPainter() }
    val light = HaloTheme.light
    val knob = HaloTheme.materials.knob
    val d = LocalDensity.current
    val elevation = with(d) { HaloTheme.spacing.xs.toPx() }
    LaunchedEffect(source) {
        source.interactions.collect {
            when (it) {
                is PressInteraction.Press -> launch { sink.animateTo(1f, motion.spring(HaloSpring.Snap)) }
                is PressInteraction.Release, is PressInteraction.Cancel -> launch { sink.animateTo(0f, motion.spring(HaloSpring.Quick)) }
            }
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HaloPressable(
            onClick = {},
            shape = CircleShape,
            interactionSource = source,
            onClickLabel = label,
            modifier = Modifier
                .size(HaloTheme.sizes.controlLg)
                .drawBehind {
                    val body = Rect(Offset.Zero, size)
                    val r = size.minDimension / 2f
                    painter.drawContactShadow(this, body, r, elevation * (1f - SINK_DEPTH * sink.value), light, KEY_SHADOW)
                    painter.drawRelief(this, body, r, r * KEY_BEVEL, r * KEY_RISE, knob, light, seed = KEY_SEED)
                },
        ) {
            HaloIcon(icon, contentDescription = null, modifier = Modifier.wrapContentSize(), size = HaloTheme.sizes.iconMd, tint = HaloTheme.colors.accent.text)
        }
        Spacer(Modifier.height(HaloTheme.spacing.sm))
        HaloText(label, style = HaloTheme.typography.bodyMedium, color = HaloTheme.colors.content.secondary)
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, family: M3Family) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = HaloTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HaloText(title, style = m3type.bodyLarge, color = m3.onSurface, modifier = Modifier.weight(1f))
        TonalSwitch(checked, onChange, family = family, contentDescription = title)
    }
}

@Composable
private fun ActivityRow(title: String, time: String, amount: String, amountColor: Color) {
    val t = HaloTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = HaloTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            HaloText(title, style = m3type.bodyLarge, color = m3.onSurface, maxLines = 1)
            Spacer(Modifier.height(HaloTheme.spacing.xxs))
            HaloText(time, style = t.data, color = m3.onSurfaceVariant)
        }
        HaloText(amount, style = t.amountSmall, color = amountColor)
    }
}

/**
 * The chart as emitted light, in two layers: the filament and its liquid fill (static, drawn once) and a light pulse
 * travelling along it (the moving layer, stepped by the runtime only while visible).
 */
@Composable
private fun ChartLight(modifier: Modifier) {
    val colors = HaloTheme.colors.bitcoin
    var phase by remember { mutableFloatStateOf(0f) }
    val ambient = rememberAmbient(HaloTheme.runtime, enabled = !HaloTheme.motion.reduced) { dt ->
        phase = (phase + dt / PULSE_PERIOD_S) % 1f
        true
    }
    val d = LocalDensity.current
    val core = with(d) { HaloTheme.stroke.indicator.toPx() }
    val glow = with(d) { HaloTheme.spacing.xs.toPx() }
    Box(modifier) {
        // Layer 1: filament + liquid fill. Records once per size.
        Spacer(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val line = chartPath(size.width, size.height)
                    val area = Path().apply {
                        addPath(line)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    val fill = Brush.verticalGradient(listOf(colors.glow, Color.Transparent))
                    onDrawBehind {
                        drawPath(area, fill)
                        // Emitted light falls off softly: three widening, fading passes instead of one hard halo.
                        for (i in GLOW_PASSES downTo 1) {
                            val width = glow * i
                            drawPath(line, colors.glow.copy(alpha = colors.glow.alpha * GLOW_ALPHA / i), style = Stroke(width, cap = StrokeCap.Round))
                        }
                        drawPath(line, colors.fill, style = Stroke(core, cap = StrokeCap.Round))
                    }
                },
        )
        // Layer 2: the moving light. Only this layer redraws per frame.
        Spacer(
            Modifier
                .fillMaxSize()
                .then(ambient.visibility)
                .drawWithCache {
                    val line = chartPath(size.width, size.height)
                    val measure = PathMeasure().apply { setPath(line, false) }
                    val segment = Path()
                    val length = measure.length
                    onDrawBehind {
                        val head = phase * length
                        val tail = length * PULSE_TAIL
                        for (i in 0 until PULSE_STEPS) {
                            val a = head - tail * (i + 1) / PULSE_STEPS
                            val b = head - tail * i / PULSE_STEPS
                            if (b <= 0f) break
                            segment.reset()
                            measure.getSegment(a.coerceAtLeast(0f), b, segment, true)
                            val strength = 1f - i.toFloat() / PULSE_STEPS
                            drawPath(segment, colors.fill.copy(alpha = strength), style = Stroke(core * 1.6f, cap = StrokeCap.Round))
                            drawPath(segment, colors.glow.copy(alpha = colors.glow.alpha * strength * GLOW_ALPHA), style = Stroke(glow * 2f, cap = StrokeCap.Round))
                        }
                    }
                },
        )
    }
}

/** A deterministic 30-day balance curve (steady growth with a dip), smoothed (Catmull–Rom) to fill [w] × [h]. */
private fun chartPath(w: Float, h: Float): Path {
    val n = CHART_POINTS
    val values = FloatArray(n) { i ->
        val x = i / (n - 1f)
        val dip = DIP_DEPTH * exp(-((x - DIP_AT) / DIP_WIDTH).let { it * it })
        x + WAVE * sin(x * PI.toFloat() * WAVES) - dip
    }
    val min = values.min()
    val max = values.max()
    val points = List(n) { i -> Offset(w * i / (n - 1f), h - (values[i] - min) / (max - min) * h) }
    return Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 0 until n - 1) {
            val p0 = points[(i - 1).coerceAtLeast(0)]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[(i + 2).coerceAtMost(n - 1)]
            cubicTo(
                p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
                p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
                p2.x, p2.y,
            )
        }
    }
}

private const val WAVE = 0.05f
private const val WAVES = 4f
private const val DIP_DEPTH = 0.18f
private const val DIP_AT = 0.55f
private const val DIP_WIDTH = 0.08f
private const val MIN_EDGE = 0.5f
private const val MIN_LEAN = 1e-3f
private const val POOL_DARK = 0.07f
private const val POOL_LIGHT = 0.55f
private const val POOL_LIFT = 0.6f
private const val POOL_REACH = 1.6f
private const val GLAZE = 0.9f
private const val GLAZE_REACH = 0.45f
private const val GLASS_SHADOW = 0.7f
private const val BODY_FAR = 0.8f
private const val SMOKE = 0.62f
private const val BLOOM_X = 0.78f
private const val SHEEN_DARK = 0.05f
private const val SHEEN_LIGHT = 0.35f
private const val EDGE_INSET = 2.5f
private const val EDGE_DARK = 0.5f
private const val EDGE_LIGHT = 0.25f
private const val RIM_LIT_DARK = 0.28f
private const val RIM_LIT_LIGHT = 0.95f
private const val RIM_MID = 0.05f
private const val RIM_FAR = 0.12f
private const val SINK_DEPTH = 0.65f
private const val KEY_SHADOW = 0.9f
private const val KEY_BEVEL = 0.35f
private const val KEY_RISE = 0.2f
private const val KEY_SEED = 13f
private const val GROUND_SEED = 2f
private const val CHART_POINTS = 30
private const val PULSE_PERIOD_S = 4.5f
private const val PULSE_TAIL = 0.18f
private const val PULSE_STEPS = 12
private const val GLOW_PASSES = 3
private const val GLOW_ALPHA = 0.6f
