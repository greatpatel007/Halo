# Proposal 0003: Halo runtime, system-like services behind the theme

**Status:** Done (2026-10-11): steps 1–5 built · **Raised by:** product direction: "one engine that handles all the
nitty-gritty of the experience, so every component talks to a single internal API, like a system."

## Problem
Continuous effects and hardware use were each re-implementing the same plumbing: visibility, lifecycle, battery
saver and reduced-motion checks (glass halo, tilt light), their own frame loops and 30 fps throttles, and separate
sensor listeners. Policy changes would have to be made in several places, and nothing saw the total cost.

## Design
`HaloTheme` stays the one door. Behind it, `HaloRuntime` (`HaloTheme.runtime`) owns small services:

| Service | Owns | Status |
|---|---|---|
| **Policy** (`HaloPolicy`) | foreground, reduced motion, battery saver, **thermal** (SEVERE+ pauses decoration); `ambientAllowed`, `pausedBecause`; event-driven | built |
| **Clock** (`HaloClock`, `rememberAmbient`) | one frame clock for continuous effects; visible-only; full rate while busy, ~30 fps when calm, nothing when still; slow-mo aware | built |
| **Sensors** (`HaloSensors`) | one listener per sensor (gravity, gyroscope), ref-counted, policy-gated; `onStop` lets subscribers settle | built |
| **Scene** (`HaloScene`) | the world-fixed light (gravity from Sensors) and HDR state; HDR back to standard range while conserving | built |
| **Feedback** | haptics + sound engine attached to the policy: event-driven sound etiquette (ringer, touch sounds); the Tide's rush and water shrink to the launch while conserving | built |
| **Lab** | Showroom → Foundations → Runtime: verdict, policy inputs, clock steps/s, sensors, light tilt, HDR, feedback | built |

Tokens stay pure values; the runtime is services. Pure parts (scheduler, models) are unit-tested; a fake runtime
serves previews and tests. It lives in `:core:designsystem` until non-UI code needs it.

## Step 1 results (emulator, 2026-10-11)
- `AmbientSchedulerTest` (4 tests): hidden effects never step; busy = every frame; calm ≈ 30 fps with physics time equal
  to real time; a pause is not handed over as elapsed time.
- The glass halo moved onto `rememberAmbient` (its own lifecycle/power/frame-loop code removed). Frames in 5 s:
  visible 153, scrolled off 1, background 0, reduced motion 1 (the demo balance), unchanged from before.
- Thermal: `adb shell cmd thermalservice override-status 3` → 1 frame; `reset` → resumes (103).

## Step 2 results (emulator, 2026-10-11)
- `HaloSensors`: subscribers share one hardware listener; the last close turns it off; the policy stops and restarts
  every sensor; missing hardware is never reported active. The tilt light (`GravityFilter`, now pure) and the glass
  halo's gyroscope inertia moved onto it; their own registration, lifecycle and power code removed.
- **Bug found and fixed:** the policy learned about lifecycle changes through recomposition, but a backgrounded app
  produces no frames, so the gravity sensor (tilt light) stayed on in the background. The policy now observes the
  lifecycle directly and pushes every change to the sensor hub synchronously (`HaloSensorsTest` covers it).
- Live listeners (`dumpsys sensorservice`): logo visible → gyroscope 1, gravity 1; logo scrolled away → 0, 1;
  background → 0, 0; back → 0, 1; logo visible again → 1, 1.
- Regression: the tilt light still shifts shading under a simulated tilt; frames: logo visible 140/5 s, scrolled off
  1/5 s once the light has settled. 34 unit tests pass.

## Step 3 results (2026-10-11)
- Policy gains listeners (several services react synchronously), `soundAllowed` (ringer + touch sounds, from a
  broadcast and a settings observer: the sound engine's once-a-second polling is gone) and `conserving` (battery
  saver or heat).
- `HaloScene` owns the light rig (gravity via the shared hub) and `HaloHdr`; `HaloTheme` reads both from the runtime.
  While conserving, HDR emission drops to standard range and the window asks for headroom 1 (HDR-capable displays).
- Feedback attaches to the policy: while conserving, the theme wave plays only its launch (no rush, no live water).
  Discrete action feedback (taps, toggles) always plays.
- `HaloSceneTest` (3): light follows gravity and settles when the hub stops; HDR limited while conserving and restored;
  sound etiquette and listener notifications. 37 tests pass.
- Emulator: battery saver → logo 1 frame/5 s, gyro 0 / gravity 0; two theme flips under saver with no crash; saver off
  → sensors back (gyro 1 / gravity 1) and motion resumes. Calm frame counts on this emulator vary with host load
  (≈50–190 per 5 s); hidden / background / saver are consistently 0–1.

## Step 4 results (2026-10-11)
- `RuntimePage` (Showroom → Foundations → Runtime) reads everything from `HaloTheme.runtime`; the clock gained a
  steps-per-second meter (one state write per second, only while running); `HaloHaptics.audioLatencyMs` exposed.
- Verified live: normal → "Ambient motion and sensors may run", gravity + gyroscope on; battery saver → "Ambient
  paused: battery saver", "conserving: HDR off, short Tide", steps/s 0, both sensors off.
- **Bug found and fixed:** at exactly 60 Hz two frames sum to 0.0333… s, which can fall a hair under the 1/30 s calm
  threshold in floating point, so calm effects stepped at 20/s instead of 30/s. A small tolerance fixes it
  (`AmbientSchedulerTest` asserts 300 calm steps per 10 s at 60 Hz). Clock frame log: 61 frames/s, 30 steps/s.
- A suspected "slow page after navigating from home" was a harness artefact (dropped injected gestures, measuring the
  wrong screen under capture load); with verified navigation the page runs at 61 frames/s, 30 steps/s, and leaving home
  stops its effect (0 frames on the Color page).

## Step 5 (2026-10-11)
The `halo-component` skill is restructured around the runtime: non-negotiables split into **Craft** (1–8) and
**Experience** (9–16, incl. sensors only from the hub and a short form while conserving), a **Runtime quick reference**
with working code, a new **Ambient** archetype with its checklist, runtime notes in the component and page templates,
and the skill's description now names the runtime so it loads for living/animated elements. `CLAUDE.md` matches.
