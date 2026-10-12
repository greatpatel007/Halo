# Proposal 0006: Migrate the design system to Material 3 Expressive foundations

**Status:** Accepted (2026-10-13; the four decisions below taken as recommended) · phase 1 in progress · **Depends on:** [0005 clean glass](0005-clean-glass.md) (its 2026-10-13 revision and
Material section) · **Proof:** Showroom → Surface lab (`MaterialLab.kt`, `NativeLab.kt`, `SurfacePage.kt`)
**Rule (owner, 2026-10-13):** Android's guides first, Apple fills the gaps, **motion follows Apple only**.

## Goal
Move `:core:designsystem` from Halo's own token vocabulary to Material 3 Expressive's: colour roles, type roles,
surface/elevation model, shape scale and navigation, while keeping what makes Halo Halo (brand blue as the one loud
colour, network colours with meaning, Paper Mono numbers, the scene light, glass for the floating layer, Apple motion,
designed haptics). Every component and Showroom page ends up on the new tokens, in dark and light, on Android 8+.

## Non-goals
- Material **motion** (stays Apple's fluid interfaces and our spring tokens).
- Material components or the `material3` library: everything stays hand-built on Compose foundations.
- Dynamic colour from the wallpaper (see decision 1).
- Resuming proposal 0004 (library release): still paused; nothing outside this repo consumes the API yet, so this is
  the cheapest moment for a breaking change.

## Where we are (inventory, 2026-10-13)
| Area | Today | Used by |
|---|---|---|
| Colours | `HaloColors`: `bg` (canvas, surface, raised, glass, scrim), `content` (primary…inverse), `line` (subtle…highlight), `accent`/`bitcoin`/`lightning` (fill, fillPressed, onFill, text, tint, glow), `status` | 12 roles in DS components; ~38 files across DS, Showroom, app |
| Brand | `haloBrand(accent)` derives the accent families in OKLCH with AA checks | `HaloApp`, Brand page |
| Type | 13 Halo styles (`headline`, `title`, `subtitle`, `body`, `bodySmall`, `label`, `overline`, `amountHero/Large/Medium/Small`, `data`, `micro`) | 10 styles in DS components; every page |
| Depth | `HaloDepth` L0–L3 + `Modifier.depth` (tone + hairline + top light; L3 blur) | `HaloSurface`, Showroom chrome, ~10 pages |
| Shape | `radius` xs 8 · sm 12 · md 16 · lg 20 · xl 28 · sheet 32 · full | all components |
| Navigation | none in the DS (planned #9 tab bar, #10 top bar); Showroom has its own glass dock | — |
| Components | Button, RollingAmount, HoldToSend, Toggle (+ ThemeSwitch), SegmentedControl, Sheet, Logo | Showroom, app |

## Target
### 1. Colour: `HaloScheme` (Material roles + Halo extensions)
- **Material roles, exact tones** (androidx `ColorLight/DarkTokens`): `primary/onPrimary/primaryContainer/onPrimaryContainer`,
  the same for `secondary`, `tertiary`, `error`; `surface`, `surfaceDim`, `surfaceBright`,
  `surfaceContainerLowest/Low/–/High/Highest`, `onSurface`, `onSurfaceVariant`, `outline`, `outlineVariant`,
  `inverseSurface`, `inverseOnSurface`, `inversePrimary`, `scrim`.
- **Generator:** a tone engine in `theme/` (tone = CIELAB L*, as in HCT; hue/chroma from the seed; gamut-mapped), replacing
  `haloBrand`'s accent derivation. `haloBrand(seed)` keeps its signature and now returns the whole scheme.
- **Halo extensions:**
  - `primaryContainer` = the brand seed exactly (fidelity): the key action stays Halo blue in both themes.
  - Extended families `bitcoin`, `lightning`, `positive` (Material's custom-colour shape: colour, on, container,
    on-container) + `fill/onFill` = the true seed for lit controls (tone 40 turns yellow olive).
  - Scene-light colours (`HaloLight`) and glass tints stay where they are, now derived from scheme roles.
- **Contrast levels:** standard / medium / high schemes (Material's contrast variants), chosen from the user's
  Android 14+ contrast setting via `HaloTheme.runtime` (`UiModeManager.getContrast()` + listener).

### 2. Type: `HaloTypography` = Material's 15 roles + emphasized + Halo numeric roles
- `displayLarge…labelSmall` (androidx `TypeScaleTokens`), each with an `…Emphasized` variant, set in Satoshi.
- Numbers keep Paper Mono through Halo roles: `amountDisplay` (was amountHero), `amountHeadline` (amountLarge),
  `amountTitle` (amountMedium), `amountBody` (amountSmall), `data` (mono body small).
- Sentence case everywhere; `overline`/`micro` all-caps labels retire (badges use Label Small).
- Mapping for migration: headline → headlineSmall, title → titleLarge, subtitle → titleMedium, body → bodyLarge,
  bodySmall → bodyMedium, label → labelLarge.

### 3. Surfaces and elevation
- `HaloSurface(level)` becomes **tonal**: `Surface` (base) and `ContainerLowest…Highest`; no hairlines, no top light.
  `HaloDepth` is removed (L1 → container, L2 → containerHigh, L3 → glass).
- **Glass is the floating layer only** (0005 revision): `Modifier.floatingGlass` (blur + translucent tint + lit rim +
  short shadow; edge lensing on 13+; blur-only on 12; frosted below; also when the system disables blur, via
  `addCrossWindowBlurEnabledListener` in the runtime policy).
- Shadows only where the scene light needs them (floating glass, touched controls): D3 stands, but flat content casts none.

### 4. Shape
- Adopt Material's corner scale: `none 0 · extraSmall 4 · small 8 · medium 12 · large 16 · largeIncreased 20 ·
  extraLarge 28 · extraLargeIncreased 32 · extraExtraLarge 48 · full`. Halo's radii map 1:1 (xs→small, sm→medium,
  md→large, lg→largeIncreased, xl→extraLarge, sheet→extraLargeIncreased); `nested()` stays for concentric corners.

### 5. Navigation (new components)
- `HaloNavigationBar` (replaces planned #9): Material 3 Expressive flexible bar, 64 dp, 24 dp icon over Label Medium,
  56 × 32 pill indicator (secondaryContainer), active label secondary, inactive onSurfaceVariant; translucent
  surfaceContainer over blur when content scrolls beneath; Tick per change; predictive back aware.
- `HaloTopAppBar` (replaces planned #10): Material small / medium-flexible / large-flexible top app bar; collapses on
  scroll; its scrolled state is the floating glass.
- Later: navigation rail for wide screens (Material's adaptive guidance), floating toolbar for contextual actions.

### 6. Components (each migrated through the `halo-component` skill, re-verified dark / light / legacy)
| Component | Becomes |
|---|---|
| `HaloButton` | Material button styles: **Filled** (key action: primaryContainer = brand blue), **Tonal** (secondaryContainer), **Outlined**, **Text**; network variants keep their fill. Expressive shape morph on press (Apple spring). Sizes XS–XL per Material Expressive |
| `HaloToggle` / `HaloThemeSwitch` | Material switch geometry and roles (on = family fill, off = containerHighest + outline); handle stays the frosted glass disc (owner); touch lifts it into glass |
| `HaloSegmentedControl` | Material connected button group / segmented: selected = secondary (or meaning) container, Label Large; drag, detents, haptics unchanged |
| `HaloSheet` | Material bottom sheet roles: surfaceContainerLow, drag handle (onSurfaceVariant), scrim; physics unchanged |
| `HaloHoldToSend` | Track = containerHighest, charge = family fill; label Label Large |
| `HaloRollingAmount` | Colours from onSurface; styles → amount roles. Logic unchanged |
| `HaloLogo` | Unchanged (glass + emitted light); verify on the new surfaces |
| Foundations | `HaloPressable`/indication: Material state-layer opacity for press/focus (Halo's touch-origin light kept); `HaloText` defaults to bodyLarge/onSurface; `HaloIcon` tints from onSurfaceVariant |

## Phases
Each phase ends green: lint, unit tests, build, Showroom screenshots in dark/light/legacy, no visual regressions outside
the phase.

| # | Phase | Work | Verification |
|---|---|---|---|
| 1 | **Tokens** | `HaloScheme` + tone engine + contrast levels; `HaloTypography` roles; shape scale. **Old names kept as deprecated aliases** mapped onto the new roles so everything still builds | Unit tests: tones match Material's table (±0.5 L*), every on-pair ≥ 4.5:1 (≥ 3:1 for large), fidelity keeps the seed, extended fills keep their seed. Colour + Type pages rebuilt to show the new roles |
| 2 | **Foundations** | Tonal `HaloSurface`, `floatingGlass` (lab code lifted + blur-off fallback), touch-glass for controls, state layers, runtime: contrast + cross-window-blur listeners | Surface lab rebuilt on DS code (lab files deleted); legacy screenshots; frame counts for the glass on a mid-range profile |
| 3 | **Components** | Button → Toggle/ThemeSwitch → SegmentedControl → Sheet → HoldToSend → RollingAmount → Logo check; specs and registry updated | Per component: skill quality gate, screenshots, haptic log, motion recording |
| 4 | **Navigation** | `HaloNavigationBar`, `HaloTopAppBar` (registry rows, specs, Showroom pages); the Showroom adopts them (its dock becomes floating glass) | Screenshots incl. content scrolling beneath, predictive back, 200 % font |
| 5 | **App and Showroom chrome** | ShowroomChrome, home, debug host, every page on the new roles and type; `MainActivity` system bars from the scheme | Full Showroom sweep dark/light |
| 6 | **Rules and docs** | `tokens.md`, skill references (archetypes, pixel-perfect, anti-patterns, materials), CLAUDE.md rules (colour, type, surfaces, motion precedence), `lint_ds.py` bans the old names, `docs/INTEGRATION.md` (brand seed → scheme) | Lint clean with the bans on |
| 7 | **Cleanup** | Remove the deprecated aliases, `HaloDepth`, the lab-only files; 0001 surface half and 0005's superseded sections archived | Build + full screenshot sweep |

Rough size: phase 1 and 2 are the heavy ones (about a session each); phase 3 is one component per step; 4–7 are smaller.

## Risks
- **Brand drift:** Material's dark `primary` is a pastel (tone 80). Mitigated by fidelity: text/icon roles follow
  Material, the key action keeps the exact brand blue as primaryContainer.
- **Meaning colours:** strict tones dull orange and yellow; network `fill` keeps the seed (proven in the lab).
- **Contrast on glass:** floating glass keeps a tint floor; high-contrast level makes it nearly opaque with an outline.
- **Big-bang regressions:** aliases in phase 1 keep every screen building; components move one at a time.
- **Performance:** only the floating layer blurs; lensing is 13+ only and cached per size.

## Decisions (accepted 2026-10-13, as recommended)
1. **Dynamic colour (Material You):** recommend **off**. Halo is a brand-led wallet; meaning colours must stay fixed.
   (Host apps can still pass their own seed through `haloBrand`.)
2. **Alias period:** recommend aliases for phases 1–6, removed in 7 (safer), rather than renaming everything at once.
3. **Shape scale:** adopt Material's names (recommended) or keep Halo's `xs…xl` names mapped onto the same values.
4. **Button set:** adopt Material's Filled / Tonal / Outlined / Text (+ network fills), retiring `Secondary`/`Ghost`
   names (recommended), or keep Halo's names as aliases.
