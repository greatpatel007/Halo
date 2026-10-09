# Proposal 0001: Halo Material, a layered surface stack with scene lighting

**Status:** **Accepted** (2026-10-09; decisions below) · **Raised by:** product direction, "surfaces need
texture and lighting (close to ray-traced) for a real-world look. Texture should be its own layer so the surface
can animate without animating the element."

## Decisions (2026-10-09)
These come from a frame-by-frame review of `Refs/` (ffmpeg contact sheets) and the current design.

| # | Decision | Detail |
|---|---|---|
| D1 | **Dark and light ship together** on one material engine | **Dark:** soft-touch matte body, dark frosted glass, gel/glass emissive accents. **Light:** matte ceramic, light frosted glass, velvet-grain fills. Both are built and QA'd in the same pass, never "light later". |
| D2 | **The reference toggle becomes `HaloToggle`** in both themes, and it is also the app's light/dark switch | Inset channel with AO, a raised capsule thumb that casts a shadow, a velvet-grain fill that brightens as the thumb uncovers it. Motion: ~330ms, critically damped, no overshoot (measured from `toggle.mp4`), which maps to the new `Mechanical(1.0, 400)`, plus a ~15% press-anticipation lean. |
| D3 | **Shadows exist only as consequences of the scene light** | Contact shadows under raised elements and ambient occlusion in recesses, drawn by the material **Occlusion** layer from the shared light. Decorative drop shadows stay banned. This replaces "depth from light, never shadow". |
| D4 | **One scene light: key light top-left by default** | Every reference is lit from the top-left. All surfaces read the same `HaloLight`, so highlights, rims and shadows agree. |
| D5 | **Touch-reactive light** | On press, emission ramps up and the specular highlight moves toward the finger (`card.mp4`). |
| D6 | **Device-tilt light + 3D parallax** | Tilting moves the light and gives hero cards a perspective tilt, with content layers at different depths (`3d graph immersive.mp4`). Sensor runs only while visible, at ~30 Hz, spring-smoothed. Off under reduced motion and power saver. |
| D7 | **Pop-out depth for signature moments only** | Content may break the card plane (graph curves, an orb rising from a bar). At most 1–2 per screen. |
| D8 | **Lightning gets electric cyan; on-chain Bitcoin stays orange; lime stays the UI accent** | Emission colour carries meaning: lime = act, orange = on-chain, cyan = Lightning. `HaloHoldToSend` and Bitcoin-variant controls take the network into account. |

**Motion calibration** (measured from raw pixels; see [research 0001](../research/0001-reference-analysis.md)):
- Toggle thumb ON: settles in ~0.33s, critically damped, no overshoot. The new token `Mechanical(1.0, 400)` fits;
  `Gentle` is too slow. OFF has a ~15% anticipation lean before the slide.
- Card: ignite +60% emission in ~0.15s (`Quick`). Release falls in 0.25s with an ~18% undershoot (`Expressive`).
  The tile lifts +3% over ~0.4s (`Gentle`). Principle: *controls sink, objects lift.*
- Graph tilt: slow, continuous, low amplitude (≤ 6°), driven by the sensor through `Gentle`.

**Technique:** every reference is a rounded shape with a height profile lit by one light. That calls for one
AGSL **SDF lighting shader family** (normals from the SDF, Lambert + Blinn-Phong + Fresnel + AO), plus
backdrop-sampling shaders (`RenderEffect.createRuntimeShaderEffect`) for glass refraction. It's built first, inside the
`HaloToggle` vertical slice.

## Problem
Today one `Modifier.depth(level)` call draws the fill, hairline edge and top light catch in a single pass on
the element's node. Grain is a separate modifier, but it draws into the same layer as the element's content.
That causes three problems:

1. **Surface and element are fused.** Animating the surface (moving light, flowing texture, a sheen sweep)
   re-records the element's draw, and transforming the element (press scale, collapse) transforms a surface
   baked into the same layer.
2. **Only 3–4 effective layers** (fill, edge, light catch, optional grain/glass). There is no slot for
   occlusion, micro-surface (texture/normal), translucency, specular lighting or emission as independent concerns.
3. **No shared light.** Each surface fakes its own "top light", so surfaces can't react coherently to one light
   source, which is what makes things read as physical.

## References (`Refs/`)
- **`danger button.webp`:**
  - a red gel pill recessed into soft-touch matte black
  - glowing (emissive) label, inner glow, specular top highlight
  - a bevelled inset bezel and a contact shadow at the recess
- **`nav bar button orange.webp`:**
  - a glossy orange glass orb: subsurface glow, refraction, specular highlight, emissive icon, contact shadow on the bar
  - a matte bar with a bevel edge
  - a **wood** texture strip with a recessed slot
- **`card.webp`:** frosted glass cards (light and dark) over a grainy wall lit by window light (gobo shadows), with soft contact shadows.
- **`card.mp4`, `toggle.mp4`, `3d graph immersive.mp4`:** motion references, to be reviewed at the start of this phase (frame-by-frame).

**What the references imply:** a real-world look needs **occlusion**: contact shadows and ambient occlusion
*caused by the scene light*. It also needs translucency (gel and glass), emissive content and true bevels/insets.
Today's principle "depth from light, never shadow" must evolve into **"everything is physically lit: shadows
exist only as consequences of the scene light (contact/AO), never as decorative drop shadows."** This needs
your sign-off (see Open questions).

## Proposal: `HaloMaterial`
Each surface is composited from **eight ordered layers**. Every layer lives in its own `GraphicsLayer`
(RenderNode), records only when its own inputs change, and composites in order:

| # | Layer | Responsibility | Typical animation (independent of the element) |
|---|---|---|---|
| 0 | **Occlusion** | Contact shadow + ambient occlusion cast onto what's beneath, from the scene light (direction, height, softness) | light direction, element "height" (press = lower) |
| 1 | **Emission** | Light the surface gives off, outside its bounds (today's glow) | intensity, colour, spread |
| 2 | **Base** | Tone / fill / gradient, plus translucency and subsurface for gel/glass (glass = backdrop blur + tint + refraction) | colour, density, scattering |
| 3 | **Texture** | Micro-surface: soft-touch matte, grain, frosted, brushed metal, wood, carbon, paper. Procedural AGSL (or bitmap) that also outputs a **normal field** | phase / offset / scale (flowing, shimmering) |
| 4 | **Lighting** | Diffuse + specular (GGX) + Fresnel rim + caustics for glass, per pixel in AGSL from the texture normals and the **scene light**: the "ray-traced" look | light position (tilt / touch), roughness |
| 5 | **Edge** | Bevel / inset rim with inner shadow and light catch, angle-aware to the scene light | light angle, depth, width |
| 6 | **Content** | The element's children (`drawContent()`), with optional **content emission** (glowing label/icon, as in the references) | element-owned (press, collapse); glow intensity |
| 7 | **Interaction** | Press highlight, focus ring (`HaloIndication`) | interaction state |

### Building blocks
- **`HaloMaterial`** (`@Immutable`): `base`, `texture: HaloTexture?`, `finish` (roughness, metalness, specular
  intensity), `edge`, `emission`. Ships as **presets**, e.g. `Matte`, `Satin`, `Glass`, `BrushedMetal`, `Paper`.
  Today's `HaloDepth` L0–L3 becomes four presets, so existing components don't change.
- **`HaloLight`**: one scene light per screen, provided through a CompositionLocal (direction, colour,
  intensity). Sources: a static key light (default), **device tilt** (rotation-vector sensor, ~30 Hz,
  spring-smoothed, active only while visible), or a **touch point**. Every surface on screen reads the same light,
  so highlights move together like real objects.
- **`MaterialState`** (`@Stable`): animatable per-layer parameters (texture phase, sheen progress, light
  override). This makes **surface-only animation** a first-class operation. Example: the HoldToSend fill's
  texture flows while charging and a specular sheen sweeps on success, while the content layer is never re-recorded.
- **`Modifier.material(material, shape, state)`**: a `Modifier.Node` that owns the layer RenderNodes via
  `requireGraphicsContext()` and composites them around `drawContent()`. Element transforms (press scale) wrap
  the whole stack; surface parameters invalidate only their own layer.

### Performance & accessibility
- Shaders are compiled once per material type, with uniforms updated per frame. Only the lighting layer re-renders
  when the light moves; it is a single fragment pass over the shape's bounds.
- **Reduced motion:** static light, no texture flow.
- **Power saver:** the tilt source is disabled.
- Layers outside the screen or fully covered are skipped.

### Showroom & skill
- New **Materials** page: a preset gallery, a draggable light and a tilt toggle, and **solo/mute per layer** for
  pixel review. The debug dock gets a "layers" control.
- The `halo-component` skill gains `references/materials.md`. Its rule: components choose a material preset and
  drive `MaterialState`; they never hand-draw lighting or texture.

## Phasing
0. **Two themes + colour semantics** (D1, D8):
   - the light palette and the Lightning cyan family as tokens
   - a theme preference (System / Dark / Light) via a settings repository in the domain/data layers
   - Showroom pages render in both themes
   - `HaloToggle` built through the skill (D2) and used as the theme switch
   - existing components re-verified in light
1. **Layer engine:** `Modifier.material`, `HaloLight` (static), and migrating `HaloDepth` to presets with zero visual
   change, plus per-layer debug toggles.
2. **Texture library:** AGSL soft-touch matte, grain, frosted, brushed, carbon, paper and wood, each with normal output.
3. **Lighting & occlusion:**
   - diffuse, specular (GGX), Fresnel and caustics from the shared light
   - contact shadow and AO from the same light
   - tilt and touch light sources
   - gel/glass translucency (the reference orb and danger button)
4. **Apply:** balance hero card (satin + tilt specular), HoldToSend (texture flows while charging, sheen on
   success), Glass L3 (refraction + rim light).

## Open questions
Questions 0–2 were resolved by D1–D8. Still open:
3. Should the texture layer animate continuously anywhere (e.g. idle shimmer on the hero card), or only in
   response to interaction? Default until decided: interaction-driven only.
