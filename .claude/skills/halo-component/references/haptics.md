# Haptics

Haptics (and their sounds) are part of the design, not decoration. Halo speaks one vocabulary (`HaloHaptic`). Only the
`feedback` package touches the vibration and audio APIs (with `USAGE_TOUCH`, so the system's touch-feedback setting
applies). Never call platform haptics directly. The Showroom Haptics page is the lab: it shows this phone's tier.

## Rules
1. **One haptic per user intent.** A tap that opens a sheet plays `Tap`; the sheet appearing is silent.
2. **Fire at the visual peak**, not at the end of the animation. Commit haptics fire on the frame the threshold
   is crossed. Success fires when the check *starts* drawing.
3. **Threshold fires exactly once per crossing.** Track an `armed` boolean. Crossing back is silent (or `ToggleOff` if
   the design calls for "disarming" feedback).
4. **Reject always pairs with a shake** (motion recipe 3). A haptic without a visual explanation feels like a bug.
5. **No haptics on scroll, fling or passive updates.** The digit roll may `tick` only on user-driven changes
   (keypad, slider), never on a live balance update.
6. **Charging progress** (hold-to-send): `Crescendo` at the start, `tick(intensity)` steps at most every 80–100ms,
   `Threshold` at 100%, and `Celebrate` when the payment actually succeeds (possibly later).
7. **Cancel** running compositions (`haptics.cancel()`) when a gesture is abandoned.
8. **Disabled controls are silent.**

## How it plays (proposal 0002, Halo Sensory)
Each token is a designed `HaloPattern` (`feedback/Patterns.kt`): a haptic timeline (transients and held ramps with
intensity + sharpness, Apple's vocabulary) plus a synthesised sound, played on the same frame. Components never
change: they still call `haptics.perform(token)`, `tick(i)` and `cancel()`; `perform(token, pan)` places the sound.

The actuator is probed once and every pattern compiles for the richest tier (`feedback/HapticPlan.kt`, unit-tested):
| Tier | When | Renders as |
|---|---|---|
| Envelope | Android 16+ actuator with envelope effects | `BasicEnvelopeBuilder` intensity/sharpness curves for held patterns; tap-only patterns use primitives (crisper) |
| Primitives | composition primitives supported | CLICK/TICK/THUD/LOW_TICK by sharpness; rises/falls for ramps; delays from real primitive durations |
| Amplitude | amplitude control only | synthesised 4 ms-step waveform, pulse length by sharpness, shortened on high-Q actuators |
| Basic | on/off motor | platform feedback for Confirm/Reject/Threshold/taps only; subtle ticks are dropped |

| Token | Haptic | Sound |
|---|---|---|
| Tap | transient 0.55 / 0.5 | soft click, E6 |
| Tick, `tick(i)` | transient i / 0.6+0.4i | tiny tick; pitch climbs the E-major pentatonic with i |
| ToggleOn / Off | crisp click + light settle / one rounder click | click up (E6) / click down (E5) |
| Threshold | transient 0.9 / 0.9 | firm click, G#6 |
| Confirm | two steps up (0.55 → 0.85) | rising glass chime B5 → E6 |
| Reject | three falling dull hits | double low thud (E3) |
| Crescendo | 300 ms swell, sharpening | rising air |
| Celebrate | full hit, decaying shimmer, two sparkles | glass arpeggio E–G#–B–E |
| Ping | transient 0.4 / 0.95 | glass tink E7, 260 ms (damped by the liquid) |
| Tide (theme wave) | deep release, then a rush that follows the front's speed: envelope body with sand jitter / a thinning stream of LOW_TICK grains / waveform | live water: whoomp, surf (band-pass rising with speed), sand crackle, foam bubbles; panned with the front |

The Tide is played live by the theme transition from its own spring (research 0002): the haptic rush is pre-computed
from the trajectory, the water voice follows it, and a mid-wave reversal retargets both. Reduced motion plays a soft
tap and a short breath instead.

Sound etiquette: follows the ringer (silent/vibrate = no sound), the system touch-sounds setting and the in-app
switch; plays as sonification at system volume; mixed in a low-latency `AudioTrack` that sleeps when silent.

## Verifying
- The emulator cannot vibrate (it does play sound). Every played token appears in the Showroom **haptic log** (dock → log), so you can
  verify *which* token fires *when*. Report the timing you observed.
- How a haptic feels needs a physical Android 13+ device. List that as unverified in the report unless the user confirms.
