# HaloToggle

> A switch you can almost feel. A ceramic (light) or graphite (dark) thumb with two finger dishes slides in a
> sunken channel, leans under your finger, and uncovers a velvet fill that brightens as it leaves the thumb's
> shadow. Built from `Refs/toggle.mp4` (measurements in `docs/research/0001-reference-analysis.md`).

| Archetype | Status | Showroom | File |
|---|---|---|---|
| Toggle | Review | `halo://showroom/toggle` | `core/designsystem/.../component/HaloToggle.kt` |

**Design read:** `HaloToggle · Toggle · switches a setting (and the app theme) · a ceramic/graphite thumb with finger dishes slides on the measured Mechanical spring, leans under the finger, and uncovers a velvet fill that brightens as it leaves the thumb's shadow`

**Assumptions:**
- `checked` = thumb at the end edge (fill visible).
- The press response is the thumb leaning and sinking, not a container scale. A switch housing is fixed hardware: *controls sink*.
- The label is optional. A switch usually sits in a labelled row, which merges semantics.

## 1. Usage
- **Use for:** binary settings that apply immediately, including the appearance (dark/light) switch.
- **Don't use for:**
  - choices that need confirmation (use a button)
  - one of several options (segmented control)
- **Tones:** Accent (blue) for settings; Bitcoin for on-chain settings; Lightning for Lightning settings.

## 2. Anatomy
All proportions are of housing height H, taken from the reference frame.
```
 ┌──────────────── housing W × H (switchMd 80×30, switchLg 112×42) ── raised rim, bevel 0.16H, body material
 │ ┌──────────── channel: inset 0.20H, radius full, bevel 0.12H, depth 0.18H, recess floor + AO + rim self-shadow
 │ │ ▓▓▓▓▓▓▓▓▓[( ◯  ◯ )]    velvet fill (texture layer) from the start edge to the thumb centre
 │ │          thumb: inset 0.11H, height 0.78H, width 1.92 × height, knob material, bevel 0.30·th
 │ │          dishes: concentric with the thumb's rounded ends, r 0.38·th, depth 0.10·th (concave)
 └─ contact shadows from the scene light: housing at elevation 0.06H, thumb at 0.16H (pressed 0.07H)
 Layout height = max(H, 48dp touch target), housing centred.
```

## 3. API
```kotlin
enum class HaloToggleTone { Accent, Bitcoin, Lightning }
enum class HaloToggleSize { Medium, Large }

@Composable
fun HaloToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: HaloToggleTone = HaloToggleTone.Accent,
    size: HaloToggleSize = HaloToggleSize.Medium,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource? = null,
)
```

## 4. Variants × sizes
| Tone | Velvet top → bottom | Rim (AO) |
|---|---|---|
| Accent | `accent.fill` → fill × (0.86, 0.72, 0.62) | bottom × (0.55, 0.45, 0.45) |
| Bitcoin | `bitcoin.fill` → same derivation | same |
| Lightning | `lightning.fill` → same derivation | same |

| Size | Housing | Thumb | Travel |
|---|---|---|---|
| Medium | 80 × 30 | 45 × 23.4 | ≈ 28dp |
| Large | 112 × 42 | 63 × 32.8 | ≈ 40dp |

Theme materials: dark = soft-touch Ink850 housing, Ink950 channel floor, graphite Ink600 satin thumb; light =
ceramic Ceramic050 housing, Ceramic150 floor, Ceramic000 thumb.

## 5. State matrix
| State | Visual | Motion | Haptic | Semantics |
|---|---|---|---|---|
| Off | thumb at the start edge; the fill hides under the thumb | — | — | Role.Switch, Off |
| On | thumb at the end edge; velvet fill visible from the start edge | — | — | On |
| Pressed | thumb leans 15% toward the opposite state and sinks (shadow 0.16H → 0.07H) | Snap | — | — |
| Released (tap) | thumb completes to the new state, fill extends or retracts with it | Mechanical | ToggleOn / ToggleOff | value flips |
| Dragging | thumb follows the finger 1:1, fill follows | none (direct) | — | — |
| Drag release | snaps to the nearest state, with the velocity projected forward | Mechanical with initial velocity | On/Off if changed | value flips if changed |
| Focused | 2dp `line.focus` ring, 2dp outside the housing | fade Fast | — | focusable |
| Disabled | whole switch at `disabledAlpha`, no gestures | fade Base | none | disabled |

## 6. Tokens used
- **Sizes:** `switchMd`, `switchLg`, `minTouch`.
- **Materials:** `materials.body`, `materials.knob`, `materials.recessFloor`, `materials.velvetGrain`.
- **Light:** `HaloTheme.light` (key light top-left, shadow tone and opacity).
- **Colors:** `accent.fill`, `bitcoin.fill`, `lightning.fill`, `line.focus`.
- **Effects:** `disabledAlpha`. **Stroke:** `focus`, `focusOffset`.
- **Motion:** `Mechanical` (new, measured), `Snap`, `Quick`; fade `Fast` / `Base`.
- **Haptics:** `ToggleOn`, `ToggleOff`.
- **New tokens:**
  - `HaloSpring.Mechanical`
  - `HaloSizes.switchMd` / `switchLg`
  - `HaloMaterials` (body, knob, recessFloor, velvetGrain)
  - `HaloLight`
  - the light palette and the `lightning` accent family

## 7. Motion choreography
| Trigger | Property | From → To | Spec | Notes |
|---|---|---|---|---|
| press | thumb position | p → p ± 0.15 (toward the opposite state) | Snap | anticipation, as measured (OFF creep) |
| press | thumb elevation | 1 → 0.44 (0.16H → 0.07H) | Snap | the shadow shortens and tightens |
| release / value change | thumb position | → 0 or 1 | **Mechanical** (ζ 1.0, k 400, ~330ms) | no overshoot |
| release | elevation | → 1 | Quick | |
| drag | position | follows the finger | direct `snapTo` | 1 px of drag = 1/travel |
| drag stop | position | → nearest, projected with velocity × 0.12s | Mechanical + initial velocity | |
| any thumb motion | fill extent | tracks the thumb centre | draw phase | the shadow on the fill moves with the thumb, so the fill "brightens as uncovered" |

## 8. Haptics
| Moment | Token | Timing |
|---|---|---|
| tap release flips on / off | ToggleOn / ToggleOff | at commit, with the value change |
| drag release changes state | ToggleOn / ToggleOff | at release |

## 9. Gestures
- Tap: `toggleable`. A cancelled press returns the thumb.
- Horizontal drag: `draggable` with `startDragImmediately = false`, so a drag never also fires a click. Any angle beyond touch slop that isn't horizontal is left to the parent scroll.

## 10. Accessibility
- `Role.Switch` with the on/off state from `toggleable`, plus the optional `contentDescription`.
- The touch target is ≥ 48dp tall: layout reserves it and pointer input extends to it.
- Font scale doesn't apply (no text).
- In RTL, the thumb's "on" position is the start (left) edge and the fill grows from the right. The lighting stays top-left, because the light is world-fixed.

## 11. Reduced motion
- Springs snap.
- The lean is skipped: position springs are `snap()` under `motion.reduced`.
- The state still changes instantly, with the haptic.

## 12. Edge cases
- Rapid taps: every value change retargets from the current position.
- The value changes from outside mid-drag: the drag wins until release, then the thumb syncs.
- Disabled mid-press: the press is cancelled and the thumb returns.

## 13. Verification
- Lint: OK.
- Screenshots: `shots/toggle-dark.png` and `shots/toggle-light.png` (state matrix), `shots/toggle-pressed.png`, `shots/toggle-mid.png`.
- Zoomed pixel review of the dishes, channel rim, fill edge and shadows in both themes (`halo.py zoom`).
- Unverified: how the haptics feel (on device).
