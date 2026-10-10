# Component registry

Every design-system component, in build order. Maintained by the `halo-component` skill (step 7) and checked by
`lint_ds.py`. Columns: name = Kotlin file / main composable, status ∈ Draft · Review · Stable.

| Name | Archetype | Status | File | Showroom |
|---|---|---|---|---|
| HaloButton | Pressable | Review | `component/HaloButton.kt` | `halo://showroom/button` |
| HaloRollingAmount | Value display | Review | `component/HaloRollingAmount.kt` | `halo://showroom/rolling-amount` |
| HaloHoldToSend | Gesture-commit | Review | `component/HaloHoldToSend.kt` | `halo://showroom/hold-to-send` |
| HaloToggle | Toggle | Review | `component/HaloToggle.kt` | `halo://showroom/toggle` |
| HaloLogo | Feedback (brand mark) | Draft | `component/HaloLogo.kt` | `halo://showroom/logo` |

## Foundations (not components; listed for reuse)
| Primitive | What it gives you |
|---|---|
| `HaloPressable` | Press scale, touch-origin highlight, haptic and semantics for anything tappable |
| `Modifier.pressScale` / `HaloIndication` | The press response parts, for custom gesture surfaces |
| `HaloText` | Text with trimmed line boxes, inheriting `LocalHaloContentColor` |
| `HaloSurface` / `Modifier.depth` | Depth levels L0–L3 (tone, hairline, top light, glass) |
| `rememberHaloBackdrop` + `backdropSource` / `backdropGlass` | Hand-built backdrop blur |
| `Modifier.haloGlow` | Light emitted by an element (instead of shadows) |
| `Modifier.haloGrain` | AGSL film grain for hero surfaces |
| `Modifier.enterRise` | Standard staggered enter |
| `AmountFormat` | Sats grouping, BTC trailing-zero dimming, fiat |
| `HaloIcon` + `HaloIcons` | The 24dp hand-drawn icon set |
| `HaloPreview` | Themed @Preview wrapper |
