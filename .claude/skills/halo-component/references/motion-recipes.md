# Motion recipes

Every recipe uses tokens, reads animated values in the draw/layout phase, and stays interruptible.
`motion = HaloTheme.motion`, `haptics = HaloTheme.haptics`, `scope = rememberCoroutineScope()`.

**Ground rules**
- Keep `Animatable`s in `remember {}`. Drive them from event callbacks (`scope.launch`) or `LaunchedEffect(key)`.
  Calling `animateTo` on a running `Animatable` retargets it from the current value and velocity. That is the
  interruption model: never `snapTo` first.
- Read values in lambdas such as `graphicsLayer { scaleX = a.value }`, `offset { IntOffset(...) }` and
  `drawBehind { ... a.value ... }`. Don't pass `a.value` as a parameter to a composable unless the layout itself must change.
- Use `animate*AsState` only for discrete state → value mappings where recomposition is cheap and rare (e.g. a color on toggle).
- Gate decoration (particles, pulses, glow breathing) on `!motion.reduced`.

---

## 1. Press response
```kotlin
HaloPressable(onClick = onClick, shape = shape, modifier = Modifier.depth(...)) { content() }
```
Scale 0.97 on `Snap`, release on `Quick` with a hair of overshoot, touch-origin highlight, `Tap` haptic, semantics.
For a custom surface use `Modifier.pressScale(interactionSource)` and pass `HaloIndication(shape)` to `clickable`.

## 2. Press-reactive corner morph (M3-Expressive feel)
```kotlin
val source = interactionSource ?: remember { MutableInteractionSource() }
val pressed by source.collectIsPressedAsState()
val radius = remember { Animatable(rest.value) }
LaunchedEffect(pressed) { radius.animateTo(if (pressed) pressedRadius.value else rest.value, motion.spring(HaloSpring.Snap)) }
Modifier.graphicsLayer { shape = RoundedCornerShape(radius.value.dp); clip = true }   // layer phase only
```
Always pass the same `source` to `HaloPressable` so the scale and the morph stay in sync.

## 3. Error shake (pair with `Reject`)
```kotlin
val shake = remember { Animatable(0f) }
val impulse = with(LocalDensity.current) { motion.shakeImpulse.toPx() }
LaunchedEffect(errorCount) {
    if (errorCount == 0) return@LaunchedEffect
    haptics.perform(HaloHaptic.Reject)
    shake.animateTo(0f, motion.spring(HaloSpring.Expressive), initialVelocity = -impulse)
}
Modifier.offset { IntOffset(shake.value.roundToInt(), 0) }
```
A spring from 0 to 0 kicked with velocity gives a natural, decaying shake. Never use a keyframed wiggle.

## 4. Enter choreography
`Modifier.enterRise(index)` gives a 12dp rise plus fade on `Gentle`, staggered 30ms per index (cap 6). Use it on static
content only; inside lazy lists it replays on scroll.

## 5. Odometer / digit roll
Each digit is a column of glyphs `0…9`, clipped to one line height. A `Digit`-spring `Animatable` holds the
**continuous** position, so 3 → 7 passes through 4, 5 and 6. Translate by `-position * lineHeight` in a `drawWithContent`/`offset {}`
lambda. Roll up when the total increases and down when it decreases. Separators and units crossfade instead
of rolling. Reference: `component/HaloRollingAmount.kt`.

## 6. Hold-to-commit (time-based)
```kotlin
val progress = remember { Animatable(0f) }
Modifier.pointerInput(enabled) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        val charge = scope.launch {
            haptics.perform(HaloHaptic.Crescendo)
            progress.animateTo(1f, motion.timed(HOLD_MS))   // linear: progress = time
            haptics.perform(HaloHaptic.Threshold)
            onCommit()
        }
        waitForUpOrCancellation()
        if (progress.value < 1f) {
            charge.cancel(); haptics.cancel()
            scope.launch { progress.animateTo(0f, motion.spring(HaloSpring.Quick)) }
        }
    }
}.semantics { customActions = listOf(CustomAccessibilityAction(label) { onCommit(); true }) }
```

## 7. Swipe-to-commit (distance-based, velocity-aware)
```kotlin
val offset = remember { Animatable(0f) }
var armed by remember { mutableStateOf(false) }
Modifier.draggable(
    orientation = Orientation.Horizontal,
    state = rememberDraggableState { delta ->
        scope.launch { offset.snapTo((offset.value + delta).coerceIn(0f, maxPx)) }   // 1:1 with the finger
        val crossed = offset.value >= thresholdPx
        if (crossed != armed) { armed = crossed; if (crossed) haptics.perform(HaloHaptic.Threshold) }  // once per crossing
    },
    onDragStopped = { velocity ->
        if (armed || velocity > flingPx) { offset.animateTo(maxPx, motion.spring(HaloSpring.Quick), velocity); onCommit() }
        else offset.animateTo(0f, motion.spring(HaloSpring.Quick), velocity)
    },
)
```

## 8. Path draw-on (success check, signature lines)
```kotlin
val measure = remember { PathMeasure() }
val segment = remember { Path() }
Canvas(Modifier.size(size)) {
    measure.setPath(checkPath(size), false)
    segment.reset()
    measure.getSegment(0f, measure.length * progress.value, segment, true)
    drawPath(segment, color, style = Stroke(stroke.icon.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}
```
Drive `progress` with `motion.spring(HaloSpring.Quick)` after the commit haptic.

## 9. Particle burst (celebration)
Precompute N particles (angle, speed, size, color from `accent`/`bitcoin` roles) in `remember`. One `Animatable` goes
from 0 to 1 over `motion.timed(700, HaloEasing.EmphasizedDecelerate)`. In `drawBehind` set
`pos = origin + dir * speed * p + gravity * p²` and `alpha = 1 − p`. Skip it entirely when `motion.reduced`.

## 10. Unit / content switch
```kotlin
AnimatedContent(
    targetState = unit,
    transitionSpec = {
        (slideInVertically(motion.spring(HaloSpring.Standard, IntOffset.VisibilityThreshold)) { it / 3 } + fadeIn(motion.fade(HaloFade.Fast)))
            .togetherWith(slideOutVertically(motion.spring(HaloSpring.Standard, IntOffset.VisibilityThreshold)) { -it / 3 } + fadeOut(motion.fade(HaloFade.Fast)))
            .using(SizeTransform(clip = false) { _, _ -> motion.spring(HaloSpring.Standard, IntSize.VisibilityThreshold) })
    },
) { unit -> ... }
```

## 11. Glow breathing / intensity
`Modifier.haloGlow(colors.accent.glow, shape, effects.glowRadius, effects.glowSpread, intensity = { glow.value })`.
Animate `glow` with a spring on state changes (pressed → 1, idle → 0.6). Use an infinite pulse only on a single
hero element, and never when `motion.reduced`.

## 12. Predictive back on overlays (sheets / dialogs)
Requires `androidx.activity:activity-compose` in `:core:designsystem`; add it when the first overlay lands.
```kotlin
PredictiveBackHandler(enabled = visible) { events ->
    try {
        events.collect { e -> backProgress.snapTo(e.progress) }        // drives scale 1→0.92, radius → radius.sheet
        onDismiss()                                                     // committed
    } catch (e: CancellationException) {
        scope.launch { backProgress.animateTo(0f, motion.spring(HaloSpring.Quick)) }   // cancelled
        throw e
    }
}
```

## 13. Shimmer skeleton (loading)
Animate a time uniform without recomposition:
```kotlin
val t = remember { mutableFloatStateOf(0f) }
LaunchedEffect(Unit) { while (true) withFrameMillis { t.floatValue = it / 1000f } }
Modifier.drawWithCache { val shader = RuntimeShader(SHIMMER); onDrawBehind { shader.setFloatUniform("t", t.floatValue); drawRect(ShaderBrush(shader)) } }
```
When `motion.reduced` is on, draw a static `bg.raised` block instead.

## 14. Screen-level shared elements (features, not the design system)
Use `SharedTransitionLayout` with `Modifier.sharedElement(rememberSharedContentState(key), animatedVisibilityScope,
boundsTransform = { _, _ -> motion.spring(HaloSpring.Gentle, Rect.VisibilityThreshold) })` around NavHost destinations.
Design-system components expose a `modifier` so features can attach this.


## Physics-first motion (Apple's fluid-interface guidance + real-world physics)
- **Response first.** The touched control moves on the next frame from local state; slow work (theme, data) lands
  after it (`TRIGGER_LEAD_FRAMES`). Cause → effect, never both at once.
- **Model the real thing.** Mass → critically damped spring; water/air → kicked spring that only decelerates (Tide);
  liquid → viscous relaxation `v += (target − v)(1 − e^(−dt/τ))`; anything in a moving phone → inertia from the
  gyroscope (lags, then catches up). Keep the model pure Kotlin and unit-test it.
- **Reuse the trajectory.** An analytic trajectory (e.g. a critically damped spring's `v(t)`) drives the feedback too:
  haptics pre-computed from it, sound following it, a reversal retargeting both.
- **Continuous motion goes through the runtime** (SKILL non-negotiable 13): `rememberAmbient` + `ambient.visibility`.
  The clock handles visibility, foreground, battery saver, reduced motion, heat and the 30 fps calm rate. Measure it
  with `dumpsys gfxinfo` frame counts; simulate heat with `adb shell cmd thermalservice override-status 3` (`reset`).

## Verifying motion
Stills lie about motion. Record at real speed and review frames:
`adb shell screenrecord --size 640x1428 --bit-rate 16000000 --time-limit 4 /sdcard/x.mp4`, pull it, and extract frames
at fixed steps (an `AVAssetImageGenerator` helper on macOS, or ffmpeg). Raw `screencap` grabs are ~1 s apart and
stall under load: don't use them for timing. Frame-phase timings: `dumpsys gfxinfo <pkg> framestats` (the buffer holds
only the last 120 frames, so read it right after the interaction).
