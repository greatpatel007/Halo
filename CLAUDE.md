# Halo: Bitcoin wallet showcase (native Android, Compose)

This is a frontend-showcase Bitcoin wallet. It demonstrates AI-built, hand-made UI components with 2027-grade
motion and micro-UX. It has no real backend, but the architecture is production-grade: swapping the fakes in
`:core:data` for real implementations is all a backend needs.

## Build & run (Windows)
- Gradle needs Studio's JBR. Set `JAVA_HOME` in the command, never in `gradle.properties`:
  `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:assembleDebug`
- Unit tests: `.\gradlew.bat :core:domain:test`
- Design-system lint: `python -I .claude/skills/halo-component/scripts/lint_ds.py`
- Build + install + open a Showroom page + screenshot:
  `powershell -ExecutionPolicy Bypass -File .claude/skills/halo-component/scripts/shot.ps1 -Page <id>`
  - Pages are `halo://showroom/<id>`. Debug params: `?slowmo=5&grid=8&rm=1`.
  - Find an element's coordinates with `scripts/find.ps1 -Desc "<label>"`.
- Emulator: AVD `Pixel_10_Pro` (1280×2856, 480 dpi = 3.0 px/dp). It can't vibrate; check haptics in the on-screen haptic log.

## Modules
`build-logic/convention` holds the convention plugins (`halo.android.application|library|compose|feature`, `halo.hilt`, `halo.jvm.library`).

| Module | Role |
|---|---|
| `:core:model` | Pure Kotlin types (`Sats`, `Transaction`, `WalletBalance`, `PaymentDestination`, `WalletError`) |
| `:core:domain` | Repository interfaces + use cases (pure Kotlin, unit-tested) |
| `:core:data` | Repository implementations (`Fake*` today) + Hilt `DataModule` bindings |
| `:core:designsystem` | Pure UI: tokens (`theme/`), primitives (`foundation/`), `icons/`, `component/` |
| `:feature:showroom` | The design-system catalog (and pattern reference for features) |
| `:app` | Hilt root, `MainActivity`, `HaloNavHost` |

## Rules
- **Components are built with the `halo-component` skill** (`/halo-component <Name> <requirements>`). It owns the
  spec → code → Showroom → registry → screenshot workflow and the quality gate. Don't hand-roll components outside it.
- The design system has no Material/Material3, no ripple and no third-party UI or animation libraries. Only tokens:
  the lint enforces no raw colours, dp/sp, `spring(`/`tween(` or platform haptics.
- `:core:designsystem` never depends on `:core:model`/`:core:domain`. Components take primitives (`Long` sats).
- Features follow UDF: `@HiltViewModel` exposes one `StateFlow<UiState>` and `onAction(Action)`; a `*Route`
  composable wires it to a stateless `*Screen`. Navigation uses type-safe `@Serializable` routes.
- Money is `Sats` (Long) / `BigDecimal` in the domain. Never use `Double` for amounts outside display formatting.
- Colour carries meaning:
  - **Lime** = UI actions
  - **orange** = on-chain Bitcoin
  - **electric cyan** = Lightning

  Numbers are always Paper Mono.
- **Dark and light themes ship together.** Every component and page is verified in both (decision D1).
- Shadows only as consequences of the scene light (contact shadow / AO via the material system). Never
  decorative drop shadows. One key light, top-left (decisions D3/D4).

## Docs
- Tokens and design rules: `.claude/skills/halo-component/references/` (tokens, motion recipes, haptics,
  archetypes, pixel-perfect, native Android, anti-patterns, architecture).
- Components: `docs/components/REGISTRY.md` and one spec per component.
- Proposals: `docs/proposals/`. Next up: 0001, the layered material surface stack with scene lighting.
