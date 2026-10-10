package com.halo.core.designsystem.component

import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import com.halo.core.designsystem.foundation.rememberPressProgress
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloMotion
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

enum class HaloLogoSize { Medium, Large }

/** Where the liquid rests: centred on the splash satellite's spot, top right (degrees, clockwise from 3 o'clock). */
const val HALO_LOGO_REST_ANGLE = -45f

/**
 * The Halo mark: a glass tube bent into a ring, a quarter filled with a self-luminous, radium-like liquid. When
 * it appears, the liquid lights up and flows once round the ring, leaving a wet film that fades, and settles back
 * in the top-right quadrant where the splash's satellite sits; a tap sends it round again, momentum kept. The empty
 * glass stays clear and only picks up light piped from the liquid. Lit by the scene light, with a light-caused
 * contact shadow and a caustic inside it. Nothing animates at rest.
 *
 * Spec: docs/components/HaloLogo.md · Archetype: Feedback (brand mark) · Showroom: halo://showroom/logo
 *
 * @param animated plays the flow on appear and on tap. False draws the resting mark, not pressable.
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
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val angle = remember { Animatable(REST) }
    // Distance flowed before the angle was last wrapped: keeps the internal currents continuous across wraps.
    val flowed = remember { mutableFloatStateOf(0f) }
    val glow = remember { Animatable(if (animated && !motion.reduced) 0f else 1f) }
    val press = rememberPressProgress(source)
    val scope = rememberCoroutineScope()
    val motionNow by rememberUpdatedState(motion)
    val onWrap: (Float) -> Unit = { flowed.floatValue += it }

    LaunchedEffect(animated) {
        if (!animated) {
            glow.snapTo(1f)
            angle.snapTo(REST)
            return@LaunchedEffect
        }
        launch { glow.animateTo(1f, motionNow.spring(HaloSpring.Gentle)) }
        angle.flow(motionNow, onWrap)
    }

    val glass: @Composable () -> Unit = {
        LogoGlass(
            size = size,
            surfaceColor = surfaceColor,
            fluidAngle = { angle.value },
            // Perceived speed: slow-mo stretches time, not the film or the menisci.
            flowSpeed = { angle.velocity * motionNow.scale },
            flowPhase = { angle.value - REST + flowed.floatValue },
            glow = { glow.value * (1f + PRESS_IGNITE * press.value) },
        )
    }
    if (animated) {
        // The touch response is the mark's own light (D5), so no highlight overlay.
        HaloPressable(
            onClick = { scope.launch { angle.flow(motionNow, onWrap) } },
            shape = CircleShape,
            onClickLabel = "Replay the halo",
            haptic = HaloHaptic.Tick,
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
 * The mark frozen at one moment of its motion: for state matrices, previews and static screenshots. Decorative
 * (no semantics).
 *
 * @param fluidAngle centre of the liquid, degrees clockwise from 3 o'clock; rest is [HALO_LOGO_REST_ANGLE].
 * @param flowSpeed degrees per second (sign = direction); sets the film and the menisci.
 * @param glow 0 = unlit liquid, 1 = glowing, up to 1.6 pressed.
 */
@Composable
fun HaloLogoFrame(
    fluidAngle: Float,
    flowSpeed: Float,
    glow: Float,
    modifier: Modifier = Modifier,
    size: HaloLogoSize = HaloLogoSize.Large,
    surfaceColor: Color = HaloTheme.colors.bg.raised,
) {
    LogoGlass(
        size = size,
        surfaceColor = surfaceColor,
        fluidAngle = { fluidAngle * DEG },
        flowSpeed = { flowSpeed * DEG },
        flowPhase = { fluidAngle * DEG },
        glow = { glow },
        modifier = modifier,
    )
}

/** One more revolution from wherever the liquid is heading, launched so it leaves at full speed. */
private suspend fun Animatable<Float, AnimationVector1D>.flow(motion: HaloMotion, onWrap: (Float) -> Unit) {
    val target = targetValue + TAU
    val omega = sqrt(HaloSpring.Orbit.stiffness) / motion.scale
    val kick = ORBIT_KICK * omega * (target - value)
    val result = animateTo(target, motion.spring(HaloSpring.Orbit), initialVelocity = max(velocity, kick))
    // Same place on the ring, so the wrap is invisible; it keeps the angle bounded.
    if (result.endReason == AnimationEndReason.Finished) {
        onWrap(value - REST)
        snapTo(REST)
    }
}

@Composable
private fun LogoGlass(
    size: HaloLogoSize,
    surfaceColor: Color,
    fluidAngle: () -> Float,
    flowSpeed: () -> Float,
    flowPhase: () -> Float,
    glow: () -> Float,
    modifier: Modifier = Modifier,
) {
    val light = HaloTheme.light
    val glass = HaloTheme.materials.glass
    val emission = HaloTheme.colors.accent.fill
    val shader = remember { RuntimeShader(HALO_GLASS_AGSL) }
    val brush = remember(shader) { ShaderBrush(shader) }
    Spacer(
        modifier
            .size(logoBox(size))
            .drawBehind {
                val w = this.size.minDimension
                val tube = w * TUBE_RADIUS
                with(shader) {
                    setFloatUniform("center", this@drawBehind.size.width / 2f, this@drawBehind.size.height / 2f)
                    setFloatUniform("ringRadius", w * RING_RADIUS)
                    setFloatUniform("tubeRadius", tube)
                    setFloatUniform("light", light.x, light.y, light.z)
                    setFloatUniform("shadowShift", light.shadowDx, light.shadowDy)
                    setFloatUniform("fluidCenter", fluidAngle())
                    setFloatUniform("fluidLength", FLUID_SHARE * TAU)
                    setFloatUniform("flowSpeed", flowSpeed())
                    setFloatUniform("flowPhase", flowPhase())
                    setFloatUniform("glow", glow())
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

// ------------------------------------------------------------------------------------------- geometry & motion

/** Centre-line radius of the ring, in box widths. */
private const val RING_RADIUS = 0.36f
/** Outer radius of the glass tube, in box widths. */
private const val TUBE_RADIUS = 0.058f
/** Share of the ring the liquid fills. */
private const val FLUID_SHARE = 0.25f
/** How far past the box the shadow and spilled light reach, in tube radii. */
private const val DRAW_MARGIN = 2.5f
/** Pressing makes the liquid glow up to this much brighter (card reference: +60%). */
private const val PRESS_IGNITE = 0.6f
/** Launch velocity as a fraction of ω·distance: 0.5 leaves at top speed and only decelerates (see the Tide). */
private const val ORBIT_KICK = 0.5f
private const val TAU = 6.2831855f
/** Radians per degree. */
private const val DEG = 0.017453292f
private const val REST = HALO_LOGO_REST_ANGLE * DEG

/*
 * The glass halo, one pass. Two parts:
 *
 *   floor  — what the ring does to the surface it rests on: a contact shadow cast by the key light from the height of
 *            the tube's centre line (glass lets about half the light through, focused into a caustic line), contact
 *            occlusion where it touches, and the liquid's light spilling out round it.
 *   tube   — a glass rod with a bore. The empty glass shows the floor under it through a cylindrical lens (flipped,
 *            magnified), absorbed towards the glass tint by thickness (Beer–Lambert), and the light it pipes from the
 *            liquid (strongest along the walls, where it is totally internally reflected). The liquid fills a quarter
 *            of the bore: self-luminous like radium, denser where thicker, with currents circulating inside it,
 *            scintillation grains that travel with it, concave menisci that flatten at the front and deepen at the
 *            back while it flows, and the wet film it leaves behind. On top, Fresnel-weighted reflections of the
 *            room and the key light as a tight highlight.
 *
 * Coordinates are local px, y down. Angles in radians, clockwise from 3 o'clock.
 */
private const val HALO_GLASS_AGSL = """
uniform float2 center;
uniform float ringRadius;
uniform float tubeRadius;
uniform float3 light;
uniform float2 shadowShift;
uniform float fluidCenter;
uniform float fluidLength;
uniform float flowSpeed;
uniform float flowPhase;
uniform float glow;
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
const float PI = 3.14159265;
const float TAU = 6.28318531;
// Inner radius of the bore, as a fraction of the tube: the rest is the glass wall.
const float BORE = 0.78;

// Shortest signed angle from b to a, in (−π, π].
float turn(float a, float b) {
    return mod(a - b + PI, TAU) - PI;
}

float noise2(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 w = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1.0, 0.0)), w.x),
               mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), w.x), w.y);
}

// Position along the liquid at ring angle phi: radians from its centre, + towards its leading end.
float along(float phi) {
    float d = turn(phi, fluidCenter);
    return flowSpeed < 0.0 ? -d : d;
}

// Radians inside the liquid (negative outside) at position a and lateral position ub (−1 … +1 across the bore).
// Concave menisci: the liquid creeps along the walls. Flowing, the leading one flattens and the trailing one deepens.
float fluidDepth(float a, float ub) {
    float s = clamp(abs(flowSpeed) / 8.0, 0.0, 1.0);
    float m = 0.4 * tubeRadius / ringRadius;
    float reach = fluidLength * 0.5;
    float creep = ub * ub - 0.5;
    float head = reach + m * (1.0 - 0.6 * s) * creep;
    float tail = -reach - m * (1.0 + 0.6 * s) * creep;
    return min(head - a, a - tail);
}

// Light the empty glass picks up from the liquid: piped along the tube from its ends, a faint glow all round, and
// the wet film the liquid leaves on the walls while it flows.
float piped(float a) {
    float reach = fluidLength * 0.5;
    float gap = max(abs(a) - reach, 0.0);
    float s = abs(flowSpeed);
    float behind = max(-a - reach, 0.0);
    float film = a < -reach ? exp(-behind / (0.08 + s * 0.07)) * min(s * 0.07, 0.45) : 0.0;
    return exp(-gap / 0.3) * 0.5 + 0.03 + film;
}

// Fresnel reflectance (Schlick) across the tube at u.
float fresnel(float u) {
    float nz = sqrt(max(1.0 - u * u, 0.0));
    return reflectance + (1.0 - reflectance) * pow(1.0 - nz, 5.0);
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
    float a = along(atan(g.y, g.x));
    float inside = clamp(fluidDepth(a, 0.0) * ringRadius / tubeRadius + 0.5, 0.0, 1.0);
    // Gaussian falloff: the light hugs the tube (a slower one would narrow into a ray towards the centre).
    float k = cd / (tubeRadius * 1.6);
    float spill = min(mix(piped(a), 1.0, inside) * glow * exp(-k * k) * 0.32, 0.6);
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
    float cover = clamp(tubeRadius - abs(s) + 0.5, 0.0, 1.0);
    if (cover <= 0.0) return floorOut;

    float u = clamp(s / tubeRadius, -1.0, 1.0);
    float nz = sqrt(max(1.0 - u * u, 0.0));
    float3 n = float3(radial * u, nz);

    // The empty glass: the floor under it through a cylindrical lens, absorbed by thickness.
    float4 under = floorAt(center + radial * (ringRadius - u * tubeRadius * 0.6));
    float3 floorC = surfaceColor.rgb * (1.0 - under.a) + under.rgb;
    float3 c = mix(glassTint.rgb, floorC, exp(-absorption * 2.0 * nz));

    // Light piped by the glass from the liquid, strongest along the walls; right past the liquid's ends the glass
    // fills with its light over about a tube's width.
    float a = along(atan(q.y, q.x));
    float ub = u / BORE;
    float depthPx = fluidDepth(a, clamp(ub, -1.0, 1.0)) * rho;
    float wall = 0.2 + 0.8 * (u * u) * (u * u);
    float endFill = exp(-max(-depthPx, 0.0) / (tubeRadius * 1.3)) * 0.5;
    c = mix(c, emission.rgb, clamp((piped(a) * wall * 0.7 + endFill) * glow, 0.0, 0.85));

    // The liquid in the bore.
    float inBore = clamp((BORE - abs(u)) * tubeRadius + 0.5, 0.0, 1.0);
    float fluid = clamp(depthPx + 0.5, 0.0, 1.0) * inBore;
    if (fluid > 0.0) {
        float nb = sqrt(max(1.0 - ub * ub, 0.0));
        // Currents: forward in the core, back along the walls (Taylor flow), advected by the distance flowed.
        float arc = a * ringRadius / tubeRadius;
        float shear = flowPhase * ringRadius / tubeRadius * 0.12 * (ub * ub * 2.0 - 0.67);
        float current = noise2(float2(arc * 1.1 - shear, ub * 1.6 + 3.0)) * 0.6
            + noise2(float2(arc * 2.3 - shear * 1.7, ub * 3.1)) * 0.4;
        // Scintillation: fine grains that travel with the liquid.
        float grain = step(0.975, hash(floor(float2(a * rho, ub * tubeRadius) / 1.2) + 7.0));
        float bright = dot(surfaceColor.rgb, float3(0.299, 0.587, 0.114));
        float3 radium = emission.rgb * glow * (0.85 + 0.3 * current) + highlightColor.rgb * grain * 0.3 * glow;
        // Densest at the core: it burns towards white on dark surfaces and stays saturated on bright ones.
        radium = mix(radium, highlightColor.rgb, clamp(nb * nb * glow * mix(0.35, 0.05, bright), 0.0, 1.0));
        // Unlit, it still reads as liquid: the emission colour, dimmed by the glass.
        float3 liquid = mix(mix(c, emission.rgb * 0.45, 0.7), radium, clamp(glow, 0.0, 1.0));
        // Thin at the walls of the bore, so it is partly transparent there.
        float density = 1.0 - exp(-4.0 * nb);
        // The meniscus catches the light: a thin bright line where the liquid ends.
        float meniscus = exp(-depthPx * depthPx / 2.0) * 0.35 * clamp(glow, 0.3, 1.0);
        c = mix(c, liquid + highlightColor.rgb * meniscus, fluid * density);
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
    return mix(floorOut, half4(half3(softClip(c)), 1.0), half(cover));
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
        HaloLogoFrame(fluidAngle = HALO_LOGO_REST_ANGLE, flowSpeed = 0f, glow = 0f)
        HaloLogoFrame(fluidAngle = 120f, flowSpeed = 400f, glow = 1f)
        HaloLogoFrame(fluidAngle = HALO_LOGO_REST_ANGLE, flowSpeed = 0f, glow = 1.6f)
    }
}
