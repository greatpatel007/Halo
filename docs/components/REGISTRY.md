# Component registry

Every design-system component, in build order. Maintained by the `halo-component` skill (step 7) and checked by
`lint_ds.py`. Columns: name = Kotlin file / main composable, status ∈ Draft · Review · Stable.

| Name | Archetype | Status | File | Showroom |
|---|---|---|---|---|
| HaloButton | Pressable | Review | `component/HaloButton.kt` | `halo://showroom/button` |
| HaloRollingAmount | Value display | Review | `component/HaloRollingAmount.kt` | `halo://showroom/rolling-amount` |
| HaloHoldToSend | Gesture-commit | Review | `component/HaloHoldToSend.kt` | `halo://showroom/hold-to-send` |
| HaloToggle | Toggle | Review | `component/HaloToggle.kt` | `halo://showroom/toggle` |
| HaloLogo | Feedback (brand mark) | Review | `component/HaloLogo.kt` | `halo://showroom/logo` |
| HaloSheet | Container / Overlay | Review | `component/HaloSheet.kt` | `halo://showroom/sheet` |

## Planned (build order)
Components still to build, one at a time in this order (P1 unlocks the Home, Send and Receive demo screens). When one
is built, its row moves to the table above. Rows marked *open* have no agreed direction yet: design each one with the
owner (references, a real prototype in the Showroom) when its turn comes. Inspiration per component and the owner's decisions: `docs/research/0003-component-inspiration.md`. Selection
(radio/checkbox) was dropped: reuse toggles, the segmented control and checkmark rows.

| # | Name | Archetype | Priority | What it is | Used on |
|---|---|---|---|---|---|
| 2 | HaloSegmentedControl | Input | P1 | Sliding pill selector (sats/BTC/fiat, on-chain/Lightning, fee speed) | Send, Receive, Settings |
| 3 | HaloAmountKeypad | Input | P1 | Custom number pad: decimal, hold-to-repeat delete, tick per key | Send, Receive |
| 4 | HaloTextField | Input | P1 | Address / invoice / search input: paste, clear, live validation, error shake | Send, Settings |
| 5 | HaloTransactionRow | Value display | P1 | Direction, title, time, network-coloured amount, pending pulse, failed state | Home, Activity |
| 6 | HaloBalanceCard | Value display | P1 | *open* · Electric glass card: deep-navy glass slab, Fresnel blue rim, a glowing halo inside, balance in mono; lifts and ignites on press | Home |
| 7 | HaloListRow | Pressable | P1 | Settings row: icon, title, subtitle, trailing toggle / chevron / value | Settings, Detail |
| 8 | HaloChip | Value display | P1 | Network badge, status pill, filter chips | Home, Activity, Detail |
| 9 | HaloTabBar | Container / Overlay | P1 | Glass bottom navigation, sliding indicator | App shell |
| 10 | HaloTopBar | Container / Overlay | P1 | Large title collapsing to glass on scroll | Every screen |
| 11 | HaloToast | Feedback | P1 | Pill notice: copied, error, payment arrived | Everywhere |
| 12 | HaloQrCode | Value display | P1 | Hand-made QR (own encoder, no libraries) with the halo mark, animated reveal | Receive |
| 13 | HaloAddress | Value display | P1 | Address in Paper Mono groups of 4, copy feedback | Receive, Detail |
| 14 | HaloFeeSelector | Input | P2 | *open* · Three choices showing arrival time and cost (sats and fiat); Custom opens a notched slider | Send |
| 15 | HaloChart | Gesture-commit | P2 | *open* · Glass tube pillars filled with glowing liquid; scrub lights a tube, ticks, liquid ripples | Home |
| 16 | HaloConfirmations | Value display | P2 | *open* · "Settling · 2 of 3 · ~10 min" with three small glass tubes that fill per block; on-chain only | Detail, Activity |
| 17 | HaloPaymentSuccess | Feedback | P2 | *open* · Unique to Halo (direction being chosen): the halo seals / per-network arc vs pour / glass shatters into light | Send, Receive |
| 18 | HaloSkeleton | Feedback | P2 | Loading placeholders with shimmer | Home, Activity |
| 19 | HaloDialog | Container / Overlay | P2 | *open* · The familiar alert layout rebuilt in Halo materials (lit slab, frosted scrim, spring in, one haptic) | Settings, Send |
| 20 | HaloContextMenu | Container / Overlay | P2 | Long-press menu (copy, explorer) | Activity, Detail |
| 21 | HaloScannerFrame | Container / Overlay | P2 | *open* · Frosted-glass bezel with lit rim; auto-zoom on the code, brackets snap, rim flashes, morphs into the pay sheet (after CRED) | Send |
| 22 | HaloAvatar | Value display | P2 | Flat monogram: initials in mono on a flat tinted circle (decided) | Send |
| 23 | HaloPinPad | Input | P3 | PIN entry, passcode dots, shake on wrong, biometric key | Lock, Onboarding |
| 24 | HaloSeedPhrase | Value display | P3 | Recovery words, blur-to-reveal, verify step | Backup |
| 25 | HaloSwipeToConfirm | Gesture-commit | P3 | Slide to send | Send |
| 26 | HaloPullToRefresh | Gesture-commit | P3 | Custom refresh (halo liquid filling) | Home, Activity |

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
| `HaloOverlay` (internal) | The window's overlay layer in `HaloApp`: sheets, dialogs, toasts render above content; the content recedes |
