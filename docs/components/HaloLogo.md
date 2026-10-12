# HaloLogo

> The Halo mark as a glass tube bent into a ring, filled with a self-luminous, radium-like liquid that circulates
> continuously and behaves like a real fluid. It should feel like an object on the card: lit by the same light,
> casting the same kind of shadow, and answering touch and motion the way liquid in glass would.

| Archetype | Status | Showroom | File |
|---|---|---|---|
| Feedback (brand mark) | Review | `halo://showroom/logo` | `core/designsystem/.../component/HaloLogo.kt` |

**Design read:** `HaloLogo · Feedback (brand mark) · signs the app · radium-like liquid circulating in a glass ring, its grains scintillating on their streamlines; a tap pushes it, turning the phone makes it lag behind the glass`

**Assumptions:**
- Geometry follows the splash ring (`splash_halo.xml`), scaled up so the glass has room for detail.
- The liquid is the accent (brand blue) in both themes, like the splash mark on dark.
- Continuous motion is wanted (user direction). It is made responsible: it runs only while the mark is on screen,
  the app is in the foreground and battery saver is off, at ~30 fps when calm; reduced motion shows it still.
- Refraction is of the surface the mark rests on (rendered from `surfaceColor`, the tube's shadow and caustic), never
  live pixels: the mark sits inside the screen's backdrop source.
- "Real physics" means physically based models, kept cheap: first-order viscous dynamics, a laminar velocity
  profile, Poisson scintillation, Beer–Lambert, Fresnel, light-caused shadow. Not a fluid simulation.

## 1. Usage
- **Use for:** brand moments: the home hero next to the HALO wordmark, an about screen, an empty-state header.
- **Don't use for:** loading (use a skeleton), status (status colours), or as an icon (`HaloIcons`).
- At most one animated mark per screen; extra marks pass `animated = false`.

## 2. Anatomy
All proportions are of the box W (`logoMd 32`, `logoLg 56`).
```
 ring centre-line radius 0.36W · tube radius 0.058W (Ø 6.5dp at 56) · bore 78% of the tube
 glass wall: floor through a cylindrical lens (×0.6, flipped), Beer–Lambert absorption, piped light along the walls
 liquid (fills the bore): soft radium glow, denser at the core; luminous clouds carried at the mean flow;
   phosphor grains on 7 streamlines × 2 depth layers, 16 per streamline, each scintillating
 Fresnel rim (pixel-averaged at the edges) + studio window + tight key-light highlight (top-left)
 contact shadow from the key light at height = tube radius, ~half transmitted, caustic line inside it; light spill
 Drawn into the box inflated by 2.5 tube radii so shadow and glow are never clipped. Touch target ≥ 48dp.
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

/** The mark frozen at one moment: state matrices, previews, static screenshots. Decorative. */
@Composable
fun HaloLogoFrame(
    flowRate: Float,          // degrees per second
    glow: Float,              // 0 unlit · 1 glowing · up to 1.6 pressed
    modifier: Modifier = Modifier,
    size: HaloLogoSize = HaloLogoSize.Large,
    surfaceColor: Color = HaloTheme.colors.bg.raised,
)
```

## 4. Variants × sizes
| Theme | Glass (`materials.glass`) | Liquid | Shadow |
|---|---|---|---|
| Dark | smoky (Ink950 tint, absorption 0.8, env Ink500) | `accent.fill`, core burns towards white | black, `light.shadowOpacity` |
| Light | clear (Slate400 tint, absorption 0.12, env Slate400) | `accent.fill`, stays saturated | cool blue-grey |

| Size | Box | Tube Ø | Touch target |
|---|---|---|---|
| Medium | 32 | 3.7dp | 48 (padded) |
| Large | 56 | 6.5dp | 56 |

## 5. State matrix
| State | Visual | Motion | Haptic | Semantics |
|---|---|---|---|---|
| Unlit (appear) | liquid dimmed, glass clear | glow 0 → 1, Gentle | — | — |
| Steady | soft glow, sparse twinkles drifting clockwise, core overtaking the walls | drive 0.35 rad/s | — | Button "Halo" |
| Pushed | fast circulation; the profile's peak moves towards the outer wall (Dean) | +6 rad/s, viscous decay τ 1.4 s | Ping | action "Push the liquid" |
| Phone turning | liquid lags behind the glass, then catches up | gyroscope, viscous coupling | — | — |
| Pressed | liquid glows +60% (D5), mark sinks to 0.97 | press progress | — | — |
| Off screen / background / battery saver | frozen, no frames | — | — | — |
| Static (`animated = false`) or reduced motion | still, glowing | none | Ping (tap only when animated) | Image "Halo" |

## 6. Tokens used
- **Sizes:** `logoMd`, `logoLg` (new), `minTouch`.
- **Colors:** `accent.fill`, `bg.raised` (default surface).
- **Light:** `HaloTheme.light`. **Materials:** `materials.glass` (new `GlassMaterial`).
- **Motion:** `Gentle` (ignite); press progress. Physics constants live in the component (drive, viscosity,
  impulse): they are physical parameters, not design tokens.
- **Haptics:** `Ping` (new: glass tink).

## 7. Motion choreography (physics)
| Trigger | Model | Values |
|---|---|---|
| appear | glow 0 → 1 (Gentle) + one impulse | liquid kicked round once, then settles to the drive |
| always | lab-frame fluid angular velocity W relaxes to (glass spin + drive): `W += (Ω + D − W)(1 − e^(−dt/τ))` | D 0.35 rad/s (lap ≈ 18 s), τ 1.4 s |
| tap | W += impulse | 6 rad/s → ≈ impulse × τ = 8.4 rad extra |
| phone rotation | Ω from the gyroscope (−z, ~30 Hz); shown motion = W − Ω | liquid lags, then is dragged along |
| across the bore | Poiseuille profile, no-slip at both walls, peak shifted outwards by δ = 0.12 + min(0.03·|rate|, 0.2) | streamline speed = 1.5 × profile × mean |
| grains | Poisson scintillation, ~1.5 flashes/s, decay ~80 ms; 6% between flashes | |
| frame rate | every frame while not calm; ~30 fps when calm (no state write, no redraw) | calm = |rate − drive| < 0.05, glow settled, not pressed |

Animated values are read only in the draw phase: zero recomposition per frame. Slow-mo scales the physics clock.

## 8. Haptics
| Moment | Token | Timing |
|---|---|---|
| tap | Ping | on click: a sharp light tap and a short glass tink (fixed resonance, damped by the liquid) |

## 9. Gestures
Tap only (`HaloPressable`, no highlight overlay: the response is the mark's own light). No drag: the mark lives in
scrolling content and must never steal a scroll. The phone's rotation is the second input.

## 10. Accessibility
- `contentDescription` (default "Halo"). Animated: `Role.Button`, click label "Push the liquid". Static: `Role.Image`.
- Touch target ≥ 48dp. No text. RTL: the flow stays clockwise and the light stays top-left (world-fixed).

## 11. Reduced motion
No physics clock, no sensor, no impulse: a still, glowing tube. A tap still gives the press glow and the haptic.

## 12. Edge cases
- Rapid taps: impulses add up; viscosity bleeds them off. No cap needed (decay is exponential).
- Dropped frames: physics steps are capped at 50 ms, so the liquid never jumps.
- Scrolled away mid-push: the clock stops, and the state resumes where it was.
- No gyroscope: the glass is treated as still.
- `surfaceColor` that doesn't match what's underneath: the tube shows the given colour. Pass the real surface.

## 13. Verification
- Lint: OK. Unit tests: `FluidFlowTest` (spin-up to the drive in τ, impulse decay and distance = impulse × τ,
  rotating-container lag and catch-up).
- Screenshots: `shots/logo-dark.png`, `shots/logo-light.png`, zoomed pixel review of tube, grains and shadow.
- Frame budget (emulator, 5 s): hero visible 180 frames; scrolled off screen 1 (the demo balance); app in
  background 0; reduced motion 0.
- Unverified on the emulator: how the motion and the gyroscope lag feel at real speed (needs a device), the Ping
  haptic, 120 Hz smoothness.
