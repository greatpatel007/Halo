package com.halo.core.designsystem.foundation.material

/*
 * Tide — the full-screen theme transition. A wave front sweeps from the trigger to the far corner:
 *
 *  ahead  (old theme) ─ content stretches toward the coming front (directional smear, iPhone-Duo-like)
 *  front             ─ sand: grains flip one by one from old to new; a broken foam crest rides on top
 *  behind (new theme) ─ wet-sand band (slightly darker, grainy) with faint water glints, then clean
 *
 * The front meanders on flowing, domain-warped noise plus a long swell, so it moves like water over sand.
 * Runs as a RenderEffect on the window layer: `content` is the live new theme, `previous` the frozen frame.
 */
internal const val TIDE_AGSL = """
uniform shader content;
uniform shader previous;
uniform float2 size;
uniform float2 origin;
uniform float2 dir;
uniform float front;
uniform float band;
uniform float dissolve;
uniform float grain;
uniform float amplitude;
uniform float smear;
uniform float time;
layout(color) uniform half4 foam;

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

half4 main(float2 p) {
    float2 perp = float2(-dir.y, dir.x);
    float along = dot(p - origin, dir);
    float across = dot(p - origin, perp);

    // Water: flowing domain-warped noise bends the front; a long swell travels along it.
    float2 q = p / (size.x * 0.25);
    float2 flow = float2(fbm(q * 1.3 + float2(time * 0.45, -time * 0.20)),
                         fbm(q * 1.3 + float2(-time * 0.30, time * 0.40) + 7.3));
    float meander = (fbm(q * 2.0 + flow * 1.6) - 0.5) * 2.0;
    float swell = sin(across / (size.x * 0.18) + time * 3.2);
    float x = along + meander * amplitude + swell * amplitude * 0.45 - front;

    // Far from the front: a single clean sample of the right theme.
    if (x > band) return previous.eval(p);
    if (x < -band) return content.eval(p);

    // Directional smear, strongest at the front, gone at the edges of the band.
    float drag = (1.0 - smoothstep(0.0, band, abs(x)));
    float smearLen = smear * drag * drag;
    float ripple = sin(x / (band * 0.09) - time * 9.0) * exp(-abs(x) / (band * 0.5));
    float2 wobble = dir * ripple * amplitude * 0.12;

    half4 newC = half4(0.0);
    half4 oldC = half4(0.0);
    for (int i = 0; i < 5; i++) {
        float k = float(i) * 0.25;
        float2 sp = clamp(p + wobble - dir * smearLen * k, float2(0.5), size - 0.5);
        newC += content.eval(sp);
        oldC += previous.eval(sp);
    }
    newC *= 0.2;
    oldC *= 0.2;

    // Sand: each grain flips from old to new as the dissolve band passes over it.
    float2 cell = floor(p / grain);
    float g = hash(cell + 13.7);
    float t = clamp((x + dissolve) / (2.0 * dissolve), 0.0, 1.0);
    float showOld = smoothstep(g - 0.06, g + 0.06, t);
    half4 c = mix(newC, oldC, half(showOld));

    // Wet sand just behind the front: a touch darker, grainy.
    float wet = exp(-pow((x + dissolve * 1.5) / (dissolve * 1.2), 2.0));
    float sand = hash(cell + 3.1) - 0.5;
    float near = exp(-abs(x) / dissolve);
    c.rgb *= half(1.0 - 0.07 * wet + sand * 0.10 * (wet + near));

    // Foam crest riding the front, broken up by grains and moving noise.
    float broken = smoothstep(0.35, 0.75, hash(cell + 5.9) * 0.6 + vnoise(p / (grain * 6.0) + time) * 0.6);
    float crest = exp(-pow(x / (dissolve * 0.35), 2.0)) * broken;
    c.rgb = mix(c.rgb, foam.rgb * c.a, half(crest * 0.55));

    // Faint caustic glints in the water behind the front.
    float glint = pow(abs(sin((across + meander * 40.0) / (grain * 9.0) + flow.x * 6.0)), 24.0)
        * exp(-abs(x + dissolve * 3.0) / dissolve) * 0.25;
    c.rgb += foam.rgb * half(glint) * c.a;
    return c;
}
"""
