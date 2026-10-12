package com.halo.core.designsystem.component

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.halo.core.designsystem.foundation.HaloPreview
import com.halo.core.designsystem.foundation.HaloPressable
import com.halo.core.designsystem.foundation.material.SDF_COMMON
import com.halo.core.designsystem.foundation.material.drawCanvasHalo
import com.halo.core.designsystem.foundation.rememberPressProgress
import com.halo.core.designsystem.runtime.HaloPlatform
import com.halo.core.designsystem.runtime.HaloSensor
import com.halo.core.designsystem.runtime.rememberAmbient
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import kotlin.math.abs
import kotlin.math.exp

enum class HaloLogoSize { Medium, Large }

/**
 * The Halo mark: a glass tube bent into a ring, filled with a self-luminous, radium-like liquid that circulates
 * continuously, with real fluid behaviour:
 * - laminar pipe flow: fastest in the core, still at the walls (no-slip), the fast core pushed towards the outer
 *   wall of the bend (Dean flow), shown by phosphor grains riding their streamlines and scintillating at random;
 * - momentum and viscosity: a gentle steady drive; a tap adds momentum that viscous drag bleeds back to the drive;
 * - inertia: turn the phone and the liquid lags behind the glass, then catches up (rotating-container physics).
 *
 * Lit by the scene light, with a light-caused contact shadow and a caustic. It runs only while it is on screen,
 * the app is in the foreground and battery saver is off, at ~30 fps when calm. Reduced motion: a still, glowing tube.
 *
 * Spec: docs/components/HaloLogo.md · Archetype: Feedback (brand mark) · Showroom: halo://showroom/logo
 *
 * @param animated runs the liquid and makes the mark pressable. False draws a still, glowing mark.
 * @param surfaceColor the surface the mark rests on; it is what you see through the glass.
 */
@Composable
fun HaloLogo(
    modifier: Modifier = Modifier,
    size: HaloLogoSize = HaloLogoSize.Large,
    animated: Boolean = true,
    surfaceColor: Color = HaloTheme.colors.bg.raised,
    contentDescription: String? = "Halo",
    interactionSource: MutableInteractionSource? = null,
) {
    val motion = HaloTheme.motion
    val motionNow by rememberUpdatedState(motion)
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val flow = remember { FluidFlow() }
    val live = animated && !motion.reduced
    val glow = remember { Animatable(if (live) 0f else 1f) }
    val press = rememberPressProgress(source)

    // The liquid's clock and gating come from the runtime: it steps only while the mark is visible, the app is in
    // the foreground and neither reduced motion, battery saver nor heat forbids it; calm flow runs at ~30 fps.
    val runtime = HaloTheme.runtime
    val ambient = rememberAmbient(runtime, enabled = live) { dt ->
        flow.step(dt)
        abs(flow.rate - DRIVE) >= CALM_RATE || glow.isRunning || press.value != 0f
    }
    val running = ambient.running

    // Appear: the liquid lights up and the pump kicks it round once.
    LaunchedEffect(live) {
        if (!live) {
            glow.snapTo(1f)
            return@LaunchedEffect
        }
        flow.push(TAP_IMPULSE)
        glow.animateTo(1f, motionNow.spring(HaloSpring.Gentle))
    }
    // Gyroscope (shared, from the runtime): the glass turns with the phone, the liquid's inertia makes it lag.
    DisposableEffect(running) {
        // z is counter-clockwise about the screen normal; the ring's angles run clockwise.
        val sub = if (running) {
            runtime.sensors.subscribe(HaloSensor.Gyroscope, onStop = { flow.glassSpin = 0f }) { values, _ -> flow.glassSpin = -values[2] }
        } else {
            null
        }
        onDispose { sub?.close() }
    }

    val glass: @Composable () -> Unit = {
        LogoGlass(
            size = size,
            surfaceColor = surfaceColor,
            flowPhase = { flow.phase },
            flowRate = { flow.rate },
            time = { flow.time },
            glow = { glow.value * (1f + PRESS_IGNITE * press.value) },
            modifier = ambient.visibility,
        )
    }
    if (animated) {
        // The touch response is the mark's own light (D5), so no highlight overlay.
        HaloPressable(
            onClick = { if (live) flow.push(TAP_IMPULSE) },
            shape = CircleShape,
            onClickLabel = "Push the liquid",
            haptic = HaloHaptic.Ping,
            interactionSource = source,
            indication = null,
            modifier = modifier
                .sizeIn(minWidth = HaloTheme.sizes.minTouch, minHeight = HaloTheme.sizes.minTouch)
                .semantics { contentDescription?.let { this.contentDescription = it } },
        ) { glass() }
    } else {
        Box(
            modifier
                .sizeIn(minWidth = HaloTheme.sizes.minTouch, minHeight = HaloTheme.sizes.minTouch)
                .semantics {
                    role = Role.Image
                    contentDescription?.let { this.contentDescription = it }
                },
            contentAlignment = Alignment.Center,
        ) { glass() }
    }
}

/**
 * The mark frozen at one moment: for state matrices, previews and static screenshots. Decorative (no semantics).
 *
 * @param flowRate circulation in degrees per second (sign = direction); shapes the Dean shift of the flow.
 * @param glow 0 = unlit liquid, 1 = glowing, up to 1.6 pressed.
 */
@Composable
fun HaloLogoFrame(
    flowRate: Float,
    glow: Float,
    modifier: Modifier = Modifier,
    size: HaloLogoSize = HaloLogoSize.Large,
    surfaceColor: Color = HaloTheme.colors.bg.raised,
) {
    LogoGlass(
        size = size,
        surfaceColor = surfaceColor,
        flowPhase = { FRAME_PHASE },
        flowRate = { flowRate * DEG },
        time = { FRAME_TIME },
        glow = { glow },
        modifier = modifier,
    )
}

/**
 * The liquid's motion, in the lab frame. The fluid has its own angular velocity [absolute]; viscous drag couples it
 * to the glass (spinning at [glassSpin] with the phone) plus a gentle pump [DRIVE], relaxing with time constant
 * [VISCOUS_S]. What you see is the motion relative to the glass. Draw-phase state only: never recomposes.
 */
@Stable
internal class FluidFlow {
    /** Mean flow travelled relative to the glass (rad). */
    var phase by mutableFloatStateOf(0f)
        private set
    /** Angular speed relative to the glass (rad/s, clockwise +). */
    var rate by mutableFloatStateOf(0f)
        private set
    /** Scintillation clock (s). */
    var time by mutableFloatStateOf(0f)
        private set
    var glassSpin = 0f
    private var absolute = 0f

    fun push(impulse: Float) {
        absolute += impulse
    }

    fun step(dt: Float) {
        val target = glassSpin + DRIVE
        absolute += (target - absolute) * (1f - exp(-dt / VISCOUS_S))
        rate = absolute - glassSpin
        phase += rate * dt
        time += dt
    }
}

@Composable
private fun LogoGlass(
    size: HaloLogoSize,
    surfaceColor: Color,
    flowPhase: () -> Float,
    flowRate: () -> Float,
    time: () -> Float,
    glow: () -> Float,
    modifier: Modifier = Modifier,
) {
    val light = HaloTheme.light
    val glass = HaloTheme.materials.glass
    val emission = HaloTheme.colors.accent.fill
    val hdr = HaloTheme.hdr
    // Android 13+: the full glass shader. Older phones draw the same ring on the canvas (see drawCanvasHalo).
    val shader = remember { if (HaloPlatform.shaders) RuntimeShader(HALO_GLASS_AGSL) else null }
    val brush = remember(shader) {
        if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ShaderBrush(shader) else null
    }
    Spacer(
        modifier
            .size(logoBox(size))
            .drawBehind {
                val w = this.size.minDimension
                val tube = w * TUBE_RADIUS
                if (shader == null || brush == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    drawCanvasHalo(w * RING_RADIUS, tube, light, glass, surfaceColor, emission, flowPhase(), glow())
                    return@drawBehind
                }
                with(shader) {
                    setFloatUniform("center", this@drawBehind.size.width / 2f, this@drawBehind.size.height / 2f)
                    setFloatUniform("ringRadius", w * RING_RADIUS)
                    setFloatUniform("tubeRadius", tube)
                    setFloatUniform("light", light.x, light.y, light.z)
                    setFloatUniform("shadowShift", light.shadowDx, light.shadowDy)
                    setFloatUniform("flowPhase", flowPhase())
                    setFloatUniform("flowRate", flowRate())
                    setFloatUniform("time", time())
                    setFloatUniform("glow", glow())
                    setFloatUniform("hdr", hdr.emission)
                    setColorUniform("surfaceColor", surfaceColor.toArgb())
                    setColorUniform("glassTint", glass.tint.toArgb())
                    setColorUniform("environment", glass.environment.toArgb())
                    setColorUniform("emission", emission.toArgb())
                    setColorUniform("highlightColor", light.highlight.toArgb())
                    setColorUniform("shadowColor", light.shadow.toArgb())
                    setFloatUniform("absorption", glass.absorption)
                    setFloatUniform("reflectance", glass.reflectance)
                    setFloatUniform("specular", light.specular * glass.specular)
                    setFloatUniform("shininess", glass.shininess)
                    setFloatUniform("shadowOpacity", light.shadowOpacity)
                }
                // Shadow and spilled light reach past the box: draw over a margin so they are never clipped.
                val margin = tube * DRAW_MARGIN
                drawRect(
                    brush = brush,
                    topLeft = Offset(-margin, -margin),
                    size = Size(this.size.width + margin * 2f, this.size.height + margin * 2f),
                )
            },
    )
}

@Composable
private fun logoBox(size: HaloLogoSize): Dp = when (size) {
    HaloLogoSize.Medium -> HaloTheme.sizes.logoMd
    HaloLogoSize.Large -> HaloTheme.sizes.logoLg
}

// ------------------------------------------------------------------------------------------- geometry & physics

/** Centre-line radius of the ring, in box widths. */
private const val RING_RADIUS = 0.36f
/** Outer radius of the glass tube, in box widths. */
private const val TUBE_RADIUS = 0.058f
/** How far past the box the shadow and spilled light reach, in tube radii. */
private const val DRAW_MARGIN = 2.5f
/** Pressing makes the liquid glow up to this much brighter (card reference: +60%). */
private const val PRESS_IGNITE = 0.6f
/** Steady circulation from the pump (rad/s): one lap in ~18 s, calm. */
internal const val DRIVE = 0.35f
/** Viscous relaxation time (s): how long the liquid takes to follow the glass and the pump. */
internal const val VISCOUS_S = 1.4f
/** Angular momentum a tap adds (rad/s): about one extra lap before drag bleeds it off. */
internal const val TAP_IMPULSE = 6f
/** Below this difference from the drive the flow counts as calm (the clock then steps it at ~30 fps). */
private const val CALM_RATE = 0.05f
/** Fixed moment for [HaloLogoFrame]. */
private const val FRAME_PHASE = 0.6f
private const val FRAME_TIME = 0.37f
/** Radians per degree. */
private const val DEG = 0.017453292f

/*
 * The glass halo, one pass. Two parts:
 *
 *   floor  — what the ring does to the surface it rests on: a contact shadow cast by the key light from the height of
 *            the tube's centre line (glass lets about half the light through, focused into a caustic line), contact
 *            occlusion where it touches, and the liquid's light spilling out all round.
 *   tube   — a glass rod with a bore. The glass wall shows the floor through a cylindrical lens (flipped, magnified),
 *            absorbed towards the glass tint by thickness (Beer–Lambert), and glows where it pipes the liquid's
 *            light (strongest along the walls: total internal reflection). The bore is full of a self-luminous,
 *            radium-like liquid in laminar flow: a soft glow (denser where thicker) carried at the mean flow, and
 *            phosphor grains riding their streamlines — Poiseuille profile, no-slip at the walls, its peak pushed
 *            towards the outer wall of the bend (Dean flow) — each scintillating at random. On top, Fresnel-weighted
 *            reflections of the room and the key light as a tight highlight.
 *
 * Coordinates are local px, y down. Angles in radians, clockwise from 3 o'clock.
 */
private const val HALO_GLASS_AGSL = """
uniform float2 center;
uniform float ringRadius;
uniform float tubeRadius;
uniform float3 light;
uniform float2 shadowShift;
uniform float flowPhase;
uniform float flowRate;
uniform float time;
uniform float glow;
// HDR: gain for emitted light (1 on SDR displays). Only the liquid's light is boosted, never the glass or shadow.
uniform float hdr;
layout(color) uniform half4 surfaceColor;
layout(color) uniform half4 glassTint;
layout(color) uniform half4 environment;
layout(color) uniform half4 emission;
layout(color) uniform half4 highlightColor;
layout(color) uniform half4 shadowColor;
uniform float absorption;
uniform float reflectance;
uniform float specular;
uniform float shininess;
uniform float shadowOpacity;
""" + SDF_COMMON + """
const float TAU = 6.28318531;
// Inner radius of the bore, as a fraction of the tube: the rest is the glass wall.
const float BORE = 0.78;
// Streamlines across the bore, and grains per streamline (an integer, so the ring closes without a seam).
const float LANES = 7.0;
const float GRAINS = 16.0;

float noise2(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 w = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1.0, 0.0)), w.x),
               mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), w.x), w.y);
}

// Laminar speed across the bore (ub −1 inner wall … +1 outer wall), relative to the mean flow: zero at both walls,
// fastest in the core, the peak pushed outwards by the bend (Dean flow), more so the faster it circulates.
// A parabola averages 2/3 of its peak, so ×1.5 keeps the mean.
float streamline(float ub) {
    float d = 0.12 + min(abs(flowRate) * 0.03, 0.2);
    float t = ub >= d ? (ub - d) / (1.0 - d) : (ub - d) / (1.0 + d);
    return 1.5 * (1.0 - t * t);
}

// Phosphor grains on one layer of streamlines: a soft dot per grain, scintillating. Each grain stays on its
// streamline (laminar: no lateral mixing) and moves at that streamline's speed.
float grains(float phi, float ub, float layer) {
    float laneW = 2.0 / LANES;
    float li = floor((ub + 1.0) / laneW + layer * 0.5);
    float lc = (li + 0.5 - layer * 0.5) * laneW - 1.0;
    if (abs(lc) > 0.92) return 0.0;
    float circ = TAU * ringRadius;
    float spacing = circ / GRAINS;
    float x = mod((phi - flowPhase * streamline(lc)) * ringRadius, circ);
    float cell = floor(x / spacing);
    float id = hash(float2(li + layer * 31.0, cell));
    float gx = (cell + 0.2 + 0.6 * hash(float2(cell, li + 7.0 + layer * 13.0))) * spacing;
    float gy = (lc + (hash(float2(li, cell + 3.0 + layer)) - 0.5) * laneW * 0.6) * BORE * tubeRadius;
    float r = 0.55 + 0.45 * hash(float2(id, 5.0));
    float spot = 1.0 - smoothstep(r * 0.5, r + 0.6, length(float2(x - gx, ub * BORE * tubeRadius - gy)));
    // Scintillation (as in radium paint): each grain flashes at a random moment about 1.5 times a second, rising
    // at once and fading in ~80 ms; barely visible between flashes, so the glow stays soft.
    float tt = time * 1.5 + id * 13.0;
    float since = (fract(tt) - hash(float2(floor(tt), id * 91.0))) / 1.5;
    float flash = since >= 0.0 ? exp(-since / 0.08) : 0.0;
    return spot * (0.06 + 0.94 * flash);
}

// Fresnel reflectance (Schlick) across the tube at u.
float fresnel(float u) {
    float nz = sqrt(max(1.0 - u * u, 0.0));
    return reflectance + (1.0 - reflectance) * pow(1.0 - nz, 5.0);
}

// The liquid's light spilled onto the floor at f (tint strength, as in floorAt).
float spillLight(float2 f) {
    float cd = abs(length(f - center) - ringRadius);
    float k = cd / (tubeRadius * 1.6);
    return min(glow * exp(-k * k) * 0.32, 0.6);
}

// What the ring does to the floor at f, premultiplied: shadow, caustic, and the liquid's light as a tint (its
// alpha is its own strength, so it shows on ceramic as well as on black).
float4 floorAt(float2 f) {
    float2 g = f - center;
    float cd = abs(length(g) - ringRadius);
    float sd = abs(length(g - shadowShift * tubeRadius) - ringRadius);
    float shadow = (1.0 - smoothstep(tubeRadius * 0.35, tubeRadius * 1.6, sd)) * shadowOpacity * 0.5;
    float caustic = exp(-sd * sd / (tubeRadius * tubeRadius * 0.05));
    float contact = exp(-cd / (tubeRadius * 0.45)) * shadowOpacity * 0.35;
    // Gaussian falloff: the light hugs the tube.
    float k = cd / (tubeRadius * 1.6);
    float spill = min(glow * exp(-k * k) * 0.32, 0.6);
    float dark = max(shadow * (1.0 - 0.8 * caustic), contact) * (1.0 - spill);
    float3 rgb = shadowColor.rgb * dark + highlightColor.rgb * caustic * shadowOpacity * 0.18 + emission.rgb * spill;
    return float4(rgb, min(dark + spill, 1.0));
}

half4 main(float2 p) {
    float2 q = p - center;
    float rho = max(length(q), 0.0001);
    float2 radial = q / rho;
    float s = rho - ringRadius;

    float4 fx = floorAt(p);
    half4 floorOut = half4(fx);
    // The spill is emitted light: on an HDR display it may be brighter than white.
    floorOut.rgb += half3(emission.rgb * spillLight(p) * (hdr - 1.0));
    float cover = clamp(tubeRadius - abs(s) + 0.5, 0.0, 1.0);
    if (cover <= 0.0) return floorOut;

    float3 lumen = float3(0.0);
    float u = clamp(s / tubeRadius, -1.0, 1.0);
    float nz = sqrt(max(1.0 - u * u, 0.0));
    float3 n = float3(radial * u, nz);

    // The glass: the floor under it through a cylindrical lens, absorbed by thickness, glowing with piped light.
    float4 under = floorAt(center + radial * (ringRadius - u * tubeRadius * 0.6));
    float3 floorC = surfaceColor.rgb * (1.0 - under.a) + under.rgb;
    float3 c = mix(glassTint.rgb, floorC, exp(-absorption * 2.0 * nz));
    float wall = 0.2 + 0.8 * (u * u) * (u * u);
    c = mix(c, emission.rgb, clamp(glow * wall * 0.5, 0.0, 0.85));

    // The liquid filling the bore.
    float inBore = clamp((BORE - abs(u)) * tubeRadius + 0.5, 0.0, 1.0);
    if (inBore > 0.0) {
        float phi = atan(q.y, q.x);
        float ub = clamp(u / BORE, -1.0, 1.0);
        float nb = sqrt(max(1.0 - ub * ub, 0.0));
        // Soft glow carried at the mean flow (noise sampled round a circle, so the ring has no seam).
        float theta = phi - flowPhase;
        float2 around = float2(cos(theta), sin(theta));
        float rr = ringRadius / tubeRadius * 0.6 + ub * 1.2;
        float cloud = noise2(around * rr + 11.0) * 0.6 + noise2(around * rr * 2.1 + 3.0) * 0.4;
        float bright = dot(surfaceColor.rgb, float3(0.299, 0.587, 0.114));
        float3 radium = emission.rgb * glow * (0.8 + 0.2 * nb) * (0.85 + 0.3 * cloud);
        // Densest at the core: it burns towards white on dark surfaces and stays saturated on bright ones.
        radium = mix(radium, highlightColor.rgb, clamp(nb * nb * glow * mix(0.3, 0.05, bright), 0.0, 1.0));
        float g = grains(phi, ub, 0.0) + grains(phi, ub, 1.0);
        radium += mix(highlightColor.rgb, emission.rgb, 0.35) * g * glow;
        // Unlit, it still reads as liquid: the emission colour, dimmed by the glass.
        float3 liquid = mix(mix(c, emission.rgb * 0.45, 0.7), radium, clamp(glow, 0.0, 1.0));
        // Thin at the walls of the bore, so it is partly transparent there.
        float weight = inBore * (1.0 - exp(-4.0 * nb));
        c = mix(c, liquid, weight);
        lumen = radium * weight * clamp(glow, 0.0, 1.0);
    }

    // Reflections: the room (bright towards the key light, dim away from it) and a soft studio window, weighted by
    // Fresnel; the key light itself as a tight highlight. Fresnel climbs from ~10% to 100% inside the last pixel, so
    // it is averaged across the pixel near the edges (otherwise the rim stipples).
    float3 r = float3(2.0 * nz * n.xy, 2.0 * nz * nz - 1.0);
    float facing = dot(r, light);
    float px = 1.0 / tubeRadius;
    float fres = abs(u) > 0.8
        ? (fresnel(clamp(u - px * 0.33, -1.0, 1.0)) + fresnel(u) + fresnel(clamp(u + px * 0.33, -1.0, 1.0))) / 3.0
        : fresnel(u);
    float3 env = environment.rgb * (0.35 + 0.65 * smoothstep(-0.5, 0.9, facing))
        + highlightColor.rgb * pow(max(facing, 0.0), 6.0) * 0.45;
    float spec = pow(max(facing, 0.0), shininess) * specular;
    c = c * (1.0 - fres) + env * fres + highlightColor.rgb * spec;
    // Standard range is soft-clipped as before; on HDR the liquid's own light rises above white on top of it.
    float3 outC = softClip(c) + lumen * (hdr - 1.0) * (1.0 - fres);
    return mix(floorOut, half4(half3(outC), 1.0), half(cover));
}
"""

// ------------------------------------------------------------------------------------------- previews

@Preview
@Composable
private fun HaloLogoRestPreview() = HaloPreview {
    Row(horizontalArrangement = Arrangement.spacedBy(HaloTheme.spacing.lg), verticalAlignment = Alignment.CenterVertically) {
        HaloLogo(animated = false)
        HaloLogo(animated = false, size = HaloLogoSize.Medium)
    }
}

@Preview
@Composable
private fun HaloLogoFlowPreview() = HaloPreview {
    Row(horizontalArrangement = Arrangement.spacedBy(HaloTheme.spacing.lg)) {
        HaloLogoFrame(flowRate = 0f, glow = 0f)
        HaloLogoFrame(flowRate = 20f, glow = 1f)
        HaloLogoFrame(flowRate = 340f, glow = 1f)
        HaloLogoFrame(flowRate = 20f, glow = 1.6f)
    }
}
