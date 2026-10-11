# Proposal 0002: Halo Sensory, one feedback system for touch, sound and light

**Status:** Accepted, phase 1+2 built (2026-10-11) · **Raised by:** product direction: "use the hardware: custom sound and haptic
feedback built into the design system for a cohesive experience. Android haptics differ per phone, but we want them
smooth like Apple's."

## Problem
1. **Haptics feel different on every phone.** Today 7 of 9 tokens are stock `performHapticFeedback` constants. On a
   Pixel they are crisp; on a phone with a cheap ERM motor the same call is a mushy buzz. Apple's haptics feel
   consistent because one actuator (Taptic Engine) and one authoring model (Core Haptics: transient and continuous
   events with *intensity* and *sharpness* curves) cover every device.
2. **There is no sound.** Apple pairs most signature moments with a tiny, tuned sound played in sync with the haptic
   (keyboard, Apple Pay, toggles). Halo's moments (hold-to-send, the Tide, the glass halo) are silent.
3. **Feedback is not one system.** Components call haptics directly; sound and the visual peak have no shared clock,
   so they could drift apart.

## Proposal

### 1. One authoring model: `HaloPattern` (Core Haptics / AHAP-like)
A pattern is a short timeline of events:
- **Transient**: a single tap, with `intensity` and `sharpness` (0..1).
- **Continuous**: a held vibration with intensity and sharpness *curves* over its duration.
- **Sound** cue on the same timeline (see 3).

Every `HaloHaptic` token becomes a designed `HaloPattern`. Components keep calling tokens; nothing in components changes.

### 2. A capability-tiered renderer (the Android workaround)
At start-up Halo probes the actuator once and picks the best tier. The same pattern renders on all of them:

| Tier | Detected by | How a pattern renders | Typical phones |
|---|---|---|---|
| **T1 Envelope** | `areEnvelopeEffectsSupported()`, `getFrequencyProfile()` | `BasicEnvelopeBuilder` (intensity + sharpness curves) / `WaveformEnvelopeBuilder` (frequency in Hz, mapped through the device's frequency profile and resonant frequency) | recent Pixels, flagship LRAs |
| **T2 Primitives** | `arePrimitivesSupported`, `getPrimitiveDurations` | Composition: sharp transients → `CLICK`/`TICK`, dull → `THUD`/`LOW_TICK`, continuous → `SLOW_RISE`/`QUICK_RISE`/`QUICK_FALL`; delays from real primitive durations | most LRA phones, Android 12+ |
| **T3 Amplitude** | `hasAmplitudeControl()` | synthesised `createWaveform` in 4–8 ms steps, sharpness approximated by attack/release steepness, Q factor used to shorten tails | mid-range |
| **T4 Basic** | none of the above | only the strongest moments (Confirm, Reject, Threshold) via `performHapticFeedback`; subtle ticks are **dropped** (Apple's rule: no haptic beats a mushy one) | ERM motors |

Plus:
- **Pre-built effects**: every token is compiled to a `VibrationEffect` once per device and cached; playing one is a
  lookup on the touch-down frame (lowest latency).
- **Per-device calibration**: an intensity curve normalised from the frequency profile and Q factor, so "0.4" feels
  similar across phones. A Showroom **Haptic lab** page plays every token per tier for tuning on real devices.
- **Respect the system**: the touch-feedback setting, `USAGE_TOUCH` attributes (system intensity), battery saver
  (T3/T4 only), an in-app off switch.

### 3. Sound: `HaloSound`, synthesised, tuned and in sync
- **A family, not a pack:** soft clicks, a glass "tink", a low "thud" and a breath of noise, all tuned to one key
  (pentatonic) so overlapping sounds never clash. Each sound is under 120 ms except the signature ones.
- **Procedural synthesis in Kotlin** (modal/FM synthesis rendered to PCM at start-up): no audio files, a tiny APK, and
  **parametric**: the hold-to-send charge rises in pitch with progress; the digit roll ticks pitch by digit; the glass
  halo's tap "tink" follows the liquid's speed.
- **Low latency:** a pre-warmed `AudioTrack` in `PERFORMANCE_MODE_LOW_LATENCY` mixing pre-rendered buffers.
- **Sync with haptics:** play sound and vibration on the same frame, offset by measured output latency
  (`AudioTrack` timestamps) so they land together. Where supported, `HapticGenerator` can derive haptics from the
  sound itself for continuous moments.
- **Etiquette (Apple):** UI sounds follow silent/vibrate mode and the system "touch sounds" setting, play on
  `USAGE_ASSISTANCE_SONIFICATION`, never on scroll or passive updates, and stay quiet when media is playing.
- **Space:** subtle stereo pan by the control's on-screen x position.

### 4. One API: `HaloFeedback`
`HaloTheme.feedback.perform(HaloFeedback.Tap)` plays the pattern's haptic and sound together; the haptic log becomes a
feedback log (token, tier, sound) so the emulator can still verify timing. `HaloTheme.haptics` stays as a thin
compatibility layer.

## More hardware to use (beyond sound and haptics)
| Idea | Hardware | Why it is next-level |
|---|---|---|
| **HDR glow** | HDR displays (`Window.setDesiredHdrHeadroom`, Android 14+) | the radium liquid, glows and the Tide's foam can be brighter than white on HDR screens: real emission, not tinted paint |
| **Tilt light + parallax** (decision D6) | gyroscope / rotation vector | the key light and highlights move as you tilt; already used by the glass halo for inertia |
| **Adaptive frame rate** | 120 Hz panels (frame-rate hints) | 120 Hz during interaction, low rate at rest: smoother and kinder to the battery |
| **Velocity-aware feedback** | touch velocity | a fast flick plays a sharper, stronger tick than a slow drag (physical) |
| **Ambient-light aware materials** | light sensor | grain and glow intensity adapt to a dark room vs daylight |
| **Biometric moments** | `BiometricPrompt` | a signature haptic + sound when a payment is authorised |

## Rollout
1. `HaloPattern`, the tier probe and the T1–T4 renderers; re-author the 9 tokens; Haptic lab page.
2. `HaloSound` synthesis engine + the sound family; `HaloFeedback` unifying both; feedback log.
3. Wire signature moments: hold-to-send charge, Tide, glass halo, toggle.
4. HDR glow and tilt light (D6).

## Verification
- Unit tests: pattern → effect compilation per tier (pure Kotlin, fake capability profiles).
- Emulator: feedback log timing; audio renders (the emulator plays sound).
- **Real devices required** for feel: one T1 phone (recent Pixel), one T2/T3 (mid-range Samsung or similar), one T4.

## Decisions (2026-10-11)
- Sound is **on by default**, following silent/vibrate mode and the system touch-sounds setting, with an in-app switch.
- Sounds are **procedurally synthesised** (no audio files).
- Test device: a Samsung / mid-range phone (expected tier: Primitives or Amplitude).
- Haptics and sound are built **together** as one layer. Components keep the `HaloTheme.haptics` call shape
  (`perform`, `tick`, `cancel`), so nothing in components changed; renaming to `HaloFeedback` is optional later.
- Sounds play over media like iOS UI sounds (the "quiet while media plays" idea was dropped).

## Built
- `feedback/`: `HaloPattern` model, `Patterns` (9 tokens + quantised `tick`), `HapticPlan` (tiers + compiler,
  unit-tested), `HapticPlayer` (probe, effect cache), `SoundSynth` (click/glass/thud/breath, unit-tested),
  `SoundEngine` (low-latency mixer, etiquette, stereo pan), `HaloFeedbackEngine` (one per process).
- Showroom Haptics page → lab: tier and capabilities, switches, every token, panned tiles, a composed charge.
- Phase 3 (2026-10-11):
  - **Theme switch**: the Tide's feedback follows the wave's own spring (research 0002): launch hit, a haptic rush
    pre-computed per tier, a live stereo water voice (body, splash, sand, Minnaert bubbles) panned with the front,
    reversals retargeted, a soft fade for reduced motion. Water clarity is a test (`TideClarityTest`).
  - **Sync**: measured audio output latency; paired haptics wait for it (max 25 ms).
  - **Calibration**: per-phone strength ×0.5–1.5 (lab knob), applied to every tier's plan.
  - **Glass halo**: tapping it plays `Ping`, a glass tink.
- Phase 4 (2026-10-11):
  - **Tilt light** (D6): the key light is world-fixed. Gravity gives the device's rotation since the holding angle;
    the same rotation turns the light (Rodrigues), capped at 30°, smoothed, re-centring over ~4 s. State is read in
    draw only, so a tilt redraws lit surfaces and never recomposes. Verified on the emulator with a simulated tilt;
    `TiltLightTest`.
  - **HDR glow**: on HDR displays (Android 14+) the window opts into HDR with 2.5× headroom; only emitted light (glass
    halo liquid and spill, `haloGlow`, Tide foam and glints) is scaled by `HaloTheme.hdr.emission`. SDR unchanged
    (verified on the emulator, which has no HDR screen; HDR itself is unverified).
  - The `halo-component` skill now requires all of this for every new component.
- Not yet: persisting the in-app switches and strength, `HapticGenerator`, 3D parallax (rest of D6).
