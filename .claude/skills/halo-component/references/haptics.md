# Haptics

Haptics are part of the design, not decoration. Halo speaks one vocabulary (`HaloHaptic`). It goes through
`View.performHapticFeedback` (honours the user's touch-feedback setting) or `VibrationEffect.Composition` with
`USAGE_TOUCH`. Never call platform haptics directly.

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

## Token → platform mapping (API 33 baseline, 34+ enhancements)
| Token | Mapping | Fallback |
|---|---|---|
| Tap | `CONTEXT_CLICK` | — |
| Tick | `SEGMENT_FREQUENT_TICK` (34+) | `CLOCK_TICK` |
| ToggleOn / ToggleOff | `TOGGLE_ON` / `TOGGLE_OFF` (34+) | `CONTEXT_CLICK` / `CLOCK_TICK` |
| Threshold | `GESTURE_THRESHOLD_ACTIVATE` (34+) | `LONG_PRESS` |
| Confirm / Reject | `CONFIRM` / `REJECT` | — |
| Crescendo | Composition `SLOW_RISE 0.5` → `QUICK_RISE 0.9` | `LONG_PRESS` |
| Celebrate | Composition `THUD 1` → `SPIN 0.6` → `TICK 0.4` | `CONFIRM` |
| `tick(i)` | Composition `TICK` at scale `i` | `CLOCK_TICK` |

## Verifying
- The emulator cannot vibrate. Every played token appears in the Showroom **haptic log** (dock → log), so you can
  verify *which* token fires *when*. Report the timing you observed.
- How a haptic feels needs a physical Android 13+ device. List that as unverified in the report unless the user confirms.
