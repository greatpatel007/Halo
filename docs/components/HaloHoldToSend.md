# HaloHoldToSend

> Sending money should feel deliberate and irreversible in the best way: you hold, energy visibly and
> physically charges across the control, it clicks into place, and the payment resolves into a check.

| Archetype | Status | Showroom | File |
|---|---|---|---|
| Gesture-commit | Review | `halo://showroom/hold-to-send` | `core/designsystem/.../component/HaloHoldToSend.kt` |

**Design read:** `HaloHoldToSend · Gesture-commit · confirms a Bitcoin payment with a deliberate 1.2s hold · an orange charge stretches across the pill (the label inverts as the fill passes under it), locks with a click, then the pill collapses into a circle where a check draws itself on amid a spark burst`

**Assumptions:**
- The parent owns the payment lifecycle via `state` (`Idle → Sending → Success`, or back to `Idle` on failure). The component owns only the visual charge, lock and collapse.
- The component locks visually the instant the hold completes (it doesn't wait for the parent), so the commit feels immediate.
- It fills its width by default: a commit control is always a full-width, bottom-anchored action.
- The label verb is a parameter (default "Hold to send") so the parent can say "Insufficient funds" while disabled.

## 1. Usage
- **Use for:** the final confirmation of any outgoing Bitcoin/Lightning payment.
- **Don't use for:**
  - non-monetary confirmations (use a `HaloButton` + dialog)
  - reversible actions
  - anything that isn't Bitcoin (it is orange by definition)

## 2. Anatomy
```
┌───────────────────────────────────────────────────────────┐ height controlXl 64, radius full
│ ┌──────┐                                                  │ track: bitcoin.tint + line.default hairline
│ │ ⚡    │→→ fill grows    Hold to send 21 000 sats          │ inner pad xs 4 → fill pill (radius full)
│ └──────┘                                                  │ thumb Ø = 64 − 2×4 = 56, bolt icon iconLg 24 centred in the thumb
└───────────────────────────────────────────────────────────┘ label centred in the space right of the thumb
 label = label (Satoshi Bold 15) + amount (Paper Mono Medium 15, narrowed groups) + "sats" (Paper Mono)
 label colour: content.primary over the track, bitcoin.onFill where the fill passes under (pixel-exact clip)
 glow: bitcoin.glow, intensity 0.35 → 1 with the charge
 locked: track collapses to a 64dp circle (bitcoin.fill); comet spinner (iconLg) or check (iconXl, draw-on)
 burst: 14 sparks, radius → 1.6 × circle, bitcoin.fill / bitcoin.text / content.primary, fade out
```

## 3. API
```kotlin
enum class HoldToSendState { Idle, Sending, Success }

@Composable
fun HaloHoldToSend(
    sats: Long,
    state: HoldToSendState,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Hold to send",
    interactionSource: MutableInteractionSource? = null,
)
```

## 4. Variants × sizes
One variant (Bitcoin) and one size (`controlXl`): a commit control should be unmistakable, not configurable.

## 5. State matrix
| State | Visual | Motion | Haptic | Semantics |
|---|---|---|---|---|
| Idle | thumb pill at left, label | — | — | Button, "Hold to send 21,000 sats", action "Send" |
| Engaged (finger down) | scale 0.98, touch-origin light | Snap | Crescendo | — |
| Progressing | fill stretches left → right from the thumb, the label inverts under the fill, glow rises | timed 1200ms linear (resumes from the current charge) | tick(0.4 … 0.9) every 10% from 40% | — |
| Cancelled (release < 100%) | fill springs back to the thumb | Quick | `cancel()` | — |
| Threshold / locked | full fill, label fades, pill collapses to a circle | Standard | Threshold (once) | `onCommit()` |
| Sending | orange circle + comet spinner | spinner timed 900ms/rev | — | stateDescription "Sending" |
| Success | spinner fades, check draws on, 14-spark burst | check Quick spring, burst timed 700ms decel | Celebrate | stateDescription "Sent" |
| Back to Idle (failure / reset) | circle expands to a pill, fill retracts | Standard / Quick | — (the parent plays Reject) | |
| Disabled | alpha 0.38, glow off, gestures ignored | fade Base | none | disabled |

## 6. Tokens used
- **Colors:** `bitcoin.fill`, `bitcoin.onFill`, `bitcoin.text`, `bitcoin.tint`, `bitcoin.glow`, `content.primary`, `line.default`.
- **Type:** `label`, `amountSmall`.
- **Sizes:** `controlXl`, `iconLg`, `iconXl`.
- **Spacing:** `xs`, `sm`, `xxs`.
- **Stroke:** `thin`, `icon`.
- **Effects:** `glowRadius`, `glowSpread`, `disabledAlpha`.
- **Motion:** springs `Snap`, `Quick`, `Standard`; `timed(1200)`, `timed(700, EmphasizedDecelerate)`; fade `Base`.
- **Haptics:** `Crescendo`, `tick(i)`, `Threshold`, `Celebrate`.
- **New tokens:** none. The spinner was lifted to `foundation/HaloSpinner.kt` (shared with `HaloButton`).

## 7. Motion choreography
| Trigger | Property | From → To | Spec | Notes |
|---|---|---|---|---|
| finger down | scale | 1 → 0.98 | Snap / Quick | `pressScale` via a manual `PressInteraction` |
| hold | charge | current → 1 | timed 1200 × (1 − current) | linear: progress = time |
| hold | glow | 0.35 → 1 | follows the charge | draw-phase intensity |
| release < 1 | charge | current → 0 | Quick | interruptible: re-press resumes |
| charge = 1 | collapse | 0 → 1 | Standard | label alpha = 1 − collapse |
| Success | check | 0 → 1 | Quick | PathMeasure draw-on |
| Success | burst | 0 → 1 | timed 700, EmphasizedDecelerate | skipped when `motion.reduced` |
| state → Idle | collapse, charge, check | → 0 | Standard / Quick / snap | |

## 8. Haptics
| Moment | Token | Timing |
|---|---|---|
| finger down | Crescendo | immediately |
| charge 40% … 90% | tick(charge) | at each 10% crossing |
| charge 100% | Threshold | the frame the lock happens, before `onCommit` |
| release early | `haptics.cancel()` | on release |
| state → Success | Celebrate | as the check starts drawing |

## 9. Gestures
- Press-and-hold (`awaitEachGesture`).
- A scroll that starts on the control cancels the hold: the pointer is consumed, so `waitForUpOrCancellation` returns null.
- Leaving the bounds cancels it.
- Multi-touch is ignored.
- Hold time is 1200ms (scaled by slow-mo). It is not removed by reduced motion, because it is the intent check.

## 10. Accessibility
- `clearAndSetSemantics` gives one node:
  - `Role.Button`
  - contentDescription "Hold to send 21,000 sats"
  - `onClick(label = "Send")`
  - custom action "Send 21,000 sats"
  - stateDescription "Sending" / "Sent" / "Unavailable"
- TalkBack users commit with a double-tap or the action, never the gesture.
- The touch target is 64dp tall and full width.
- At 2× font, the label ellipsizes before the amount does (the amount is never truncated).
- RTL: the fill direction mirrors (it grows from the start edge).

## 11. Reduced motion
- The charge still takes 1200ms (essential).
- Collapse, check and springs snap.
- The particle burst is removed.
- The glow is static.

## 12. Edge cases
- Repeated press-release-press: the charge resumes from its current value.
- A hold that completes while the parent is already `Sending`: ignored (not interactive unless Idle).
- The parent flips to Success before the collapse finishes: the animations run in parallel.
- A huge amount (21M BTC in sats): the label ellipsizes the verb.
- `enabled = false` mid-charge: the charge is cancelled and springs back.

## 13. Verification
- Lint: OK.
- Screenshots: `shots/hold-to-send.png` (idle + matrix), `shots/hold-to-send-charging.png` (mid-hold), `shots/hold-to-send-success.png`.
- Unverified: how the haptics feel on a device. The token order is verified in the haptic log.
