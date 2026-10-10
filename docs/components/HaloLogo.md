# HaloLogo

> The Halo mark as a glass tube bent into a ring. A bead of lime light launches from where the splash's satellite
> sits, runs once round the ring with the glass carrying its glow, and settles back where it started. It should
> feel like a real object on the card: lit by the same light, casting the same kind of shadow.

| Archetype | Status | Showroom | File |
|---|---|---|---|
| Feedback (brand mark) | Draft | `halo://showroom/logo` | `core/designsystem/.../component/HaloLogo.kt` |

> **Work in progress.** The code currently renders a quarter-filled tube of radium-like liquid (iteration 2). It is
> being reworked into a fully filled tube whose liquid circulates continuously, with real fluid physics: laminar
> (Poiseuille) flow with a Dean shift in the bend, scintillating phosphor grains on their streamlines, viscous
> relaxation, tap impulses and gyroscope inertia. The sections below still describe iteration 1 (a single bead of
> light) and will be rewritten with iteration 3.

**Design read:** `HaloLogo · Feedback (brand mark) · signs the app · a bead of lime light launches from the splash satellite's spot, runs once round a glass ring with the glass carrying its glow, and settles back where it started`

**Assumptions:**
- The mark keeps the splash geometry (`splash_halo.xml`): a ring with its satellite at the top right (−45°). The
  satellite becomes the bead of light inside the glass, so the splash and the in-app mark tell one story.
- The light is the accent (lime) in both themes, like the splash mark on dark. On light the glass is clear and the
  lime light carries the mark.
- **No idle loop** (Apple motion guidance: motion has a purpose and never competes with content). The mark plays
  once when it appears and again when tapped, then rests. At rest nothing renders.
- Refraction is of the surface the mark rests on: the shader renders that floor (its token colour, the tube's own
  shadow, caustic and spilled light) at the refracted position, so what you see through the glass is consistent.
  The mark never samples live pixels behind it (it sits inside the screen's backdrop source).

## 1. Usage
- **Use for:** brand moments: the home hero next to the HALO wordmark, an about screen, an empty state header.
- **Don't use for:** loading (use a skeleton), status (use the status colours), or as a button icon (use `HaloIcons`).
- **Where it rests:** on `bg.raised` hero surfaces by default (`surfaceColor` tells it what is underneath).

## 2. Anatomy
All proportions are of the mark's box W (`logoMd 32`, `logoLg 56`).
```
 ┌──────────── box W × W ────────────┐
 │        ╭───── glass tube ─────╮    centreline radius 0.34W, tube radius 0.075W (Ø 8.4dp at 56)
 │      ╭─╯   ● bead (−45° rest)  ╰─╮  bead: hot core, radius ≈ 0.55 × tube, lime burning to white
 │      │      floor seen through  │  refraction: the floor under the tube, flipped and magnified (×0.6)
 │      ╰─╮   the glass           ╭─╯  Fresnel rim + specular from the key light (top-left)
 │        ╰──────────────────────╯    contact shadow, offset by the light from height = tube radius,
 └───────────────────────────────────┘  glass lets ~half the light through; a caustic line sits inside it
 Drawn into the box inflated by 2 × tube radius, so shadow and glow are never clipped.
 Touch target ≥ 48dp (logoMd is padded).
```

## 3. API
```kotlin
enum class HaloLogoSize { Medium, Large }

@Composable
fun HaloLogo(
    modifier: Modifier = Modifier,
    size: HaloLogoSize = HaloLogoSize.Large,
    animated: Boolean = true,
    surfaceColor: Color = HaloTheme.colors.bg.raised,
    contentDescription: String? = "Halo",
    interactionSource: MutableInteractionSource? = null,
)

/** The mark frozen at one moment of its motion: state matrices, previews, static screenshots. */
@Composable
fun HaloLogoFrame(
    beadAngle: Float,
    beadSpeed: Float,
    glow: Float,
    modifier: Modifier = Modifier,
    size: HaloLogoSize = HaloLogoSize.Large,
    surfaceColor: Color = HaloTheme.colors.bg.raised,
)
```

## 4. Variants × sizes
| Theme | Glass (`materials.glass`) | Light | Shadow |
|---|---|---|---|
| Dark | smoky graphite tint (Ink700), absorption 0.55, environment Ink600 | `accent.fill` (volt) | black, `light.shadowOpacity` |
| Light | cool clear tint (Slate400), absorption 0.22, environment Slate500 | `accent.fill` | cool blue-grey (`light.shadow`) |

| Size | Box | Tube Ø | Touch target |
|---|---|---|---|
| Medium | 32 | 4.8dp | 48 (padded) |
| Large | 56 | 8.4dp | 56 |

## 5. State matrix
| State | Visual delta | Motion | Haptic | Semantics |
|---|---|---|---|---|
| Unlit (first frame) | clear glass, no light | — | — | — |
| Igniting | the bead lights at −45° | glow 0 → 1, Gentle | — | — |
| Orbiting | the bead runs clockwise with a comet tail that grows with speed; the glass glows where it passed | angle +2π, Orbit with kick | — | — |
| Rest | bead at −45°, soft symmetric glow either side, faint light piped all round | — | — | Image "Halo" |
| Pressed | the light ignites +60% (the mark's touch response is its light, D5) and the mark sinks to 0.97 | Snap / Quick (press progress) | — | — |
| Tapped | one more revolution from wherever the bead is, momentum kept | Orbit, velocity = max(current, kick) | Tick | action "Replay the halo" |
| Static (`animated = false`) | rest state, not pressable | — | — | Image "Halo" |
| Reduced motion | rest state; a tap gives the press ignite only | springs snap | Tick | same |

## 6. Tokens used
- **Sizes:** `logoMd`, `logoLg` (new), `minTouch`.
- **Colors:** `accent.fill` (light), `bg.raised` (default surface).
- **Light:** `HaloTheme.light` (direction, highlight, shadow tone, shadow opacity, shadow offset per height).
- **Materials:** `materials.glass` (new `GlassMaterial`).
- **Motion:** `Orbit` (new), `Gentle`; press progress (Snap / Quick).
- **Haptics:** `Tick`.
- **New tokens:**
  - `HaloSizes.logoMd` / `logoLg`: the mark needs its own box; icon sizes are too small for glass detail.
  - `HaloSpring.Orbit (1.0, 12)`: Apple's smooth spring at ~1.8s. `Gentle` is too fast for a full revolution,
    `Tide` too slow for an object this small.
  - `GlassMaterial` + `HaloMaterials.glass`: components pick materials by role; glass needs absorption,
    Fresnel and an environment, which `ReliefMaterial` doesn't have.

## 7. Motion choreography
| Trigger | Property | From → To | Spec | Notes |
|---|---|---|---|---|
| appear | glow | 0 → 1 | Gentle | the bead ignites where the splash satellite was |
| appear | bead angle | −45° → −45° + 360° | Orbit, launched with v₀ = ½·ω·2π | leaves at full speed, only decelerates |
| any bead motion | comet tail | length ∝ angular speed | draw phase | the glass keeps glowing where the bead has just been |
| press | light | × (1 + 0.6·press) | press progress (Snap in, Quick out) | D5: emission ramps up (+60%, `card.mp4`) |
| press | scale | 1 → 0.97 | via HaloPressable | |
| tap | bead angle | current → target + 360° | Orbit, v = max(v, kick) | interruptible: a tap mid-orbit adds a revolution |
| settle | bead angle | wrapped to −45° | snap (invisible) | keeps the angle bounded |

All animated values are read in the draw phase: zero recomposition per frame. Nothing animates at rest.

## 8. Haptics
| Moment | Token | Timing |
|---|---|---|
| tap | Tick | on click (a light acknowledgement, not a button `Tap`) |

## 9. Gestures
Tap only (`HaloPressable`). No drag: the mark lives in scrolling content and must never steal a scroll.

## 10. Accessibility
- `contentDescription` (default "Halo"). Animated marks are a `Role.Button` with the click label "Replay the
  halo"; static marks are `Role.Image`.
- Touch target ≥ 48dp.
- No text, so font scale doesn't apply. RTL: the orbit stays clockwise and the light stays top-left; the light is
  world-fixed and the mark is a logo, not a direction.

## 11. Reduced motion
- No ignite, no orbit: the mark appears at rest, lit.
- A tap still acknowledges with the press ignite and the haptic; the bead doesn't travel.

## 12. Edge cases
- Rapid taps: each adds a revolution to the current target, with the velocity kept (never a jump).
- Recomposed or scrolled away mid-orbit: the animation keeps its state; leaving composition cancels it.
- Theme switch mid-orbit: materials and light change underneath; the motion continues.
- `surfaceColor` that doesn't match what's underneath: the inside of the tube shows the given colour. Pass the
  real surface colour.

## 13. Verification
- Lint: `lint_ds.py HaloLogo`.
- Screenshots: `shots/logo-dark.png`, `shots/logo-light.png` (state matrix), `shots/home-logo.png` (hero), plus a
  real-speed recording of the orbit reviewed frame by frame.
- Zoomed pixel review of the tube edges, highlight, caustic and shadow in both themes (`halo.py zoom`).
- Unverified: how the Tick haptic feels (on device); 120 Hz smoothness (emulator).
