---
name: halo-component
description: Builds a production-grade, hand-made Jetpack Compose component for the Halo Bitcoin-wallet design system from a written requirement — spec, implementation, physics-based motion choreography, designed haptics and sound, world-fixed lighting and HDR emission, accessibility, Showroom page and registry entry, verified on a device screenshot. Use whenever the user asks to create, build, design or add a UI component, control, micro-interaction or animated element to Halo (e.g. "build a hold-to-send button", "add an amount keypad", "make the balance roll like an odometer"), or invokes /halo-component.
argument-hint: "[ComponentName] [requirements…]"
---

# Halo component forge

You turn a requirement into a **finished design-system component**. Its bar is "2027-grade fintech app on
Android": pixel-exact, physically animated, tactile, audible, lit by one world-fixed light, accessible and
unmistakably native. Every component is built **from scratch** on Compose foundations and Halo tokens.

Every component is a *sensory* object, not just a picture: it moves by real physics (Apple's fluid-interface
guidance), it answers touch with a designed haptic and its sound, its highlights and shadows follow the device's
tilt, and anything that emits light glows past white on HDR screens. All of that is in the non-negotiables below.

## The contract (all six or it isn't done)

| # | Artifact | Path |
|---|----------|------|
| 1 | Spec | `docs/components/<Name>.md` (from `templates/ComponentSpec.md`) |
| 2 | Component + previews | `core/designsystem/src/main/kotlin/com/halo/core/designsystem/component/<Name>.kt` |
| 3 | Showroom page | `feature/showroom/src/main/kotlin/com/halo/feature/showroom/pages/components/<Name>Page.kt` |
| 4 | Catalog entry | `ShowroomCatalog.kt`: one `ShowroomPage("<id>", …, ShowroomSection.Components)` line under the marker comment |
| 5 | Registry row | `docs/components/REGISTRY.md` |
| 6 | Verified screenshot | `shots/<id>.png` plus a written review against the checklists |

`<Name>` is the Kotlin file name and matches the main composable, e.g. `HaloButton`.

## Non-negotiables

1. **Foundations only.** Use Compose `ui`/`foundation`/`animation`/`graphics` and `com.halo.core.designsystem.foundation.*`.
   No Material/Material3, no ripple, no Lottie/Haze/Accompanist or any other third-party UI library.
2. **Tokens only.** All values come from the theme:
   - colors: `HaloTheme.colors`, by semantic role
   - type: `HaloTheme.typography`
   - layout: `.spacing`, `.sizes`, `.radius`, `.stroke` and `.effects`
   - motion: `HaloTheme.motion.spring(HaloSpring.X)`, `.fade()` and `.timed()`
   - haptics: `HaloTheme.haptics.perform(HaloHaptic.X)`

   If a value you need doesn't exist, **add a token**: edit `theme/*.kt` and `references/tokens.md`, and say so in
   the report. Never hard-code a literal. The lint enforces this.
3. **Stateless and hoisted.** State comes in and events go out. `modifier: Modifier = Modifier` is the first
   optional parameter, content slots come last, and interactive components accept
   `interactionSource: MutableInteractionSource? = null`. No ViewModels, repositories or `:core:model` types: the
   design system is pure UI and takes primitives such as `Long` sats.
4. **Every press goes through `HaloPressable`.** It handles press scale, the touch-origin highlight, the haptic and
   semantics in one place. Custom gestures must reproduce all four duties.
5. **Animate in the draw/layout phase.** Read animated values inside `graphicsLayer {}`, `offset {}`,
   `drawBehind {}` or `drawWithCache {}` lambdas, never as composition arguments, so there is zero recomposition
   per frame and 120 Hz holds.
6. **Interruptible physics.** Use `Animatable` or `animate*AsState` with spring tokens. Everything retargets
   mid-flight, and an animation never blocks input.
7. **Accessible and native.**
   - Semantics: role, `stateDescription`, labels, and custom actions for gestures.
   - 48dp touch targets.
   - Works at 200% font scale, in RTL, and under reduced motion.
   - Predictive back for anything dismissible.
   - Edge-to-edge insets.
8. **Brand rules** (decision record: `docs/proposals/0001-material-surface-stack.md`).
   - Numbers that are values use Paper Mono.
   - Colour carries meaning:
     - lime (`colors.accent`) = UI actions
     - orange (`colors.bitcoin`) = on-chain Bitcoin
     - electric cyan (`colors.lightning`) = Lightning
   - A component that represents a payment takes the network into account.
   - **Both themes.** Every component is built and screenshot-verified in dark *and* light.
   - Lighting is physical: one scene light (key light top-left). Depth comes from tone, rims, emission and
     light-caused shadows (contact/AO). Decorative drop shadows are banned.

9. **Physics first (Apple's motion guidance).** Model motion on the real thing: springs (critically damped for
   mass, underdamped only where a bounce is physical), momentum and viscosity, inertia. The touched control answers
   on the next frame; the consequence grows out of it (cause → effect). If a motion has an analytic trajectory,
   keep it: feedback and tests reuse it. Verify motion with a **real-speed recording** reviewed frame by frame
   (`references/motion-recipes.md` → "Verifying motion"), not only stills.
10. **Sensory feedback is designed with the motion.** Every user action plays one `HaloHaptic` token through
    `HaloTheme.haptics` (it carries both the haptic and its synthesised sound; the engine renders the best tier this
    phone supports and syncs the two). Pick the token whose *physics* matches the moment (click, tick, thud, glass
    ping, rush…). A signature moment that the existing tokens don't capture gets a new pattern in
    `feedback/Patterns.kt` (haptic timeline with intensity/sharpness + sound cue), designed in the spec's Sound &
    haptics section, unit-tested per tier. A continuous effect drives *live* feedback from its own trajectory (the
    Tide is the reference: `feedback/TideFeedback.kt`). Pass `pan` from the control's x for stereo placement. See
    `references/haptics.md`.
11. **One world-fixed light, read in draw.** `HaloTheme.light`'s direction turns with device tilt (`LightRig`), so read
    `light.x/y/z/shadowDx/shadowDy` **only in draw lambdas or shader uniforms**, never in composition: a tilt must
    redraw, not recompose. Shaded surfaces use the relief painters or their own shader with the `light` uniform.
12. **Emitted light uses HDR.** If the component *emits* light (glow, liquid, sparks, foam), scale only the emitted
    part by `HaloTheme.hdr.emission` (read in draw; 1 on SDR) and keep everything else in standard range. Reflected
    light (highlights, surfaces, text) is never boosted. `haloGlow` already does this.
13. **Continuous motion is gated.** Anything that animates without input runs only while it is visible
    (`onGloballyPositioned` + `boundsInWindow`), the lifecycle is ≥ STARTED, battery saver is off and reduced motion
    is off; it drops to ~30 fps when calm (skip state writes) and writes nothing when still. Measure it: frames over
    5 s visible / scrolled away / backgrounded / reduced motion, and report the numbers. Sensors (gyroscope,
    gravity) follow the same rules at ~30 Hz.
14. **Prove the physics.** Pure models (dynamics, trajectories, haptic plans, synthesised sound) live in plain
    Kotlin and get unit tests in `core/designsystem/src/test` (e.g. `FluidFlowTest`, `TideFeedbackTest`,
    `TideClarityTest`). New sounds are measured (spectrum share, stereo correlation, peak), not only listened to.

## Workflow

### 0. Load context (always)
Read `references/tokens.md`, `docs/components/REGISTRY.md` and your archetype's section in `references/archetypes.md`.
Skim `references/motion-recipes.md` for the patterns you'll need.

### 1. Design read
Before the line below, answer in one sentence each: *what real object or material is this, what physics moves it,
what does touching it feel and sound like, does it emit light?* Then state one line: `<Name> · <archetype> · <job> · <signature moment>`. Example:
`HaloHoldToSend · Gesture-commit · confirms a payment with a deliberate hold · the fill charges with a rising haptic and bursts into a check`.

Archetypes: **Pressable, Toggle, Input, Value display, Gesture-commit, Container/Overlay, Feedback.**
Ask **at most one** question, and only if a wrong guess would waste the build. Otherwise write your assumptions into the spec.

### 2. Reuse check
Compose from what exists: foundation primitives and registered components. If an existing component covers 80%
of the request, extend it with a variant instead of creating a sibling.

### 3. Spec
Fill **every** section of `templates/ComponentSpec.md` into `docs/components/<Name>.md`:
- anatomy with dp values
- variants × sizes
- state matrix
- motion table (trigger → property → spec)
- haptics map
- semantics
- edge cases
- reduced-motion fallback

The spec is the source of truth the code and screenshot are checked against.

### 4. Implement
Start from `templates/Component.kt.tmpl`. Keep one component family per file, with private helpers in the same file.
- Resolve colors per variant/state in a private `@Composable` resolver; don't scatter `if`s across the draw code.
- Previews: one `@Preview` per key state, wrapped in `HaloPreview { }`.
- Lift a gesture or effect into `foundation/` only if a second component needs it.
- Feedback: `haptics.perform(token, pan)` on the user action (not on passive updates); new patterns in
  `feedback/Patterns.kt` with tests in `HapticPlanTest`/`SoundSynthTest`; only `feedback/` touches vibration/audio APIs.
- Light & HDR: read `HaloTheme.light` direction and `HaloTheme.hdr.emission` inside draw/shader code only.
- Continuous effects: gate them (non-negotiable 13) and keep their models pure and tested (14).

### 5. Showroom page
Start from `templates/ShowroomPage.kt.tmpl`. It has three blocks:
- **Playground:** the live component plus knobs.
- **State matrix:** every state shown statically side by side.
- **Spec:** tokens and the motion table as `SpecLine`s.

Register the page in `ShowroomCatalog.kt` under the marker comment.

### 6. Quality gate (loop until clean)
```bash
python3 -I .claude/skills/halo-component/scripts/lint_ds.py <Name>
```
```bash
python3 -I .claude/skills/halo-component/scripts/halo.py shot <id>
```
(`halo.py` runs on Windows, macOS and Linux; on Windows use `python` instead of `python3`.)
1. The lint must report **OK**.
2. `halo.py shot` builds, installs, deep-links to `halo://showroom/<id>` and saves `shots/<id>.png`. **Read the PNG.**
3. Review it against `references/pixel-perfect.md` and your archetype's checklist.
4. Unit tests pass: `halo.py gradle :core:designsystem:testDebugUnitTest`.
5. Feedback: the haptic log (dock → log) shows the right tokens in order; the Showroom **Haptics** lab shows this
   phone's tier. The emulator plays sound but can't vibrate.
6. Light: with a simulated tilt (`adb emu sensor set acceleration 6.5:7.3:0.81`, capture within ~0.6 s, reset to
   `0:9.77:0.81`) highlights and shadows move with no layout change.
7. Continuous motion: report the frame counts (non-negotiable 13).
8. Fix, then repeat. Report the result honestly: anything not verified (how a haptic feels on a real actuator,
   HDR on an HDR screen, 120 Hz feel) is listed as unverified.

**Capturing interaction and motion states.** These are proven recipes; don't improvise coordinates or timing.
- **Find, don't guess:** `halo.py find "<accessibility label>"` prints an element's bounds and centre in device px.
  This only works because components ship real semantics, which is one more reason to get them right.
- **Slow it down:** `halo.py shot <id> --no-build --slowmo 5 [--grid 8] [--reduced] [--theme dark|light]` passes deep-link params
  (`?slowmo=5&grid=8&rm=1`) to the debug dock, so there's no dock tapping. At 5×, a 300ms spring lasts 1.5s and is easy to catch.
- **Drive and capture on the device in one shell**, so the timings are device-side:
  `adb shell "input swipe X Y X Y 1800 & sleep 0.7; screencap -p /sdcard/a.png; sleep 0.8; screencap -p /sdcard/b.png"`, then `adb pull`.
  `input` takes about 0.3–0.5s to start, so add that to every sleep.
- **Read the haptic log** in the captures. It shows which token fired and in what order (e.g. `Tick 0.7 → 0.9 → Threshold`).
- **Fresh state:** `halo.py shot` without `--no-build` reinstalls, which kills the process. Use `adb shell am force-stop com.halo.wallet`
  before reproducing a bug, so stale in-memory state from earlier captures can't mislead you.
- **Capture every state in the spec matrix** (`shots/<id>-<state>.png`), at least: rest, pressed or engaged, mid-transition, and final.

### 7. Registry
Add the row to `docs/components/REGISTRY.md`: name, archetype, status, file, deep link.

### 8. Report
Give the design read, the files touched, any new tokens or feedback patterns, the screenshot path, the checklist
result, test and frame-budget numbers, and anything still needing a physical device (haptic feel per tier, HDR,
tilt feel, 120 Hz).

## When the request conflicts with the system
- **A color, size or curve outside the tokens:** propose a token, with the reason, in the report.
- **"Use Material X":** build the Halo equivalent and explain the mapping.
- **Orange on something non-Bitcoin, a drop shadow, a spinner where content is loading:** push back and offer
  the on-system alternative (accent, glow, skeleton).
- **A request that needs data or business logic:** keep the component presentational. Logic belongs in a
  use case (`:core:domain`) and its state in a feature ViewModel. See `references/architecture.md`.

## Map
- **Foundations** (`core/designsystem/.../foundation`):
  - `HaloPressable`, `pressScale` (`HALO_PRESSED_SCALE`, `_WIDE`), `HaloIndication`, `rememberPressProgress`
  - `HaloText`, `HaloSurface` / `Modifier.depth`
  - `backdropSource` / `backdropGlass`, `haloGlow`, `haloGrain`
  - `HaloSpinner`, `enterRise`, `AmountFormat`, `HaloPreview`
  - `HaloThemeRevealHost` / the Tide (theme transition with live feedback)
- **Feedback** (`core/designsystem/.../feedback`): `HaloPattern` + `Patterns` (tokens), `HapticPlan` (tiers),
  `HapticPlayer`, `SoundSynth` + `SoundEngine`, `TideFeedback` / `TideSound` (live feedback from a trajectory).
- **Light & HDR** (`theme/`): `HaloLight` + `LightRig` + `rememberLightRig` (world-fixed tilt light), `HaloHdr`.
- **Reference implementations, one per archetype built so far:**
  - `HaloButton` (Pressable: press morph, layout-phase collapse)
  - `HaloRollingAmount` (Value display: per-digit odometer, TextMeasurer drawing)
  - `HaloHoldToSend` (Gesture-commit: manual `PressInteraction`, timed charge, clip-inverted label, draw-on check, burst)
  - `HaloToggle` (Toggle: SDF-lit relief materials, measured Mechanical spring, response-first)
  - `HaloLogo` (Feedback/brand: AGSL glass, real fluid physics, gyroscope inertia, gated continuous motion, HDR)
- **Theme:** `core/designsystem/.../theme`. **Icons:** `icons/HaloIcons` + `HaloIcon`.
- **Showroom chrome:** `feature/showroom/.../ui/ShowroomChrome.kt` (`ShowroomCard`, `SectionLabel`, `Knob`, `SpecLine`, `haloDepth`).
- **References:**
  - `tokens.md`
  - `motion-recipes.md`
  - `haptics.md`
  - `archetypes.md`
  - `pixel-perfect.md`
  - `native-android.md`
  - `anti-patterns.md`
  - `architecture.md`
