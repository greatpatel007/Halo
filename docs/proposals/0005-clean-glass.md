# Proposal 0005: Clean glass, one material family for the whole UI

**Status:** Revising (2026-10-13): after research 0004 the owner set the rule *Android's guides first, Apple fills the gaps, motion follows Apple only*. The revision below supersedes the earlier "glass everywhere" model; the sections after it are kept for history, proven in the Showroom **Surface lab** (`halo://showroom/surface`,
`feature/showroom/.../pages/SurfacePage.kt` + `GlassLab.kt`). Nothing is in the design system yet. Supersedes the
*surface* half of [0001](0001-material-surface-stack.md). 0001's decisions D1 (two themes), D3 (shadows only from the
scene light), D4 (one key light, top-left), D5 (touch-reactive light), D6 (tilt) and D8 (colour meaning) stay.
**Raised by:** product direction: "the whole UI should feel similar but unique in how it presents information, and
most important is surface: we are missing surface."

## Revision (2026-10-13): Android-first layering
Proven in the Surface lab (`NativeLab.kt`).

| Layer | Rule | Source |
|---|---|---|
| **Content** (balance, lists, settings, actions) | Opaque **tonal** containers on the grain base: a tone ladder (canvas < surface < raised), no blur, no rims. Hierarchy by tone; the hero takes the highest tone | Material 3 tone-based surfaces |
| **Floating layer** (toolbar/tab bar, top bar, sheets, menus, toasts) | The only glass: backdrop blur paired with a translucent tint (less dim because blurred), a lit 1 px rim, a short contact shadow, and **edge lensing** (the rim refracts what lies just outside it; AGSL, Android 13+, blur-only on 12, frosted fill below) | AOSP window blur + Material 3 Expressive floating toolbar; lensing from Apple |
| **Controls** | Quiet and tonal at rest; while touched they lift into frosted glass, grow slightly and light from the finger. The switch handle stays a frosted disc at rest (owner's call) | Apple (controls become glass during interaction) |
| **Colour** | Only for the key action (one larger, filled key: Send) and real meaning (network); everything else neutral, labels full contrast | Material 3 Expressive emphasis; Apple "tint only the primary" |
| **Motion** | Apple's fluid interfaces only: response first, springs, interruptible | Owner |
| **Platform** | Honour the contrast setting, battery saver (blur off → designed fallback), remove-animations, font scale | Android 14+ / AOSP |

### Material 3 Expressive foundations (2026-10-13)
Proven in the lab (`MaterialLab.kt`, values from the androidx material3 token sources):
- **Colour roles:** Material's full role set (primary / secondary / tertiary / error families with containers, the
  surface ladder `surface`, `surfaceDim/Bright`, `surfaceContainerLowest…Highest`, `onSurface(Variant)`,
  `outline(Variant)`, inverses, scrim) at Material's exact tones (tone = CIELAB L*), hue and chroma from Halo's seeds.
  Halo extensions: the brand blue is kept exactly as **primaryContainer** (fidelity; the key action), and on-chain,
  Lightning and positive are **extended families**. Network families also keep their true seed as `fill` for lit
  controls (Material's tone 40 turns Lightning yellow olive); text uses the readable tone.
- **Type:** Material's 15 roles (display → label) + the Expressive emphasized variants, in Satoshi; numbers stay Paper
  Mono (Halo rule). Sentence-case titles replace all-caps section labels.
- **Navigation:** the flexible (short) navigation bar: 64 dp, docked, 24 dp icon over Label Medium, a 56 × 32 dp pill
  indicator in secondaryContainer (6 dp to the label), active label in secondary, inactive in onSurfaceVariant. Content
  scrolls beneath it, so its surfaceContainer is translucent over a blur (Android 16/17 system style).
- **Components:** segmented = secondary (or meaning) container on surfaceContainerHighest, Label Large; switch = family
  fill when on, surfaceContainerHighest + outline when off; tonal keys = secondaryContainer, key action = primaryContainer.

## Problem
The controls became physical objects (lit relief, contact shadows, sunken tracks), but everything they sit on stayed
flat: `Modifier.depth` draws a tone step plus a hairline. Real knobs sat on flat rectangles, so the UI read as two
styles. The material engine planned in 0001 (layers, textures, GGX lighting) was never built for surfaces.

## How we got here (rejected directions)
| Tried | Why it was rejected |
|---|---|
| **One machined body:** plateaus, wells and engraved grooves in the same soft-touch/ceramic material | Same colour for body and shapes, soft wide bevels and soft drop shadows everywhere: it read as *badly done neumorphism*. Subtler values couldn't fix the recipe |
| **Smoked glass** panes plus the existing relief controls | The panes worked, but the relief controls (domed keys, ceramic/graphite tab and switch) clashed with the glass: "especially the tab" |
| **Clean glass** for panes *and* controls | **Chosen.** One material family, hierarchy from material and light instead of extrusion |

## Decision: four materials, each with one job
| Material | What it is | Used for | How it reads |
|---|---|---|---|
| **Base** | Flat soft-touch (dark) or ceramic (light). Fine grain, **no relief** | The screen | A soft pool of room light from the key light's side; the only depth is light falloff |
| **Pane** (thin glass, 2–3 mm) | Frosted backdrop blur, a whisper of tint | Cards, lists, sheets, bars, the dock | A crisp 1 px specular line on the lit edge, a faint far rim (internal reflection), a broad sheen from the lit side, a short tight contact shadow. On light, the silhouette comes from the shadow side |
| **Solid glass** (thick) | A glass piece with a body | The halo mark | A brighter body, a specular band entering from the lit edge, a lit rim, a **caustic** focused onto what lies beneath (away from the light), and its **state as coloured light inside it** |
| **Frosted glass** (discs) | Milky, matte glass, no gloss | Every control: tab thumbs, toggle discs, round keys | Diffuses the light beneath it (state colour blooms through the whole disc), a soft lit/far gradient for volume, an even rim a touch brighter towards the light |
| **Inset glass** | A groove sunk into a pane | Tab tracks, toggle grooves, inputs | Darker, shaded on its lit wall, its far lip catching the light. A groove can hold light (the toggle's on-state) |
| **Emitted light** | Not a material | Data and state: chart filaments, liquid, the logo, the colour inside solid glass | Carries meaning (blue = act, orange = on-chain, yellow = Lightning) and glows *through* the glass. Boosted on HDR |

**Why this isn't neumorphism:** edges are crisp (≤ 1 px highlights), never soft extrusions; hierarchy comes from
*which material* something is, not how far it sticks out; and the materials contrast (matte vs glass vs light).

**Why it's unique:** most apps use flat cards or Apple's clear Liquid Glass. Halo's glass is physically lit by one
world-fixed light that turns with tilt, frosts real light sitting behind it (the halo's own glow), and shows state
and data as coloured light *inside* the glass. Information reads like an instrument.

## Rules
1. **Nothing is extruded except solid glass.** No plateaus, wells or bevelled cards. Panes lie flat on the base.
2. **Every edge is a light event.** Rims, specular lines and caustics follow `HaloTheme.light` (read in draw), so a tilt
   moves them all together. No decorative strokes or drop shadows.
3. **Colour is light.** State shows its meaning colour as light: a glow inside solid glass (a selected tab), or a fully
   lit body (an on switch's groove, the primary button). Never a translucent colour wash: over a dark floor it turns
   muddy. Labels take the colour by proximity (draw phase).
4. **Glass needs something to frost.** Each screen has one backdrop: the base plus the light resting on it (the halo
   glow, data emission). Glass over nothing turns into a grey card.
5. **One backdrop per screen, panes never nested.** A pane is never another pane's source (no recursion, one blur
   pass per pane). Bars, the dock and sheets frost the screen's content.
6. **Layers stay separate (0001's layering, kept).** Shadow, backdrop, tint, light and content are separate draw
   steps; a moving layer (a light pulse, a thumb, a caustic) redraws alone and never re-records the content.
7. **Light theme is white glass, not smoked.** Same physics; the silhouette comes from the shadow side and the
   caustic is brighter.

## What changes
| Area | Change |
|---|---|
| `foundation/` | New `Modifier.glassPane(...)` (shadow → `backdropGlass` → tint, sheen, rims), and draw helpers `solidGlass(...)` / `insetGlass(...)` in `foundation/material`. `HaloDepth.Surface/Raised` map to panes; `Glass` stays for floating chrome; `depth` stays for compatibility until every component moves |
| `theme/Materials.kt` | `HaloMaterials` gains glass roles: `pane` (frost, tint, sheen, rim lit/mid/far, shadow), `solid` (body, specular, rim, caustic, inner light), `inset` (floor, wall shade, lip). Values per theme; dark = smoked, light = white |
| `HaloSegmentedControl` | Frosted-glass thumb in an inset-glass track; the selected meaning colour blooms through the frost, and the label under the thumb goes full contrast (the thumb carries the colour). Physics, haptics, drag and API unchanged |
| `HaloToggle` / `HaloThemeSwitch` | Solid-glass bead in an inset groove. **Off:** an empty dark groove, a smoky bead. **On:** the groove becomes lit coloured glass at the full meaning colour (opaque, never a translucent wash, which turns olive over a dark floor) and the bead is a frosted glass disc (the Nothing look: milky, matte, no gloss streak) that diffuses the colour beneath it, paler than the groove so it still reads as the knob. State reads by colour, brightness *and* position. Mechanical travel, lean, haptics, Tide transition unchanged |
| `HaloButton` | Primary keeps its emitted accent fill (it *is* light); Secondary becomes a solid-glass pill; Ghost unchanged |
| `HaloSheet`, top bar, dock | Panes (they already frost; they gain the lit edge and shadow) |
| `HaloLogo` | Already glass; its glow becomes the backdrop light glass frosts |
| `HaloHoldToSend` | Track = inset glass; the charge is light filling the groove |
| Planned components | Built on these materials from the start (keypad keys = solid glass, text field = inset glass, chart = emitted light inside a pane, list rows on panes) |
| Relief (`ReliefPainter`) | Kept for the base's grain and contact shadows; no longer used for extruded bodies |

## Fallbacks, power and accessibility
- **Android 8–11 (no blur):** panes use the existing frosted fill under the same tint, rims and shadow. Solid and inset
  glass are plain canvas brushes and need no shaders, so they look the same on every phone.
- **Conserving power:** the backdrop is recorded each frame while something under it moves. Static screens record
  once; continuous light under glass (the chart pulse) follows the runtime's policy.
- **Contrast:** text on panes keeps AA in both themes at the strongest backdrop light; the tint has a floor. A future
  "reduce transparency" setting turns panes opaque (`bg.raised` + rims), as Apple does.
- **Reduced motion:** caustics and inner light are static; thumbs snap.

## Phasing
1. **Tokens and foundation:** glass roles in `HaloMaterials`, `glassPane`, `solidGlass`, `insetGlass`; lift the lab code
   into the design system. Lint rules for the new tokens. Unit tests where there's a model (caustic/lean positions).
2. **Controls:** rebuild `HaloSegmentedControl` and `HaloToggle` (+ `HaloThemeSwitch`) on solid/inset glass;
   re-verify dark/light/legacy, drag, haptics.
3. **Surfaces:** panes for sheets, top bar, dock and Showroom cards; `HaloButton` Secondary; `HaloHoldToSend` track.
4. **Skill:** `halo-component` gains `references/glass.md` (the four materials, the rules above, the recipes). Every
   new component chooses a material; none hand-draws its own surface.
5. **Then** resume the component build order (keypad, text field, …) on glass, and the Home proof as the first demo screen.

## Decisions (2026-10-13)
The owner delegated these; each is decided as a product designer would, with the reason.

| # | Question | Decision | Why |
|---|---|---|---|
| 1 | Default tuning | Ship the lab's 1× values as the token defaults; keep the tuning knobs in the Showroom for later passes | They were designed as the defaults and read well in both themes |
| 2 | Soft-touch vs ceramic | **One finish per theme:** soft-touch on dark, ceramic on light. The lab switch stays as a lab tool only | Under glass the difference is barely visible; a user setting would add a choice nobody can see |
| 3 | Primary button | **Stays emitted light:** a fully lit blue body (the one loud thing per screen). Secondary becomes a solid-glass pill | A glass pill would weaken the main call to action; "a lit body" already follows rule 3 |
| 4 | Light on white | **Solved by the lit-body rule:** on-states use an opaque lit fill at the meaning colour, which reads on white as well as on black. Filaments and glows are kept for data (charts), where a dark pane sits behind them | A glow can't show on white; a lit body can |
| 5 | Backdrop light | **One light source per hero screen, never decorative.** Home: the halo's blue behind the balance. Receive: the network's colour behind the QR (orange on-chain, yellow Lightning). Payment success: the network colour. Lists and settings have none | Glass needs something to frost, but light must still mean something (D8) |
| 6 | Toggle | **Glass only. On = lit coloured groove + a frosted disc glowing with it; off = dark groove + a grey frosted disc** (see "What changes") | The first glass toggle signalled state only by position and a thin line; a switch must read in a glance |
