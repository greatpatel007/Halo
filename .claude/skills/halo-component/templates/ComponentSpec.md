# <Name>

> <One sentence: what it is and the feeling it should create.>

| Archetype | Status | Showroom | File |
|---|---|---|---|
| <Pressable · Toggle · Input · Value display · Gesture-commit · Container/Overlay · Feedback> | Draft | `halo://showroom/<id>` | `core/designsystem/.../component/<Name>.kt` |

**Design read:** `<Name> · <archetype> · <job> · <signature moment>`

**Assumptions:** <anything decided without asking>

## 1. Usage
- **Use for:** …
- **Don't use for:** … (and what to use instead)

## 2. Anatomy
```
┌──────────────────────────────────────┐  height: sizes.controlXl (64)
│ [icon 20] ·8· Label            [24] │  padding: spacing.xl (20) horizontal
└──────────────────────────────────────┘  radius: radius.full
```
Name every part, with its token.

## 3. API
```kotlin
@Composable
fun <Name>(
    ...,
    modifier: Modifier = Modifier,
    ...
)
```

## 4. Variants × sizes
| Variant | Fill | Content | Edge | Glow |
|---|---|---|---|---|

| Size | Height | Padding | Type | Icon |
|---|---|---|---|---|

## 5. State matrix
| State | Visual delta | Motion | Haptic | Semantics |
|---|---|---|---|---|
| Default | | | | |
| Pressed | | | | |
| Focused | | | | |
| Disabled | | | | |
| … | | | | |

## 6. Tokens used
Colors, type, spacing, sizes, radius, stroke, effects, springs and haptics, by name. List any **new tokens** proposed, with the reason.

## 7. Motion choreography
| Trigger | Property | From → To | Spec | Notes |
|---|---|---|---|---|
| press | scale | 1 → 0.97 | Snap | via HaloPressable |

## 8. Haptics
| Moment | Token | Timing |
|---|---|---|

## 9. Gestures
Type, thresholds, velocity handling, cancellation. Write "none" if tap-only.

## 10. Accessibility
Role, label, stateDescription, custom actions, touch target, 2× font behaviour, RTL behaviour, focus.

## 11. Reduced motion
What snaps, what stays, what is removed.

## 12. Edge cases
Long text, zero/huge values, rapid repeated input, interruption mid-animation, disabled while animating.

## 13. Verification
- Lint: …
- Screenshot: `shots/<id>.png`. Review notes against `pixel-perfect.md`.
- Unverified: …
