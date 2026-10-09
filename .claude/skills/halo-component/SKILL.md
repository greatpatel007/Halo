---
name: halo-component
description: Builds a production-grade, hand-made Jetpack Compose component for the Halo Bitcoin-wallet design system from a written requirement — spec, implementation, motion choreography, haptics, accessibility, Showroom page and registry entry, verified on a device screenshot. Use whenever the user asks to create, build, design or add a UI component, control, micro-interaction or animated element to Halo (e.g. "build a hold-to-send button", "add an amount keypad", "make the balance roll like an odometer"), or invokes /halo-component.
argument-hint: "[ComponentName] [requirements…]"
---

# Halo component forge

You turn a requirement into a **finished design-system component**. Its bar is "2027-grade fintech app on
Android": pixel-exact, physically animated, tactile, accessible and unmistakably native. Every component
is built **from scratch** on Compose foundations and Halo tokens.

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

## Workflow

### 0. Load context (always)
Read `references/tokens.md`, `docs/components/REGISTRY.md` and your archetype's section in `references/archetypes.md`.
Skim `references/motion-recipes.md` for the patterns you'll need.

### 1. Design read
State one line: `<Name> · <archetype> · <job> · <signature moment>`. Example:
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

### 5. Showroom page
Start from `templates/ShowroomPage.kt.tmpl`. It has three blocks:
- **Playground:** the live component plus knobs.
- **State matrix:** every state shown statically side by side.
- **Spec:** tokens and the motion table as `SpecLine`s.

Register the page in `ShowroomCatalog.kt` under the marker comment.

### 6. Quality gate (loop until clean)
```bash
python -I .claude/skills/halo-component/scripts/lint_ds.py <Name>
```
```bash
powershell -ExecutionPolicy Bypass -File .claude/skills/halo-component/scripts/shot.ps1 -Page <id>
```
1. The lint must report **OK**.
2. `shot.ps1` builds, installs, deep-links to `halo://showroom/<id>` and saves `shots/<id>.png`. **Read the PNG.**
3. Review it against `references/pixel-perfect.md` and your archetype's checklist.
4. Fix, then repeat. Report the result honestly: anything not verified (e.g. how a haptic feels) is listed as unverified.

**Capturing interaction and motion states.** These are proven recipes; don't improvise coordinates or timing.
- **Find, don't guess:** `scripts/find.ps1 -Desc "<accessibility label>"` prints an element's bounds and centre in device px.
  This only works because components ship real semantics, which is one more reason to get them right.
- **Slow it down:** `shot.ps1 -Page <id> -NoBuild -SlowMo 5 [-Grid 8] [-Reduced]` passes deep-link params
  (`?slowmo=5&grid=8&rm=1`) to the debug dock, so there's no dock tapping. At 5×, a 300ms spring lasts 1.5s and is easy to catch.
- **Drive and capture on the device in one shell**, so the timings are device-side:
  `adb shell "input swipe X Y X Y 1800 & sleep 0.7; screencap -p /sdcard/a.png; sleep 0.8; screencap -p /sdcard/b.png"`, then `adb pull`.
  `input` takes about 0.3–0.5s to start, so add that to every sleep.
- **Read the haptic log** in the captures. It shows which token fired and in what order (e.g. `Tick 0.7 → 0.9 → Threshold`).
- **Fresh state:** `shot.ps1` without `-NoBuild` reinstalls, which kills the process. Use `adb shell am force-stop com.halo.wallet`
  before reproducing a bug, so stale in-memory state from earlier captures can't mislead you.
- **Capture every state in the spec matrix** (`shots/<id>-<state>.png`), at least: rest, pressed or engaged, mid-transition, and final.

### 7. Registry
Add the row to `docs/components/REGISTRY.md`: name, archetype, status, file, deep link.

### 8. Report
Give the design read, the files touched, any new tokens, the screenshot path, the checklist result and anything
still needing a physical device (haptics, 120 Hz feel).

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
- **Reference implementations, one per archetype built so far:**
  - `HaloButton` (Pressable: press morph, layout-phase collapse)
  - `HaloRollingAmount` (Value display: per-digit odometer, TextMeasurer drawing)
  - `HaloHoldToSend` (Gesture-commit: manual `PressInteraction`, timed charge, clip-inverted label, draw-on check, burst)
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
