# HaloSegmentedControl

> A selector you can grab. The toggle's ceramic (light) or graphite (dark) thumb slides along a sunken track,
> clicking into a detent at every segment, and the label it rests under takes the colour of what it means.

| Archetype | Status | Showroom | File |
|---|---|---|---|
| Toggle | Review | `halo://showroom/segmented` | `core/designsystem/.../component/HaloSegmentedControl.kt` |

**Design read:**
- *Object:* a sliding selector on a hi-fi or camera body: a knob-material slider in a recessed channel, with a detent per position.
- *Physics:* a heavy slider. It travels on the measured `Mechanical` spring (critically damped, never bounces). Flicks carry by
  their momentum, and pushing past an end compresses the thumb against the wall (rubber band) instead of letting it leave the track.
- *Touch and sound:* a detent click (`Tick`) per segment crossed. A tap clicks at once, and the travel past intermediate
  detents clicks softer.
- *Light:* reflected light only. It's lit by the world-fixed key light, and its contact shadow comes from that light. Nothing emits.

`HaloSegmentedControl · Toggle · picks one of 2–5 options (unit, network, fee speed) · the thumb is thrown along the track, clicks through each detent and lands without a bounce, and the label under it lights up in its meaning colour`

**Assumptions:**
- Segments are equal width (as on iOS). Long labels end in an ellipsis rather than resizing their neighbours.
- Selection commits on release, never mid-drag; `onSelect` fires once per gesture.
- A drag moves the thumb only when it starts on the thumb. Elsewhere, a tap selects and a vertical drag goes to the parent scroll.
- The press response is the thumb (lean and sink), not a container scale. The track is fixed hardware.
- Colour lives on the selected label and icon, not on a fill. A selector isn't an action, so it stays calmer than the brand blue.

## 1. Usage
- **Use for:**
  - a choice of 2–5 mutually exclusive options that applies immediately: sats / BTC / fiat, on-chain / Lightning,
    slow / normal / fast fee, chart range
- **Don't use for:**
  - on / off (use `HaloToggle`)
  - navigation between screens (`HaloTabBar`)
  - more than 5 options or long labels (a list of checkmark rows)
  - a choice that needs confirmation (buttons)
- **Colour:** each segment can carry a `HaloPaymentNetwork`. On-chain selects in orange, Lightning in yellow, and anything
  else in the accent blue.

## 2. Anatomy
Proportions are of the nominal housing height H (Medium `controlSm` 36, Large `controlMd` 48).
```
 ┌──────────── housing: full width × H, radius full, bevel 0.10H, raise 0.06H, body material ─────────────┐
 │ ┌──── channel: inset 0.08H, radius full, bevel 0.10H, depth 0.14H, recess floor + AO ───────────────┐ │
 │ │ (  ◉ On-chain  )      ⚡ Lightning                                                                 │ │
 │ │  thumb: inset 0.13H, height H − 0.26H, width (W − 0.26H) / n, knob material, bevel 0.30·th          │ │
 └─ contact shadows from the scene light: housing at 0.04H, thumb at 0.14H (pressed 0.06H) ──────────────┘
 Segment: label (typography.label) with an optional icon (iconSm 16, gap spacing.xs), padding spacing.sm horizontal.
 Layout height = max(housing, minTouch 48); every segment is tappable over the full 48dp height.
 At large font scales the housing grows to the label height + 2 × (0.13H + spacing.xs).
```

## 3. API
```kotlin
@Immutable
class HaloSegment(
    val label: String,
    val icon: ImageVector? = null,
    val network: HaloPaymentNetwork? = null,   // null = accent (UI choice)
)

enum class HaloSegmentedSize { Medium, Large }

@Composable
fun HaloSegmentedControl(
    segments: List<HaloSegment>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: HaloSegmentedSize = HaloSegmentedSize.Medium,
)
```
Give it a width (usually `fillMaxWidth()`); the segments share it equally.

## 4. Variants × sizes
| Segment colour | Selected label + icon | Other labels |
|---|---|---|
| Accent (network null) | `accent.text` | `content.secondary` |
| On-chain | `bitcoin.text` | `content.secondary` |
| Lightning | `lightning.text` | `content.secondary` |

Theme materials (shared with the toggle): dark has a soft-touch Ink850 housing, an Ink950 floor and a graphite thumb; light
has a ceramic housing and floor with a white ceramic thumb.

| Size | Housing H | Thumb height | Type | Icon |
|---|---|---|---|---|
| Medium | 36 (`controlSm`) | ≈ 26.6 | `label` | `iconSm` 16 |
| Large | 48 (`controlMd`) | ≈ 35.5 | `label` | `iconSm` 16 |

## 5. State matrix
| State | Visual delta | Motion | Haptic | Semantics |
|---|---|---|---|---|
| Rest | thumb under the selected segment; its label in the meaning colour | — | — | `Role.Tab`, selected |
| Press another segment | thumb leans 15% toward it | Snap | — | — |
| Press the selected segment | thumb sinks (shadow 0.14H → 0.06H) | Snap | — | — |
| Tap release | thumb travels to the new segment; label colours cross-fade with distance | Mechanical | `Tick` on release; softer passing ticks (0.45) at intermediate detents | selection moves |
| Press cancelled | lean returns | Mechanical | — | — |
| Drag (started on the thumb) | thumb follows the finger 1:1 and sinks | direct | `Tick` per detent crossed | — |
| Drag past an end | thumb stays at the wall and compresses (scaleX ≥ 0.88, pivot on the wall) | direct, rubber band | — | — |
| Drag release | carried by velocity × 0.12s to the nearest segment | Mechanical + initial velocity | `Tick` for any detent crossed on the way | `onSelect` once, if changed |
| Focused | 2dp `line.focus` ring around the focused segment | fade Fast | — | focusable |
| Disabled | whole control at `disabledAlpha`; no gestures | fade Base | — | disabled |

## 6. Tokens used
- **Sizes:** `controlSm`, `controlMd`, `minTouch`, `iconSm`.
- **Spacing:** `xs`, `sm`.
- **Typography:** `label`.
- **Colors:** `content.secondary`, `accent.text`, `bitcoin.text`, `lightning.text`, `line.focus`.
- **Materials and light:** `materials.body`, `materials.knob`, `materials.recessFloor`, `HaloTheme.light`.
- **Effects:** `disabledAlpha`. **Stroke:** `focus`, `focusOffset` (via `HaloIndication`).
- **Motion:** `Mechanical`, `Snap`, `Quick`; fade `Base`.
- **Haptics:** `Tick` (full and scaled with `haptics.tick`).
- **New tokens:** none.

## 7. Motion choreography
| Trigger | Property | From → To | Spec | Notes |
|---|---|---|---|---|
| press on another segment | thumb position | p → p + 0.15·sign | Snap | anticipation (the toggle's measured lean) |
| press on the thumb, or drag | thumb elevation | 1 → 0.43 | Snap | the shadow shortens and tightens |
| release | elevation | → 1 | Quick | |
| selection change | thumb position | → index | **Mechanical** (ζ 1.0, k 400, ~330ms) | retargets mid-flight |
| drag | position | follows the finger | direct `snapTo` | 1 px = 1 / segment width |
| drag past an end | thumb scaleX | 1 → ≥ 0.88 | direct | Apple's rubber band (c 0.55) over 0.24 of a segment, half of it as compression |
| drag release | position | → projected nearest | Mechanical + initial velocity | projection 0.12s, as for the toggle |
| any thumb motion | label colours | secondary ↔ meaning colour | draw phase | weight = 1 − distance to the thumb, no recomposition |
| enable / disable | alpha | 1 ↔ 0.38 | fade Base | |

## 8. Sound & haptics
A detent: one short, crisp click per position, quieter while the thumb is passing through on its own and full under the finger. The existing `Tick` token is exactly that (and `haptics.tick(intensity)` scales it), so no new pattern is needed.
| Moment | Token | Haptic | Sound | Timing |
|---|---|---|---|---|
| tap selects a new segment | `Tick` | crisp click | tick | on release, before the thumb moves |
| thumb passes an intermediate detent after a tap | `tick(0.45)` | lighter click | softer tick | at each half-way crossing |
| drag (or a thrown release) crosses a detent | `Tick` | crisp click | tick | at each half-way crossing |
| tap on the already-selected segment | none | — | — | — |
Pan: the segment's centre on screen (−1 left … +1 right).

## 8b. Light & emission
- **Lit by the key light (read in draw, turns with tilt):** housing rim, channel walls and floor AO, thumb relief, and both contact
  shadows. The thumb layer is translated, not redrawn, while it travels; it repaints only when it sinks or the light moves.
- **Emitted:** nothing. Colour is on the labels, which are reflected (never HDR-boosted).
- **Older phones:** `ReliefPainter` falls back to `CanvasRelief` (gradient bevels and soft shadows) with the same light.

## 9. Gestures
- **Tap:** `selectable` per segment over the full 48dp height. A cancelled press returns the lean.
- **Horizontal drag:** `draggable`, `startDragImmediately = false`, so a tap never also drags and vertical movement goes to the parent.
  It only moves the thumb when it starts on the thumb.
- **Release:** target = nearest of (position + velocity × 0.12s), clamped to the ends.
- **Interruption:** a new tap mid-travel retargets from the current position; a drag grabs the thumb wherever it is.

## 10. Accessibility
- `selectableGroup()` on the row; each segment is `Role.Tab` with `selected` and its label (the icon is decorative).
- The touch target is ≥ 48dp tall per segment.
- At 200% font scale the housing grows with the label; labels stay on one line and end in an ellipsis.
- In RTL the order mirrors, the thumb travels from the right and drags invert. The light stays top-left (world-fixed).
- D-pad and keyboard focus moves between segments with a focus ring; Enter selects.
- State is shown by position and colour, never colour alone.

## 11. Reduced motion
- Springs snap (`motion.spring` under `motion.reduced`); the lean is skipped.
- A drag still follows the finger, because that's direct manipulation, not animation.
- Haptics still play, but no passing ticks, since nothing travels.

## 12. Edge cases
- **Long labels:** one line with an ellipsis; the segment never grows.
- **Out-of-range `selectedIndex`:** clamped for drawing.
- **Fewer than 2 segments:** rendered, but the thumb can't move.
- **Rapid taps:** each retargets from the current position. Only the first crossing of each detent ticks.
- **Selection changed from outside mid-drag:** the drag wins until release, then the thumb syncs.
- **Disabled mid-drag:** the drag stops and the thumb returns to the selected segment.
- **Segments list changes:** the position is re-synced to `selectedIndex`.

## 13. Verification
- Lint: `lint_ds.py HaloSegmentedControl`.
- Unit tests: `SegmentPhysicsTest` covers nearest detent, release projection, rubber band and compression, and crossing detection.
- Screenshots: `shots/segmented.png` (dark), `shots/segmented-light.png`, `shots/segmented-legacy.png`.
- Motion: slow-mo captures mid-travel and mid-drag.
- Feedback: the haptic log shows `Tick` for a tap.
- Unverified: haptic feel on a real actuator, 120 Hz feel, tilt feel on a device.
