# Halo: Bitcoin wallet showcase (native Android, Compose)

This is a frontend-showcase Bitcoin wallet. It demonstrates AI-built, hand-made UI components with 2027-grade
motion and micro-UX. It has no real backend, but the architecture is production-grade: swapping the fakes in
`:core:data` for real implementations is all a backend needs.

## Build & run (Windows, macOS, Linux)
All tooling goes through one stdlib-only Python CLI, `.claude/skills/halo-component/scripts/halo.py`
(below, `halo.py`; run it as `python3 -I <path>` on macOS/Linux and `python -I <path>` on Windows). It finds the
JDK (`$JAVA_HOME`, else Android Studio's bundled JBR) and the SDK (`$ANDROID_HOME`, `local.properties`, else the OS
default location) by itself. Never put machine-specific paths in `gradle.properties`.
- Check the discovered toolchain: `halo.py doctor`
- Build: `halo.py gradle :app:assembleDebug` (any Gradle args are passed through to `gradlew` / `gradlew.bat`)
- Unit tests: `halo.py gradle :core:domain:test`
- Design-system lint: `python3 -I .claude/skills/halo-component/scripts/lint_ds.py`
- Build + install + open a Showroom page + screenshot: `halo.py shot <id>` → `shots/<id>.png`
  - Pages are `halo://showroom/<id>`. Debug params: `?slowmo=5&grid=8&rm=1` (`--slowmo 5 --grid 8 --reduced`).
  - Find an element's coordinates with `halo.py find "<label>"`.
  - Pixel review: `halo.py zoom shots/<id>.png <x> <y> --w 300 --h 160 --scale 4`.
- Emulator: AVD `Pixel_10_Pro` (1280×2856, 480 dpi = 3.0 px/dp); override with `--avd` or `$HALO_AVD`. It can't
  vibrate; check haptics in the on-screen haptic log.

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
- The key light is world-fixed: it turns with device tilt (`LightRig`). Read `HaloTheme.light` direction and
  `HaloTheme.hdr` only in draw/shader code. Only emitted light (glows, liquid, sparks) is boosted on HDR screens.
- Motion follows Apple's fluid-interface guidance and real physics (springs, momentum, viscosity, inertia). Every user
  action plays one `HaloHaptic` token (haptic + synthesised sound, rendered per actuator tier; only `feedback/` touches
  vibration/audio APIs). Continuous animation runs only while visible, foregrounded, outside battery saver and
  reduced motion. Physics and feedback models are unit-tested (`halo.py gradle :core:designsystem:testDebugUnitTest`).
- Apps enter through `HaloApp(brand, appearance) { }` (proposal 0004): brand = palettes + fonts (`haloBrand(accent)`
  derives roles in OKLCH with AA contrast); the appearance store belongs to the host. Never hard-code Halo's lime
  where a role (`colors.accent`) is meant: other apps re-brand it. Plug-in guide: `docs/INTEGRATION.md`.
- minSdk 26: shaders only when `HaloPlatform.shaders` (Android 13+), blur when `HaloPlatform.blur` (12+), each with a
  designed canvas fallback; newer platform calls are gated by `SDK_INT` (Android Lint `NewApi` stays clean). Check
  with `halo.py shot <id> --legacy`.
- Components reach the system only through `HaloTheme.runtime` (proposal 0003): continuous motion via
  `rememberAmbient`, sensors via `runtime.sensors`, power/thermal/lifecycle via `runtime.policy`. Never register sensors,
  read lifecycle/power/thermal state or write a frame loop in a component. Live view: Showroom → Foundations → Runtime.

## Docs
- Tokens and design rules: `.claude/skills/halo-component/references/` (tokens, motion recipes, haptics,
  archetypes, pixel-perfect, native Android, anti-patterns, architecture).
- Components: `docs/components/REGISTRY.md` and one spec per component.
- Proposals: `docs/proposals/`. In progress: 0004 (portable design system, step 3 next: library packaging). Then 0001.
- Adding Halo to another app: `docs/INTEGRATION.md`.
