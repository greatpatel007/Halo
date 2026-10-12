# Halo tokens (v0.1)

Code is the source of truth: `core/designsystem/src/main/kotlin/com/halo/core/designsystem/theme/`. Keep this file in sync
whenever a token is added. Access everything through `HaloTheme.*` inside composables.

## Color: semantic roles (`HaloTheme.colors`)
Dark-first. Components reference **roles only**, never `HaloPalette`.

| Role | Value | Use |
|---|---|---|
| `bg.canvas` | ink950 `#0A0A0B` | L0 app background |
| `bg.surface` | ink850 `#141416` | L1 cards, rows, inputs |
| `bg.raised` | ink800 `#1A1A1D` | L2 sheets, popovers, selected |
| `bg.glass` | ink700 @72% | L3 tint over backdrop blur |
| `bg.scrim` | black @60% | behind modals |
| `content.primary` | ink100 `#F4F4F2` | text/icons (18:1) |
| `content.secondary` | ink300 `#9C9CA3` | supporting text (7.3:1) |
| `content.tertiary` | ink400 `#6E6E76` | non-essential only (3.9:1) |
| `content.disabled` | ink500 | disabled labels |
| `content.inverse` | ink950 | text on light fills |
| `line.subtle / default / strong` | white 6 / 10 / 16% | L1 / L2 / L3 edges, dividers |
| `line.focus` | blue400 (dark) / blue600 (light) | keyboard focus ring |
| `line.highlight` | white 8% | top-edge light catch (L2+) |
| `accent.fill / fillPressed / onFill` | blue500 `#2A67FF` / blue600 / white | primary actions (brand blue; white label 4.7:1) |
| `accent.text` | blue400 `#6E9BFF` (dark, 7.3:1) / blue600 (light, 5.3:1) | blue text/icons |
| `accent.tint` | blue900 `#0D1A3C` brand navy / blue050 | selected chip/row background |
| `accent.glow` | blue500 @30% (dark) / @28% (light) | glow behind active accent |
| `bitcoin.*` | same shape on the calmed btc ramp (`#ED9449`, OKLCH C 0.14) | **on-chain Bitcoin only** |
| `lightning.*` | same shape on the softened yellow ramp (`#E6D06A`, C 0.13; ink label; text yellow800 on light). Exact brand yellow `#F5DE4B` = `HaloPalette.YellowBrand`, brand moments only | **Lightning only** |
| `status.positive / negative / warning` (+ `Tint`) | `#6ECF9A` / `#ED7473` / coral `#F2AA8C` (dark; deeper stops on light) | incoming, errors, cautions (warning is coral, never yellow) |

**Colour hierarchy (OKLCH chroma):** brand blue `#2A67FF` is the only loud colour (C 0.24). Network families sit at ~0.14,
status colours at 0.10–0.15, and the ink neutrals carry a faint blue cast (hue 264, C ≤ 0.012). A new colour must sit in
this hierarchy: nothing may out-shout the brand blue.

`LocalHaloContentColor` carries the container's content color down the tree. `HaloText` and `HaloIcon` default to it,
so text in a blue button automatically becomes `accent.onFill`.

## Typography (`HaloTheme.typography`)
Every style trims its line box (`LineHeightStyle.Trim.Both`), so a text node is exactly its line height.

| Token | Font | Size/LH | Tracking | Use |
|---|---|---|---|---|
| `amountHero` | Paper Mono Light | 48/52 | −3% | balance |
| `amountLarge` | Paper Mono Regular | 32/38 | −2% | amount entry |
| `amountMedium` | Paper Mono Regular | 20/24 | −1% | card amounts |
| `amountSmall` | Paper Mono Medium | 15/20 | 0 | list rows |
| `headline` | Satoshi Bold | 26/32 | −2% | screen titles |
| `title` | Satoshi Bold | 20/26 | −1% | sheet titles |
| `subtitle` | Satoshi Medium | 17/22 | −0.5% | row titles |
| `body` | Satoshi Regular | 15/22 | 0 | paragraphs |
| `bodySmall` | Satoshi Regular | 13/18 | 0 | helper text |
| `label` | Satoshi Bold | 15/20 | 0 | buttons |
| `overline` | Paper Mono Medium | 11/14 | +8% | section labels, UPPERCASE |
| `data` | Paper Mono Regular | 13/18 | 0 | addresses, txids, times |
| `micro` | Paper Mono SemiBold | 10/12 | +6% | badges, chips |

Rules:
- A numeral that is a value never uses Satoshi.
- Sats are grouped with narrowed spaces (`AmountFormat.satsAnnotated`).
- Insignificant trailing BTC zeros are dimmed (`AmountFormat.btcAnnotated`).
- Neither font has a `₿` glyph or a thin space. Draw `₿` and `⚡` as vectors (`HaloIcons.Bitcoin`, `HaloIcons.Bolt`), never as text.

## Spacing, sizes, radius, stroke, effects
- **`spacing`** (4dp grid):
  - `none 0`, `xxs 2`, `xs 4`, `sm 8`, `md 12`, `lg 16`
  - `xl 20`, `xxl 24`, `x3 32`, `x4 40`, `x5 56`, `x6 72`
  - `gutter 20`
- **`sizes`:**
  - controls: `controlSm 36`, `controlMd 48`, `controlLg 56`, `controlXl 64`
  - `minTouch 48`
  - icons: `iconSm 16`, `iconMd 20`, `iconLg 24`, `iconXl 32`
  - switch housings: `switchMd 52×30`, `switchLg 76×42` (round thumb; travel ≈ 0.8 × its diameter, like a real switch)
  - glass halo mark: `logoMd 32`, `logoLg 56`
  - sheet grip: `sheetHandle 36×4`
- **`radius`:**
  - `xs 8`, `sm 12`, `md 16`, `lg 20`, `xl 28`, `sheet 32`
  - `full` (a 50% shape)
  - `nested(outer, padding)` returns `outer − padding`, for concentric corners
- **`stroke`:** `hairline` (1px), `thin 1`, `focus 2` (`focusOffset 2`), `icon 1.75`, `indicator 2`.
- **`effects`:**
  - `glassBlur 24`, `highlightFalloff 40`
  - `glowRadius 20`, `glowSpread 2`
  - `grain 0.05`, `disabledAlpha 0.38`

## Depth (`HaloDepth` + `Modifier.depth` / `HaloSurface`)
| Level | Recipe |
|---|---|
| `Canvas` (L0) | canvas, no edge |
| `Surface` (L1) | surface + `line.subtle` |
| `Raised` (L2) | raised + `line.default` + top light |
| `Glass` (L3) | backdrop blur (`effects.glassBlur`) + `bg.glass` + `line.strong` + top light |

Light effects:
- **Glow:** `Modifier.haloGlow(color, shape, radius, spread, intensity = { … })`, with `accent.glow` or `bitcoin.glow`.
- **Grain:** `Modifier.haloGrain(effects.grain)` on hero surfaces only.

## Materials (`HaloTheme.materials`)
Components pick a material role, never their own albedo, gloss or grain.
- `body`, `knob`: `ReliefMaterial` for housings and movable parts (soft-touch/graphite on dark, ceramic on light).
- `recessFloor`, `velvetGrain`: channels and velvet fills.
- `glass`: `GlassMaterial` (tint, absorption, Fresnel reflectance, environment, specular, shininess). Smoky graphite
  glass on dark, cool clear glass on light. Used by the halo mark.

## Motion (`HaloTheme.motion`)
| `HaloSpring` | ζ / k | Use |
|---|---|---|
| `Snap` | 1.0 / 2000 | press-in, instant feedback |
| `Quick` | 0.8 / 1000 | release, small icons, checks |
| `Standard` | 0.9 / 500 | size / position / layout |
| `Expressive` | 0.65 / 400 | selection pills, morphs, success pop, shake |
| `Gentle` | 1.0 / 220 | screens, sheets, shared elements, enter |
| `Digit` | 0.75 / 700 | odometer digit roll |
| `Mechanical` | 1.0 / 400 | switch thumbs, sliders with mass (~330ms, never bounces) |
| `Tide` | 1.0 / 7 | the theme transition's water front (launched with a kick) |
| `Throw` | 0.82 / 440 | a surface thrown and carried by momentum (sheets after a fling); a placed surface uses `Gentle` |

- **Calls:**
  - `motion.spring<T>(token, visibilityThreshold?)` for anything spatial.
  - `motion.fade(HaloFade.Fast|Base|Slow = 120|200|320ms, easing)` for color and alpha only.
  - `motion.timed(ms)` for user-waited progress such as a hold. It is never removed by reduced motion.
- **Easing (`HaloEasing`):**
  - `EmphasizedDecelerate` for entering
  - `EmphasizedAccelerate` for exiting
  - `Standard` for on-screen changes
- **Choreography:**
  - Enter: rise `motion.enterRise` (12dp) + fade on `Gentle`, staggered by `staggerDelayMillis(i)` (30ms, capped at 6). Use `Modifier.enterRise(i)`.
  - Exit is about 60% of enter (`HaloMotion.EXIT_RATIO`).
- **Slow-mo and reduced motion:** `motion.scale` (Showroom slow-mo) is already folded into every spec. `motion.reduced` makes springs snap. Check it for anything decorative, such as particles or glow pulses.

## Haptics (`HaloTheme.haptics`)
| Token | Use |
|---|---|
| `Tap` | buttons |
| `Tick` | keypad digits, slider steps, digit roll |
| `ToggleOn` / `ToggleOff` | switches |
| `Threshold` | a gesture crosses its commit point |
| `Confirm` | success |
| `Reject` | invalid (always paired with a shake) |
| `Crescendo` | start of a hold-to-commit charge |
| `Celebrate` | payment sent |
| `Ping` | the glass halo struck lightly |
| `Land` | a tray or sheet comes to rest (soft round knock) |

Call `haptics.perform(token)`. Use `haptics.tick(intensity 0..1)` for charging progress and `haptics.cancel()` to abort a composition.


## Light & HDR (`HaloTheme.light`, `HaloTheme.hdr`)
- `light`: the one key light (top-left), **world-fixed**: its direction turns with device tilt via `LightRig`
  (gravity, ≤ 30° swing, smoothed, re-centres on the holding angle over ~4 s). Read `x/y/z/shadowDx/shadowDy` in draw only.
- `hdr.headroom` / `hdr.emission`: how much brighter than SDR white the display can go (1 on SDR; emission capped at
  2.5×). Multiply emitted light only, in draw/shader code.
