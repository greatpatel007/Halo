# HaloSheet

> A tray of soft-touch (ceramic in light) material that slides up over the screen for a flow or a detail; it feels
> like a physical slab you can throw, that is carried by its momentum and lands with a soft knock.

| Archetype | Status | Showroom | File |
|---|---|---|---|
| Container/Overlay | Review | `halo://showroom/sheet` | `core/designsystem/.../component/HaloSheet.kt` |

**Design read:** `HaloSheet · Container/Overlay · a tray for flows (send, receive, details) over the current screen · the
slab is thrown, carried by momentum and lands with a soft knock while the screen behind sinks back`

- **Object:** a slab sliding out from under the bottom edge.
- **Physics:** a mass on a critically damped spring, 1:1 with the finger, momentum projected on release, rubber band
  at the ends.
- **Touch and sound:** a light tick passing a resting point; a soft, low knock (`Land`) when it comes to rest.
- **Emits light:** no. The top edge catches the key light; its contact shadow follows the tilt.

**Assumptions:**
- A sheet taller than ~57% of the screen also rests half open (iOS medium/large).
- Short content gets one resting point: its own height (Family's "tray").
- Sheets render in the window's overlay layer (`HaloApp`), so the content behind can recede and the Tide sweeps them.

## 1. Usage
- **Use for:** multi-step flows that keep context (send, receive), transaction details, pickers, confirmations.
- **Don't use for:** short notices (`HaloToast`), whole screens (navigate), destructive confirmations that need a
  centred alert (`HaloDialog`, when built).

## 2. Anatomy
```
   screen behind: scale 0.95 · drop spacing.sm (8) · blur glassBlur×0.25 · scrim bg.scrim × openness
┌──────────────────────────────────────┐  radius.sheet (32), top corners only
│               ──── 36×4              │  grip: sizes.sheetHandle, line.strong, in a minTouch/2 (24) band
│ Title                                │  content padding: spacing.gutter (20) horizontal (contentPadding)
│ …content…                            │  max height: screen − status bar − spacing.x5 (56)
│                                      │  bottom: navigation bar ∪ IME inset
└──────────────────────────────────────┘  slab runs to the screen's bottom edge (no gap on overshoot / morph)
  fill bg.raised · edge line.default (hairline) · top light line.highlight (thin) · contact shadow (key light)
```

## 3. API
```kotlin
enum class HaloSheetValue { Hidden, Partial, Expanded }

class HaloSheetState { targetValue; currentValue; isVisible; fun show(); fun expand(); fun hide() }
@Composable fun rememberHaloSheetState(initialValue: HaloSheetValue = Hidden): HaloSheetState

@Composable
fun HaloSheet(
    state: HaloSheetState,
    modifier: Modifier = Modifier,
    title: String? = null,              // pane title for screen readers
    dismissible: Boolean = true,        // false: resists below its lowest point; no dim tap / back dismissal
    onDismiss: () -> Unit = {},         // after the sheet has left following a user dismissal
    contentPadding: PaddingValues = PaddingValues(horizontal = spacing.gutter),
    content: @Composable ColumnScope.() -> Unit,
)
```

## 4. Variants × sizes
| Content | Resting points |
|---|---|
| Shorter than ~57% of the screen | Expanded = its own height |
| Taller | Partial (half the screen) and Expanded (its height, capped below the status bar) |

One material in each theme: soft-touch slab (dark), ceramic (light). No size variants.

## 5. State matrix
| State | Visual delta | Motion | Haptic | Semantics |
|---|---|---|---|---|
| Hidden | not composed | — | — | — |
| Opening | rises from the bottom; dim fades in; screen recedes | Gentle | `Land` on arrival | pane title |
| Partial / Expanded | resting | — | — | custom actions Close, Expand |
| Dragging | 1:1 with the finger; rubber band past the ends | snap per frame | tick passing a point | — |
| Thrown | carried to the projected point, a hint of overshoot | Throw | `Land` on rest | — |
| Morphing (content changed) | height follows the content | Standard | — | — |
| Back preview | shrinks toward its bottom edge (≤ 0.92) | 1:1 with the gesture | tick on commit | — |
| Closing | slides down; dim and recede release | Gentle (placed) / velocity | tick | — |

## 6. Tokens used
- **Colours:** `bg.raised`, `bg.scrim`, `line.default`, `line.strong`, `line.highlight`.
- **Shape and size:** `radius.sheet`, `radius.full`, `sizes.sheetHandle` (**new**), `sizes.minTouch`,
  `spacing.gutter / sm / x3 / x5`, `stroke.hairline / thin`, `effects.glassBlur`.
- **Springs:** `Gentle`, `Standard`, `Quick`, `Throw` (**new**).
- **Haptics:** `Land` (**new**), `tick()`.
- **New tokens and why:**
  - `HaloSpring.Throw` (0.82 / 440): Apple's sheet spring. The only surface spring that may overshoot, and only
    after a throw.
  - `HaloHaptic.Land`: a resting surface needed a felt-not-heard knock that no existing token gives.
  - `sizes.sheetHandle` (36×4): the grip.

## 7. Motion choreography
| Trigger | Property | From → To | Spec | Notes |
|---|---|---|---|---|
| `show()` | top edge | hidden → lowest point | Gentle | Land on arrival |
| `expand()` / `hide()` | top edge | current → target | Gentle | |
| drag | top edge | follows the finger | snap | rubber band `(1 − 1/(x·0.55/d + 1))·d` past the ends |
| release | top edge | → point nearest `y + v/1000 · 0.998/(1 − 0.998)` | Throw if \|v\| > 1200 dp/s, else Gentle | starts at the release velocity |
| content height changes | top edge | → new height's point | Standard | Family tray morph |
| any | dim, recede | follow openness (0 … 1) | — | read in draw / layer only |
| back gesture | scale (bottom origin) | 1 → 0.92 | 1:1, Quick back | commit hides |

## 8. Sound & haptics
A slab on felt: passing a resting point is a faint detent; coming to rest is a soft, round knock felt more than heard.
| Moment | Token | Haptic | Sound | Timing |
|---|---|---|---|---|
| passes a resting point while dragging | `tick(0.35)` | light sharp tick | tiny click | at the crossing |
| comes to rest (open, after a drag or throw) | `Land` (new) | one soft round tap, intensity 0.42, sharpness 0.25 | muted low knock, 110 Hz, gain 0.12 | on arrival |
| dismissed (drag, dim tap, back) | `tick(0.3)` | light tick | tiny click | at the release / tap |

`Land` per tier: envelope and primitives render the soft tap (a low tick primitive on primitive phones); the amplitude
tier a short low-amplitude pulse; basic motors play nothing (`basic = null`), since it isn't a moment worth a buzz.

## 8b. Light & emission
The slab's top edge catches the key light (`line.highlight`). The contact shadow comes from `ReliefPainter`, offset by
the light's direction and read in draw, so it follows tilt. Nothing emits light.

## 9. Gestures
- **Drag:** vertical, on the whole sheet. Nested scroll hands over: the sheet moves up until it is fully open before
  content scrolls, and once content is back at its top, a downward drag moves the sheet.
- **Release:** project the landing with the velocity and choose the nearest resting point (or hidden when
  dismissible). So a short fast flick and a long slow drag both work.
- **Dim tap:** dismisses. **Back:** predictive, commit dismisses, cancel springs back.
- **Not dismissible:** the bottom limit becomes the lowest resting point, with a rubber band below it.

## 10. Accessibility
- `paneTitle` = `title`; the sheet is a traversal and focus group.
- Custom actions: Close (when dismissible), Expand (when it has a partial point). The dim has an onClick "Close".
- Back always works when dismissible. Content scrolls inside at 200% font scale. Layout mirrors in RTL (it's symmetric).

## 11. Reduced motion
Springs snap (`motion.reduced`): open, close and morph are instant. The dim and recede still follow openness. Haptics
still play.

## 12. Edge cases
- **Rapid `show` / `hide`:** each request retargets the running animation.
- **Content changes mid-drag:** the morph waits for the release.
- **A throw past the top:** overshoot is drawn by the slab running to the bottom edge, so no gap appears.
- **IME:** the bottom inset includes the keyboard.
- **Outside `HaloApp`:** the sheet draws where it is declared (previews).
- **Android 8–11:** no blur, the screen behind is only dimmed and scaled.
