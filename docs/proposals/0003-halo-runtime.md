# Proposal 0003: Halo runtime, system-like services behind the theme

**Status:** Accepted, step 1 built (2026-10-11) · **Raised by:** product direction: "one engine that handles all the
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
| **Sensors** | one shared tilt stream (gravity + gyroscope), ref-counted, on only while a visible effect or the light needs it | step 2 |
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
