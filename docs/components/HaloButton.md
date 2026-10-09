# HaloButton

> The primary way to act. It feels like pressing a soft, lit key: it squeezes, its corners soften under the
> thumb, and when work starts it collapses into a spinning orb.

| Archetype | Status | Showroom | File |
|---|---|---|---|
| Pressable | Review | `halo://showroom/button` | `core/designsystem/.../component/HaloButton.kt` |

**Design read:** `HaloButton · Pressable · triggers an action in 4 variants × 4 sizes · press softens the pill's corners while a touch-origin light blooms; loading morphs the pill into a circle with a hand-drawn spinner`

**Assumptions:**
- The label is a `String` (not a slot), which enforces consistent type.
- The button wraps its content unless the caller gives it a fixed or filling width.

## 1. Usage
- **Use for:** single, clear actions ("Continue", "Review", "Receive").
- **Don't use for:**
  - committing a payment (use `HaloHoldToSend`)
  - toggling state (Toggle archetype)
  - navigation rows (use a pressable row)
- **Variant rules:**
  - One `Primary` per view.
  - `Bitcoin` only when the action *is* a Bitcoin/Lightning action ("Pay invoice", "Open channel").
  - `Ghost` for tertiary actions.

## 2. Anatomy
```
           ┌─ glow (Primary/Bitcoin): effects.glowRadius 20, spread 2, intensity 0.55 → 1 on press
┌──────────▼───────────────────────────────┐  height: size token (36 / 48 / 56 / 64)
│ ·pad· [icon] ·sm 8· Label ·sm 8· [icon] ·pad· │  pad: md 12 / lg 16 / xl 20 / xxl 24
└──────────────────────────────────────────┘  radius: height/2 (pill) → pressed radius (sm/md/md/lg)
 loading: width → height (circle), label fades out, spinner (icon size, stroke.indicator) fades in
```
Small (36dp visual) reserves a 48dp-tall layout slot (`sizes.minTouch`).

## 3. API
```kotlin
@Composable
fun HaloButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HaloButtonVariant = HaloButtonVariant.Primary,
    size: HaloButtonSize = HaloButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    interactionSource: MutableInteractionSource? = null,
)
```

## 4. Variants × sizes
| Variant | Fill (rest → pressed) | Content | Edge | Glow |
|---|---|---|---|---|
| Primary | accent.fill → accent.fillPressed | accent.onFill | — | accent.glow |
| Secondary | bg.raised | content.primary | line.default + line.highlight (top light) | — |
| Ghost | transparent | content.primary | — | — |
| Bitcoin | bitcoin.fill → bitcoin.fillPressed | bitcoin.onFill | — | bitcoin.glow |

| Size | Height | H-padding | Type | Icon | Pressed radius |
|---|---|---|---|---|---|
| Small | controlSm 36 | md 12 | label | iconSm 16 | radius.sm 12 |
| Medium | controlMd 48 | lg 16 | label | iconMd 20 | radius.md 16 |
| Large | controlLg 56 | xl 20 | label | iconMd 20 | radius.md 16 |
| XLarge | controlXl 64 | xxl 24 | label | iconLg 24 | radius.lg 20 |

## 5. State matrix
| State | Visual delta | Motion | Haptic | Semantics |
|---|---|---|---|---|
| Default | as above; glow 0.55 | — | — | Role.Button, label = text |
| Pressed | scale 0.97, corners → pressed radius, fill → fillPressed, glow → 1, highlight from touch point | Snap in, Quick out | — | — |
| Clicked | — | — | Tap | onClick |
| Focused | 2dp line.focus ring, 2dp outside | fade Fast | — | focusable |
| Disabled | whole button at effects.disabledAlpha, glow 0 | fade Base | none | disabled |
| Loading | width → height, label alpha → 0, spinner alpha → 1 and rotating | Standard (morph), timed 900ms/rev (spin) | none | stateDescription "Loading", not clickable |

## 6. Tokens used
- **Colors:** `accent.*`, `bitcoin.*`, `bg.raised`, `content.primary`, `line.default`, `line.highlight`, `line.focus`.
- **Type:** `label`.
- **Sizes:** `controlSm`, `controlMd`, `controlLg`, `controlXl`, `minTouch`, `iconSm`, `iconMd`, `iconLg`.
- **Spacing:** `sm`, `md`, `lg`, `xl`, `xxl`.
- **Radius:** `sm`, `md`, `lg`.
- **Stroke:** `thin`, `indicator`.
- **Effects:** `glowRadius`, `glowSpread`, `disabledAlpha`.
- **Springs:** `Snap`, `Quick`, `Standard`. **Fades:** `Base`. **Timed:** 900ms spinner revolution.
- **Haptic:** `Tap`.
- **New tokens:** none.

## 7. Motion choreography
| Trigger | Property | From → To | Spec | Notes |
|---|---|---|---|---|
| press | scale | 1 → 0.97 | Snap (in) / Quick (out) | `HaloPressable`, ≥ 90ms flash on quick taps |
| press | corner radius | h/2 → pressed radius | same press progress | layer-phase clip + draw |
| press | fill, glow | rest → pressed | same press progress | Primary / Bitcoin |
| loading on | width | rest → height | Standard | layout phase; neighbours reflow unless width is fixed |
| loading on | label alpha / spinner alpha | 1 → 0 / 0 → 1 | follows the morph progress | spinner composes only while visible |
| loading off | reverse of the above | | Standard | |
| enabled change | alpha | 1 ↔ 0.38 | fade Base | |

## 8. Haptics
| Moment | Token | Timing |
|---|---|---|
| click | Tap | on release (via `HaloPressable`) |

## 9. Gestures
Tap only.

## 10. Accessibility
- `Role.Button`. The label is the text, so icons are decorative.
- Loading sets `stateDescription = "Loading"` and disables the click.
- Touch target is ≥ 48dp (Small reserves 48dp height; Compose extends touch bounds).
- At 2× font, the label grows and the button stays single-line with ellipsis when width-constrained.
- In RTL, the leading/trailing icons swap sides automatically (Row).

## 11. Reduced motion
- The scale, morph and width morph snap.
- Fades shorten to Fast.
- The spinner still rotates: it is functional progress, not decoration.

## 12. Edge cases
- **Very long label in a fixed width:** ellipsizes.
- **Loading toggled rapidly:** the morph retargets from its current width.
- **Disabled while loading:** stays collapsed, dimmed.
- **Empty text with an icon:** acts as an icon button (still pass a label for accessibility).

## 13. Verification
- Lint: OK.
- Screenshot: `shots/button.png` (+ `shots/button-pressed.png`).
- Unverified: how the Tap haptic feels on a physical device.
