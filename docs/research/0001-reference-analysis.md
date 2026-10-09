# Research 0001: how the `Refs/` surfaces and components are built

**Method:**
- Each video was broken into ffmpeg contact sheets and full-resolution crops.
- Motion was measured from raw pixels (`measure.py`: a scanline per frame for the toggle; bolt-region luma and card
  width per frame for the card).
- Times come from 60 fps sources.

This document feeds proposal 0001 (material engine) and the `halo-component` skill.

## The enabling insight: one SDF lighting shader family
Nearly everything in the references is a **rounded shape with a height profile, lit by one light**:
- the toggle housing, channel and thumb
- the gel button and its bezel
- the heart badge well
- the orb

All of these can be rendered by one AGSL shader family:

1. **Shape SDF:** analytic signed distance to a rounded rect, stadium, circle or sphere. It's exact, cheap and resolution-independent.
2. **Height profile `h(d)`** from the distance to the edge:
   - raised bevel (housing rim, thumb)
   - recess (channel, bezel, badge well)
   - dome (gel, orb)
   - dish (finger rests)

   Flipping the sign turns raised into recessed.
3. **Normal = ∇h** (from the SDF gradient), then **Lambert + Blinn-Phong specular + Fresnel** against the scene light
   uniform (key light top-left), plus **AO** as a function of distance into a recess.
4. Materials only change the profile and coefficients (roughness, specular, Fresnel, subsurface), not the shader.

Two more techniques complete the set:
- **Backdrop-sampling shaders:** `RenderEffect.createRuntimeShaderEffect(shader, "content")` (API 33, our minSdk) lets
  AGSL read the layer behind it. This drives glass refraction, the orb's lens distortion and frosted glass with real
  bending.
- **Emitters as scene lights:** `HaloLight` keeps a list of emitters (position, colour, intensity), and Edge layers add
  their contribution. That's how the graph card's rim turns orange next to the glowing tube.

## Per reference

### `toggle.mp4`: light ceramic switch (→ `HaloToggle`, both themes)
| Part | Construction |
|---|---|
| Housing | A raised stadium rim, lit top-left with the shadow side bottom-right. The **Occlusion** layer casts a soft contact shadow down-right (light-caused). |
| Channel | A recessed stadium with AO on the upper-left inner wall (~4dp) and a light bounce on the lower edge. |
| Thumb | A raised stadium (dome profile) with **two circular dishes** (finger rests). It casts a shadow onto the channel floor that moves with it. Its highlight stays top-left as it slides, because the light is world-fixed. |
| Fill | Base gradient from yellow-orange at the top to orange-red at the bottom (lit from above), × **velvet grain** (fine value noise, about ±8% luma, ~1–2px), + AO rim (deep red, ~3dp). Where the thumb's shadow falls, the fill is darker, so it **brightens as the thumb uncovers it**. |

**Measured motion:**
- **ON:** thumb travel settles in about **0.33s**, critically damped, with **no overshoot**. The scanline positions follow
  83 → 91 → 103 → 119 → 139 → 157 → 169 → 175 → 178 → 179 px at 33ms steps. That fits ζ = 1.0, k ≈ 400.
  `Gentle` (k 220, ~0.45s) is too slow, so this proposes the new token **`HaloSpring.Mechanical(1.0, 400)`**.
- **OFF:** a slow **anticipation creep** of about 15% of the travel over ~0.35s (179 → 164 px), then a fast completion
  (~0.25s). The touch translation: while pressed, the thumb **leans ~15% toward its target**; on release it completes on `Mechanical`.

*Uncertain:* whether the two thumb circles are dishes or domes. The shading reads as shallow dishes; I'll prototype both and compare.

### `card.mp4`: emissive glass tile (Lightning cyan, D8)
| Part | Construction |
|---|---|
| Body | A deep-navy glass slab. **Fresnel** keeps the centre dark and brightens a blue rim band (~10dp) near the edges, which reads as glass thickness. A specular hairline sits on the top-left edge. |
| Bolt | An emissive object *inside* the glass: white-cyan core, blue falloff, and bevelled faces (its own SDF lighting). **Bloom** is a blurred copy, clipped to the card. The bolt is cropped by the card edge. |
| Light bleed | The rim brightens near the bolt (an emitter → Edge layer). The text is cyan with a faint glow. |
| Occlusion | A soft ambient shadow onto the wall below. |

**Measured interaction** (desktop hover in the video; touch translation in brackets):
- **Ignite:** bolt luma +60% (105 → 200) in about **0.15s**, with no overshoot → `Quick`/`Snap`. [On touch-down.]
- **Light follows the pointer:** before the ignite, the bolt-centre luma *dips* ~20% as the highlight moves toward the
  approaching cursor. [The specular centre tracks the finger.]
- **Lift:** card width +3.1% (255 → 263) over about **0.4s** → `Gentle`. [Pressing an *object* lifts it.]
- **Release:** emission falls 208 → 107 in **0.25s**, **undershoots ~18%** below rest, then recovers in ~0.35s. That's an
  underdamped intensity spring (ζ ≈ 0.65) → `Expressive`, an incandescent "after-dip". Scale returns on `Gentle`.

**Principle derived from this:** *controls sink, objects lift.* Buttons and toggles press in (0.97); glass cards and
tiles rise (1.03) and ignite.

### `3d graph immersive.mp4`: dark frosted glass with neon tubes
| Part | Construction |
|---|---|
| Card | Smoky frosted glass (backdrop blur + dark tint + grain) with a white rim hairline on the top and right edges. The rim **takes on the colour of nearby emitters** (orange next to the tube and heart). |
| Tilt | Real perspective: `graphicsLayer { rotationX / rotationY; cameraDistance }`, about ±6°, slow and continuous. [Device tilt, D6.] |
| Neon tube | A 3D curve projected with perspective, drawn as **layered strokes** (wide dark → mid → thin bright core) for cylindrical shading, plus bloom. The colour gradient runs *along* the curve (yellow → orange → pink-red). |
| Glass tube | Wide low-alpha stroke, thin specular strokes offset toward the light, and a slight backdrop distortion. |
| Pop-out | The curve has z-depth. It is split into segments, **sorted back-to-front** (painter's algorithm), and its width scales with perspective. The tubes intertwine and leave the card plane with no 3D engine needed. [D7.] |
| Heart badge | A recessed well (concave SDF), an emissive orange ring, and a glossy heart (gradient + specular). |
| Motion | Tubes draw on, and an end **whips** out of the plane with overshoot (an underdamped spring). The card tilts continuously. |

### `danger button.webp`: gel button
- A recessed **bezel** (concave SDF ring) in soft-touch matte black.
- A translucent red **gel** dome: subsurface makes it brighter in the centre and darker at the edges, with a high-gloss specular band at the top.
- An **emissive label** (glow), with light bleed into the bezel.

### `nav bar button orange.webp`: glass orb
- An analytic **sphere** in AGSL: its normal comes from the sphere equation.
  - **Refraction** of the bar behind it, through the backdrop-sampling shader.
  - A Fresnel rim and a specular highlight at the top-left.
  - Internal orange emission (subsurface).
  - A **caustic** bright spot bottom-right.
- An emissive icon, and a **contact shadow** on the bar.
- The bar has a bevel, and the wood strip has a recessed slot. Procedural wood is possible in AGSL, but it's a quality risk; a bitmap texture would be safer.

### `card.webp`: frosted cards on a lit wall
- The **environment:**
  - a grainy wall
  - soft diagonal window-light bands (gobo) from the key light
  - card contact shadows that agree with the light
- **Frosted glass** = backdrop blur + tint (light or dark) + grain + edge highlight. Coloured data bars have emissive caps (dots).

## Resolved by the product owner (2026-10-09)
- **Thumb circles are dishes** (concave finger rests).
- **Card "hover" becomes a landing animation:** when a card lands on the surface it plays the sequence once:
  lift → ignite → settle with the after-dip. There are no hover semantics on touch.
- **Devices are high-end.** The full pipeline is the default; a lighter fallback (no refraction or bloom, static
  specular) exists only as an emergency path, plus reduced motion.
- **Texture sourcing** (decided here; the standard is that nothing may look fake or cheap):
  - Micro-surfaces (grain, velvet, soft-touch, frosted, brushed) are procedural AGSL.
  - Natural materials (wood, stone, fabric) are photographic CC0 PBR maps (albedo + normal + roughness), lit by the
    same SDF shader. Procedural wood is rejected.
  - Downloads need the owner's approval at the time.
- **Quality bar:** pixel-perfect with *no single flawed dot*. Every slice gets a zoomed pixel review of edges, corners,
  gradients and seams in both themes (`scripts/zoom.ps1`), on top of the full-screen review.

## What this changes
1. **Build order:** the SDF lighting shader is the foundation of *every* reference. Build it first, inside a vertical
   slice: **`HaloToggle`** (light + dark), which exercises:
   - SDF, raised/recessed/dish profiles, lighting and AO
   - the moving occlusion shadow
   - velvet grain and the uncover-brightening fill
   - `Mechanical` motion with press anticipation

   Then generalise it into the 8-layer engine.
2. **New tokens:**
   - `HaloSpring.Mechanical(1.0, 400)`.
   - Press anticipation (~15% lean for switches).
   - An emission envelope: ignite on `Quick`, release on `Expressive` with undershoot.
   - Lift 1.03 for objects.
3. **Performance tiers:** the SDF lighting is cheap. Backdrop refraction and bloom cost more. Define a degrade path (static
   specular, no refraction) for low-end devices and reduced motion.
