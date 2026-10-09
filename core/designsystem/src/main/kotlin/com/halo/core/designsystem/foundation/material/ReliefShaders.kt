package com.halo.core.designsystem.foundation.material

/*
 * Halo's SDF lighting shader family (research 0001). Every physical surface is a rounded shape with a height
 * profile, lit by the one scene light:
 *
 *   shape SDF  →  height h(p)  →  normal ∇h  →  Lambert + Blinn-Phong + AO + self-shadow
 *
 * All coordinates are local pixels. Colour uniforms use `layout(color)` so they are converted into the
 * destination colour space exactly like Compose-drawn colours — a token renders identically either way.
 * Coverage is analytic (0.5px SDF band), so every edge is anti-aliased without MSAA.
 */

internal const val SDF_COMMON = """
// Rim self-shadow: fraction of the geometric shadow length (recesses read shallow) and penumbra growth.
const float SHADOW_REACH = 0.4;
const float PENUMBRA = 0.6;
// Every crease gets a physical rounded lip this wide (px): the light rolls across it over >1px, so creases
// can never alias into stippled single-pixel highlights.
const float LIP = 1.5;
// Highlights roll off smoothly above this level instead of hard-clipping (no Mach bands). Flat token
// colours sit at or below it, so they still render exactly.
const float KNEE = 0.98;

float sdRoundRect(float2 p, float4 r, float radius) {
    float2 c = (r.xy + r.zw) * 0.5;
    float2 h = (r.zw - r.xy) * 0.5;
    float2 q = abs(p - c) - h + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

// Rounded edge profile: 0 at the edge, 1 once `t` (distance inside / bevel) reaches 1.
// 1 − (1 − t)³: finite slope at the edge (3 / bevel), so the normal never flips inside one pixel; and zero
// slope *and* zero curvature where it meets the flat top (C2), so the shading has no kink — no Mach band line.
float rim(float t) {
    float u = 1.0 - clamp(t, 0.0, 1.0);
    return 1.0 - u * u * u;
}

// Softened distance from an edge: zero slope at the edge (C1), ≈ s − LIP well inside.
float softEdge(float s) {
    s = max(s, 0.0);
    return s * s / (s + LIP);
}

float3 softClip(float3 c) {
    float3 over = max(c - KNEE, 0.0);
    return min(c, float3(KNEE)) + (1.0 - KNEE) * (1.0 - exp(-over / (1.0 - KNEE)));
}

float hash(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
"""

/** A raised body, optionally with a recessed channel and up to two dishes. */
internal const val RELIEF_AGSL = """
uniform float4 body;
uniform float bodyRadius;
uniform float bevel;
uniform float height;
uniform float4 recess;
uniform float recessRadius;
uniform float recessBevel;
uniform float recessDepth;
uniform float4 dishA;
uniform float4 dishB;
layout(color) uniform half4 baseColor;
layout(color) uniform half4 recessColor;
layout(color) uniform half4 highlightColor;
layout(color) uniform half4 shadowColor;
uniform float3 light;
uniform float ambient;
uniform float diffuse;
uniform float specular;
uniform float shininess;
uniform float shadowOpacity;
uniform float aoStrength;
uniform float grain;
uniform float seed;
""" + SDF_COMMON + """
bool hasRecess() { return recess.z > recess.x; }

float dish(float2 p, float4 d) {
    if (d.z <= 0.0) return 0.0;
    float r = 1.0 - softEdge(d.z - length(p - d.xy)) / d.z;
    return r < 1.0 ? d.w * (1.0 - r * r) : 0.0;
}

float surfaceHeight(float2 p) {
    float h = height * rim(-sdRoundRect(p, body, bodyRadius) / bevel);
    if (hasRecess()) {
        h -= recessDepth * rim(softEdge(-sdRoundRect(p, recess, recessRadius)) / recessBevel);
    }
    return h - dish(p, dishA) - dish(p, dishB);
}

half4 main(float2 p) {
    float d = sdRoundRect(p, body, bodyRadius);
    float coverage = clamp(0.5 - d, 0.0, 1.0);
    if (coverage <= 0.0) return half4(0.0);

    float dx = surfaceHeight(p + float2(0.5, 0.0)) - surfaceHeight(p - float2(0.5, 0.0));
    float dy = surfaceHeight(p + float2(0.0, 0.5)) - surfaceHeight(p - float2(0.0, 0.5));
    float3 n = normalize(float3(-dx, -dy, 1.0));

    // Flat faces render exactly at the token colour (ambient + diffuse == 1 for n = +z).
    float shade = ambient + diffuse * max(dot(n, light), 0.0) / light.z;
    float3 halfway = normalize(light + float3(0.0, 0.0, 1.0));
    float spec = specular * max(pow(max(dot(n, halfway), 0.0), shininess) - pow(halfway.z, shininess), 0.0);

    float3 albedo = float3(baseColor.rgb);
    float shadowAmount = 0.0;
    if (hasRecess()) {
        float si = -sdRoundRect(p, recess, recessRadius);
        float floorness = rim(si / recessBevel);
        albedo = mix(albedo, float3(recessColor.rgb), floorness);
        float ao = aoStrength * (1.0 - smoothstep(0.0, recessBevel * 2.0, si)) * smoothstep(-0.5, 0.5, si);
        // The rim blocks the key light for the floor next to the wall that faces it. Shallow recesses cast a
        // short shadow; the penumbra widens with distance from the rim (soft area light, not a hard edge).
        float2 towardsLight = light.xy / light.z * recessDepth * SHADOW_REACH * floorness;
        float penumbra = length(towardsLight) * PENUMBRA + 0.75;
        float blocked = smoothstep(-penumbra, penumbra, sdRoundRect(p + towardsLight, recess, recessRadius));
        shadowAmount = 1.0 - (1.0 - ao) * (1.0 - shadowOpacity * blocked * floorness);
    }
    float3 tint = mix(float3(1.0), float3(shadowColor.rgb), shadowAmount);
    float3 color = (albedo * shade + float3(highlightColor.rgb) * spec) * tint;
    color *= 1.0 + (hash(floor(p) + seed) - 0.5) * 2.0 * grain;
    color = clamp(softClip(color), 0.0, 1.0);
    return half4(half3(color) * half(coverage), half(coverage));
}
"""

/** Velvet fill — the texture layer inside a channel: lit gradient, fibre grain, warm AO rim, rim shadow. */
internal const val VELVET_AGSL = """
uniform float4 channel;
uniform float channelRadius;
uniform float channelBevel;
uniform float channelDepth;
uniform float2 extent;
layout(color) uniform half4 topColor;
layout(color) uniform half4 bottomColor;
layout(color) uniform half4 rimColor;
layout(color) uniform half4 shadowColor;
uniform float3 light;
uniform float shadowOpacity;
uniform float aoStrength;
uniform float grain;
uniform float seed;
""" + SDF_COMMON + """
half4 main(float2 p) {
    float d = sdRoundRect(p, channel, channelRadius);
    float coverage = clamp(0.5 - d, 0.0, 1.0)
        * clamp(p.x - extent.x + 0.5, 0.0, 1.0)
        * clamp(extent.y - p.x + 0.5, 0.0, 1.0);
    if (coverage <= 0.0) return half4(0.0);
    float si = -d;

    float t = clamp((p.y - channel.y) / (channel.w - channel.y), 0.0, 1.0);
    float3 color = mix(float3(topColor.rgb), float3(bottomColor.rgb), smoothstep(0.0, 1.0, t));

    // Velvet: fine fibres plus slightly coarser clumps.
    float fibre = (hash(floor(p) + seed) - 0.5) * 0.65 + (hash(floor(p * 0.5) + seed * 1.7) - 0.5) * 0.35;
    color *= 1.0 + fibre * 2.0 * grain;

    // Warm occlusion where the fill meets the channel walls.
    float ao = aoStrength * (1.0 - smoothstep(0.0, channelBevel * 1.5, si));
    color = mix(color, float3(rimColor.rgb), ao);

    float2 towardsLight = light.xy / light.z * channelDepth * SHADOW_REACH;
    float penumbra = length(towardsLight) * PENUMBRA + 0.75;
    float blocked = smoothstep(-penumbra, penumbra, sdRoundRect(p + towardsLight, channel, channelRadius));
    color *= mix(float3(1.0), float3(shadowColor.rgb), shadowOpacity * blocked);

    color = clamp(softClip(color), 0.0, 1.0);
    return half4(half3(color) * half(coverage), half(coverage));
}
"""

/**
 * Light-caused contact shadow (decision D3): the caster's own SDF, offset away from the key light by its
 * elevation and softened in proportion to it. Drawn source-over (premultiplied), so it composites correctly
 * even inside offscreen layers.
 */
internal const val SHADOW_AGSL = """
uniform float4 caster;
uniform float casterRadius;
uniform float2 offset;
uniform float blur;
uniform float opacity;
layout(color) uniform half4 shadowColor;
""" + SDF_COMMON + """
half4 main(float2 p) {
    float d = sdRoundRect(p - offset, caster, casterRadius);
    float a = opacity * (1.0 - smoothstep(-blur, blur, d));
    return half4(shadowColor.rgb * half(a), half(a));
}
"""
