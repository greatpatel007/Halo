# Research 0002: feedback for the theme switch (switch + Tide)

**Goal:** sound and haptics that *match what the eye sees*, moment by moment, following real physics. Written
2026-10-11 before implementation; **built the same day** (`feedback/TideFeedback.kt`, `feedback/TideSound.kt`,
hooked into `ThemeReveal`; tests in `TideFeedbackTest`).

## 1. What actually happens on screen
| t (1×) | Visual | Physical reading |
|---|---|---|
| press | thumb sinks and leans 15% toward the new side (Snap) | a mechanical switch being loaded against its detent |
| tap release (frame 0) | thumb slides ~40dp on `Mechanical` (critically damped, ~330 ms, no bounce) | detent released: a crisp over-centre click, a mass sliding in a channel, landing |
| +2 frames, after the theme lands | the switch alone flips material; the **Tide** is born at the switch | the switch is the source: water released |
| 0 → ~1.2 s | the front sweeps from the switch to the far corner on `Tide` (critically damped, k = 7, launched with a kick: full speed at once, only decelerating); sand grains flip, foam crest, wet sand behind | a wave running up a beach: a rush that thins out |
| mid-wave tap | the front reverses, keeping its momentum | the water pulled back |
| reduced motion | 120–200 ms crossfade | no water, just a change |

The Tide's front velocity is analytic. With error e = x − target, critically damped:
`e(t) = (e₀ + (v₀ + ωe₀)t)·e^(−ωt)`, so `v(t) = e^(−ωt)·(v₀ − ωt(v₀ + ωe₀))`. For the launch kick
(v₀ = ½ωD, e₀ = −D) that is `v(t) = ½ωD·(1 + ωt)·e^(−ωt)`: peak speed at once, half speed at ~0.63 s, 10% at
~1.4 s (ω = 2.65 rad/s). **Every feedback parameter below follows the normalised speed s(t) = |v(t)| / v_peak**, so
the sound and the vibration slow down exactly as the water does, and follow it backwards on a reversal.

## 2. What it should feel and sound like
Physical sources, in order. One cause, then its effect (Apple: the control answers first, the consequence grows
out of it). Each layer is something that happens in the scene, not decoration.

| Moment | Haptic | Sound | Real-world model |
|---|---|---|---|
| Switch commit (frame 0) | `ToggleOn` / `ToggleOff`: crisp over-centre click (+ light settle on On) | click up / click down | detent spring releasing |
| Wave launch | one deep, soft hit (intensity 0.55, sharpness 0.2; to-light slightly sharper) | a low "whoomp": 70–90 Hz body with a pitch drop + a short noise burst | a body of water released at once |
| The rush | a continuous body whose intensity follows s(t)^0.8, low sharpness (water is round) | surf: noise through a band-pass whose centre rises with speed (≈250 Hz → 1.8 kHz) and level follows s(t)^0.9 | turbulent flow noise rises in level and brightness with speed |
| Sand | sparse sharp grains on top of the body, rate ∝ s(t) (≈ 30/s → 0) | crackle: very short high-passed noise grains, rate ∝ s(t) | sand grains rolling in the swash |
| Foam | — (too fine to feel) | bubbles: tiny rising sine chirps (Minnaert resonance, 1.8–4 kHz), rate ∝ s(t), only while fast | air bubbles collapsing in foam |
| Direction | to dark: deeper (sharpness −, surf centre ×0.75); to light: brighter (×1.25) | | |
| Position | — | stereo pan follows the front's x across the screen (it starts at the switch, top right, and runs left) | a source you can locate |
| Reversal | haptic restarts from the reversed trajectory; sound follows live | | the water pulled back keeps its energy |
| End | fades with s(t) → nothing; no landing hit | | a wave thinning out on sand |
| Reduced motion | one soft, round tap | a short quiet breath (200 ms) | |

What it must not do: buzz uniformly (it would feel like a phone ringing), stack a haptic on every grain flip
(thousands), keep running after the wave is visually gone, or be louder than the switch click it follows.

## 3. Android constraints and how each tier renders the rush
A running vibration can't be modulated live, but the trajectory is known at launch, so the whole wash is
**pre-computed from the spring** and played as one effect; a reversal cancels it and plays the new trajectory.
| Tier | Rush rendering |
|---|---|
| Envelope | one envelope: points every max(min control point, 24 ms); intensity = body·(1 ± grain jitter), sharpness = base + 0.2·s ± jitter (grain), launch spike first |
| Primitives | the launch as `THUD`, then a granular stream of `LOW_TICK`/`TICK` whose scale follows s and whose spacing widens as it slows (≈ 35 ms → 140 ms): "sand rushing", no motor buzz |
| Amplitude | a 4 ms-step waveform: body amplitude from s plus short grain spikes |
| Basic | launch hit only (or nothing); never a long buzz |

Sound is synthesised live on the mixer thread (one voice), so it follows the actual animation frame by frame:
the reveal pushes `s` and the pan each frame; the voice smooths them per block (no zipper noise) and stops itself
once released and silent.

## 4. Verification plan
- Unit tests: the sampled trajectory matches the spring (peak at t = 0, half speed near 0.63 s at 1×); every tier's
  plan starts with the launch, decays, ends silent; grain spacing widens as speed falls; the live voice is silent
  before launch and after release, never clips, never NaNs.
- Emulator: the feedback log shows the sequence (Toggle → Tide); the sound plays through the Mac; a real-speed
  recording checks that the audible rush ends when the front leaves the screen.
- Device (user's Samsung, likely Primitives or Amplitude tier): feel.

## 5. Results (emulator, 2026-10-11)
- `TideFeedbackTest` (8 tests): trajectory equals the analytic spring; half speed at 0.63 s; wash ≈ 1.9 s at 1×; each
  tier launches, thins out and ends silent; reversals carry no launch hit; the water voice never clips or NaNs,
  rushes then thins (early energy > 5× late), and stops after release.
- On device (emulator): the voice starts with the wave and its trajectory reports 1.89 s. It now ends 0.3 s after
  the trajectory dies, instead of waiting ~3.4 s for the front to settle its last pixel.
- Feel on a real actuator: pending the user's Samsung.
