# HaloRollingAmount

> A balance that feels alive: each digit rolls on its own spring like a precision odometer, units glide
> between sats, BTC and fiat, and one tap masks it digit by digit.

| Archetype | Status | Showroom | File |
|---|---|---|---|
| Value display | Review | `halo://showroom/rolling-amount` | `core/designsystem/.../component/HaloRollingAmount.kt` |

**Design read:** `HaloRollingAmount · Value display · shows a Bitcoin amount in sats / BTC / fiat · digits roll individually in the direction of change, staggered right-to-left; privacy mask morphs digits to dots`

**Assumptions:**
- The component receives raw `sats: Long` plus a display rate and formats via `AmountFormat`. The rate is a display input, not business logic.
- Only digits roll; separators and units crossfade.

## 1. Usage
- **Use for:** balances, amount summaries, live fiat equivalents, keypad amount echo.
- **Don't use for:** dense lists with many simultaneous updates (use static `HaloText` with `amountSmall`), or non-monetary counters.

## 2. Anatomy
```
 [₿ glyph]·sm·[d][d][␣][d][d][d][␣][d][d][d]·sm·[sats]
   │            └ digit cells: width = mono advance of "0", height = style line box, clipped
   │              group gaps / . / , cells: 0.5 × advance (narrowed, glyph centered)
   └ BTC only: HaloIcons.Bitcoin, cap-height size, bitcoin.text, ink bottom on the baseline
 suffix (Sats only): "sats", amountMedium for ≥32sp styles else data, content.secondary, baseline-aligned
 BTC: insignificant trailing zeros in content.tertiary
```

## 3. API
```kotlin
@Composable
fun HaloRollingAmount(
    sats: Long,
    modifier: Modifier = Modifier,
    unit: AmountUnit = AmountUnit.Sats,
    fiatPerBtc: Double = 0.0,
    style: TextStyle = HaloTheme.typography.amountHero,
    hidden: Boolean = false,
    tickOnChange: Boolean = false,
)
```

## 4. Variants × sizes
Size follows `style`: `amountHero` (balance), `amountLarge` (entry), `amountMedium` (cards), `amountSmall` (rows).
Units:

| Unit | Example | Extras |
|---|---|---|
| Sats | `4 812 350 sats` | narrowed group gaps, suffix |
| Btc | `₿ 0.04812350` | vector ₿, dimmed trailing zeros |
| Fiat | `$5,016.34` | locale currency format |

## 5. State matrix
| State | Visual | Motion | Haptic | Semantics |
|---|---|---|---|---|
| Value | static digits | — | — | "4,812,350 sats" |
| Increasing | changed digits roll **up** (new from below) | Digit spring, 18ms stagger right→left | `tick` if `tickOnChange` | updated description |
| Decreasing | changed digits roll **down** | same | same | |
| Digit count grows | new leftmost cells fade in | fade Base | — | |
| Unit switch | whole row slides vertically + crossfades, width animates | Standard + fade Fast | — | |
| Hidden | each digit fades/rises out to "•", separators dim to 50% | Standard, 20ms stagger | — | "Amount hidden" |

## 6. Tokens used
- **Type:** `amountHero` / `amountLarge` / `amountMedium` / `amountSmall` / `data`.
- **Colors:** `content.primary` (via `LocalHaloContentColor`), `content.secondary`, `content.tertiary`, `bitcoin.text`.
- **Spacing:** `sm`.
- **Springs:** `Digit`, `Standard`. **Fades:** `Fast`, `Base`.
- **Haptic:** `tick(0.5)`.
- **New tokens:** none.

## 7. Motion choreography
| Trigger | Property | From → To | Spec | Notes |
|---|---|---|---|---|
| value change | digit position (continuous) | old → new (mod 10, direction by sign) | Digit (ζ 0.75) | delay `fromRight × 18ms` (scaled) |
| roll in flight | glyph alpha | 1 − distance from centre | derived | soft motion-blur feel |
| new cell | alpha | 0 → 1 | fade Base | only after first composition |
| unit change | content | slide ±⅓ height + fade | Standard / fade Fast | `AnimatedContent` + `SizeTransform` |
| hide / show | mask | 0 ↔ 1 | Standard, 20ms stagger | digits rise 30% + fade; "•" fades in |

## 8. Haptics
| Moment | Token | Timing |
|---|---|---|
| user-driven change (`tickOnChange = true`) | `tick(0.5)` | once per change, at roll start |

Live/background updates never tick.

## 9. Gestures
None.

## 10. Accessibility
- One merged node with `contentDescription` (the spoken amount, or "Amount hidden"). The cells are invisible to TalkBack.
- At 2× font, cells scale with the style (sp). The row may wrap only via its container (it doesn't wrap itself).
- RTL: numbers stay LTR.

## 11. Reduced motion
Digit, unit and mask animations snap. Values remain correct.

## 12. Edge cases
- 0 sats.
- 21M BTC (16 digits).
- A negative value shows a leading "-" glyph cell.
- A rate of 0 in Fiat shows "$0.00".
- A rapid series of changes: each digit retargets from its current position, wrapping in the right direction.
- A length change shifts cells; they are keyed from the right so existing digits stay put.

## 13. Verification
- Lint: OK.
- Screenshots: `shots/rolling-amount.png` and `shots/rolling-amount-rolling.png` (mid-roll).
- Unverified: how the tick feels on a device.
