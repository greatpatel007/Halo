# Native Android contract

Halo looks bespoke but must *behave* like the best first-party Android apps.

## Window & insets
- The app is edge-to-edge (`enableEdgeToEdge`, transparent bars, light icons). Components never assume a status
  or navigation bar. Screens/overlays consume insets with `WindowInsets.safeDrawing`, `statusBars`, `navigationBars` and `ime`.
- A bottom-anchored component (sheet, CTA bar, keypad) adds `navigationBarsPadding()`. A text input
  screen adds `imePadding()`, and the field stays visible as the keyboard animates.

## Back
- Every dismissible surface supports **predictive back** (`PredictiveBackHandler`): it previews the dismissal with progress and
  settles on commit or cancel (motion recipe 12). Never intercept back without a visual preview.
- Screen-level back animations come from NavHost `popExit`/`popEnter` (springs are seekable).

## Accessibility
- Semantics on every component:
  - a role
  - a label (`contentDescription` only on non-text visuals)
  - `stateDescription` for state
  - `customActions` for gestures
  - `liveRegion` for async outcomes
- Merge a row's descendants (`Modifier.semantics(mergeDescendants = true)`) so TalkBack reads it as one item.
- Touch targets are ≥ 48dp. Layouts survive 200% font scale (nonlinear scaling on Android 14+).
- Respect "Remove animations": the theme sets `motion.reduced`, so check it for decoration.
- Keyboard and D-pad: everything interactive is focusable, `HaloIndication` draws the focus ring, and Enter activates.

## RTL
- Use `start`/`end`, never `left`/`right`. Directional icons (back, chevron, send arrows) mirror in RTL. Wrap them in
  `Modifier.graphicsLayer { scaleX = if (layoutDirection == Rtl) -1f else 1f }` or provide mirrored vectors.
- Numbers stay LTR inside RTL text (amounts are isolated).

## Haptics & system settings
- Haptics route through `HaloHaptics`, which respects the system touch-feedback setting.
- Don't override the system font, display size or dark-theme preference; the design system scales with them.

## Performance (120 Hz)
- Read animated state only in draw/layout lambdas, and allocate nothing per frame inside `drawBehind`. Cache
  `Path`s, `Paint`s and shaders in `drawWithCache` or `remember`.
- Use `drawWithCache` for anything derived from size. `RuntimeShader` uniforms are set in the draw lambda.
- Use `Modifier.Node` (not `composed {}`) for reusable modifiers with state.
- Use stable parameters: `@Immutable`/`@Stable` classes, lambdas remembered when passed through.

## Platform APIs by version (minSdk 26, proposal 0004)
Everything newer than Android 8 is used where the phone has it, with a designed fallback below:

| API | From | Below that |
|---|---|---|
| `RuntimeShader` (AGSL) | 13 | canvas fallback (`HaloPlatform.shaders`): gradients, bitmap grain, crossfade |
| `RenderEffect` blur | 12 | frosted fill (`HaloPlatform.blur`) |
| haptic primitives / envelopes | 12 / 16 | amplitude waveform, platform effects |
| `VibrationAttributes` | 13 | sonification `AudioAttributes` |
| thermal status | 10 | never "hot" (battery saver still applies) |
| `BlurMaskFilter` (hardware) | 9 | stacked outlines (`haloGlow`) |
| per-app night mode | 12 | Compose palette only |
| HDR headroom | 14/15 | standard range |

Debug: `?legacy=1` / `halo.py shot <id> --legacy` renders as Android 8–12 on any phone.


## Hardware: haptics, sound, sensors, HDR
- **Haptics vary by phone.** Never assume an actuator: the feedback engine probes it once and renders each pattern on
  the richest tier (envelope → primitives → amplitude → basic). Basic motors only get strong moments.
- **Sound etiquette.** UI sounds follow the ringer (silent/vibrate = none) and the system touch-sounds setting, play as
  sonification, and are short and quiet. Sound and haptic are synced (measured audio latency, ≤ 25 ms wait).
- **Sensors** (gravity for the tilt light, gyroscope for inertia): ~30 Hz, foreground only, off under reduced motion
  and battery saver, unregistered on dispose; write state only when the value really changed.
- **HDR** (Android 14+): the theme opts the window into HDR only on HDR-capable displays and requests 2.5× headroom;
  `HaloTheme.hdr.headroom` stays 1 elsewhere, so SDR rendering is unchanged.
