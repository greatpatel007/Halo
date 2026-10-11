# Proposal 0003: Halo runtime, system-like services behind the theme

**Status:** Accepted, steps 1–2 built (2026-10-11) · **Raised by:** product direction: "one engine that handles all the
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
| **Scene** | the world-fixed light and HDR headroom, driven by Sensors/Policy | step 3 |
| **Feedback** | haptics + sound (the existing engine), on the same clock and policy | step 3 |
| **Lab** | a Showroom "Runtime" panel: live policy, active effects, sensors, frame budget | step 4 |

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
