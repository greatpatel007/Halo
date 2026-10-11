package com.halo.core.designsystem.foundation.material

/*
 * Tide — the full-screen theme transition. A wave front grows from the trigger and sweeps to the far corner:
 *
 *  ahead  (old theme) ─ content stretches toward the coming front (directional smear, iPhone-Duo-like)
 *  front             ─ sand: grains flip one by one from old to new; a broken foam crest rides on top
 *  behind (new theme) ─ wet-sand band (slightly darker, grainy) with faint water glints, then clean
 *
 * Shape: a conic with its focus on the trigger, m = mix(along, r, bend). Near the trigger the front is
 * curved (an egg around the switch); as it travels it reads flatter and flatter across the fixed screen.
 * Distances are divided by |∇m| so the band keeps a constant pixel width all the way round.
 * The front meanders on flowing, domain-warped noise plus a long swell, so it moves like water over sand.
 * Runs as a RenderEffect on the window layer: `content` is the live theme, `frozen` a frozen frame. Normally the
 * live theme is behind the front (washed in) and the frozen old frame ahead; after a mid-wave reversal the theme
 * flips back, so `swap` = 1 makes the frozen frame the washed region and the live theme the unwashed one.
 *
 * The trigger (`keep`, a pill) always shows the live content: it is where the new theme comes from, so its own
 * response to the finger (the switch thumb sliding) is never hidden by the frozen frame or the sand.
 */
internal const val TIDE_AGSL = """
uniform shader content;
uniform shader frozen;
uniform float2 size;
uniform float2 origin;
uniform float2 dir;
uniform float bend;
uniform float front;
uniform float band;
uniform float dissolve;
uniform float grain;
uniform float amplitude;
uniform float smear;
uniform float time;
uniform float swap;
// HDR: gain for emitted light (1 on SDR). Foam and glints catch the light and may sparkle above white.
uniform float hdr;
layout(color) uniform half4 foam;
uniform float4 keep;
uniform float keepRadius;

float hash(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1.0, 0.0)), u.x),
               mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * vnoise(p);
        p = p * 2.03 + 17.1;
        a *= 0.5;
    }
    return v;
}

half4 behind(float2 p) { return swap > 0.5 ? frozen.eval(p) : content.eval(p); }
half4 ahead(float2 p) { return swap > 0.5 ? content.eval(p) : frozen.eval(p); }

half4 tide(float2 p) {
    float2 d = p - origin;
    float r = length(d);
    float2 radial = r > 0.5 ? d / r : dir;
    float2 perp = float2(-dir.y, dir.x);
    float along = dot(d, dir);
    float across = dot(d, perp);

    // Conic front around the trigger; normal and slope of the distance field.
    float2 grad = mix(dir, radial, bend);
    float slope = max(length(grad), 0.15);
    float2 normal = grad / slope;
    float m = mix(along, r, bend);

    // Water: flowing domain-warped noise bends the front; a long swell travels along it.
    float2 q = p / (size.x * 0.25);
    float2 flow = float2(fbm(q * 1.3 + float2(time * 0.45, -time * 0.20)),
                         fbm(q * 1.3 + float2(-time * 0.30, time * 0.40) + 7.3));
    float meander = (fbm(q * 2.0 + flow * 1.6) - 0.5) * 2.0;
    float swell = sin(across / (size.x * 0.18) + time * 3.2);
    float x = (m - front) / slope + meander * amplitude + swell * amplitude * 0.45;

    // Far from the front: a single clean sample of the right theme.
    if (x > band) return ahead(p);
    if (x < -band) return behind(p);

    // Directional smear along the front normal, strongest at the front, gone at the edges of the band.
    // Tap offsets are jittered per grain so the stretch reads as continuous sand, never as ghosted copies.
    float2 cell = floor(p / grain);
    float drag = 1.0 - smoothstep(0.0, band, abs(x));
    float smearLen = smear * drag * drag * drag;
    float ripple = sin(x / (band * 0.12) - time * 9.0) * exp(-abs(x) / (band * 0.35));
    float2 wobble = normal * ripple * amplitude * 0.08;
    float jitter = hash(cell + 1.7);

    half4 newC = half4(0.0);
    half4 oldC = half4(0.0);
    for (int i = 0; i < 5; i++) {
        float k = (float(i) + jitter) * 0.2;
        float2 sp = clamp(p + wobble - normal * smearLen * k, float2(0.5), size - 0.5);
        newC += behind(sp);
        oldC += ahead(sp);
    }
    newC *= 0.2;
    oldC *= 0.2;

    // Sand: each grain flips from old to new as the dissolve band passes over it. Thresholds stay inside
    // the dissolve so every grain is fully settled by its edges (no stray speckle across the band).
    float g = 0.06 + 0.88 * hash(cell + 13.7);
    float t = clamp((x + dissolve) / (2.0 * dissolve), 0.0, 1.0);
    float showOld = smoothstep(g - 0.05, g + 0.05, t);
    half4 c = mix(newC, oldC, half(showOld));

    // Wet sand just behind the front: a touch darker, grainy. Speckle stays tight to the front.
    float wet = exp(-pow((x + dissolve * 1.5) / (dissolve * 1.2), 2.0));
    float sand = hash(cell + 3.1) - 0.5;
    float near = exp(-abs(x) / (dissolve * 0.5));
    c.rgb *= half(1.0 - 0.07 * wet + sand * (0.10 * wet + 0.05 * near));

    // Foam crest riding the front, broken up by grains and moving noise.
    float broken = smoothstep(0.35, 0.75, hash(cell + 5.9) * 0.6 + vnoise(p / (grain * 6.0) + time) * 0.6);
    float crest = exp(-pow(x / (dissolve * 0.35), 2.0)) * broken;
    c.rgb = mix(c.rgb, foam.rgb * c.a, half(crest * 0.55));

    // Faint caustic glints in the water behind the front.
    float glint = pow(abs(sin((across + meander * 40.0) / (grain * 9.0) + flow.x * 6.0)), 24.0)
        * exp(-abs(x + dissolve * 3.0) / dissolve) * 0.25;
    c.rgb += foam.rgb * half(glint) * c.a;
    c.rgb += foam.rgb * half((crest * 0.55 + glint) * (hdr - 1.0)) * c.a;
    return c;
}

half4 main(float2 p) {
    // Signed distance to the trigger's pill: inside it the live content shows untouched, with a 1px soft edge.
    float2 extent = (keep.zw - keep.xy) * 0.5;
    float2 q = abs(p - (keep.xy + keep.zw) * 0.5) - extent + keepRadius;
    float k = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - keepRadius;
    if (k < -1.0) return content.eval(p);
    half4 c = tide(p);
    return k < 1.0 ? mix(content.eval(p), c, half(smoothstep(-1.0, 1.0, k))) : c;
}
"""
