# Proposal 0004: Portable design system, plug Halo into any app

**Status:** Paused (2026-10-12): steps 1–3 built; step 4 and the release work wait until the components and demo screens are designed · **Raised by:** product direction: "we can easily plug this into
any app without much hassle."

## Problem
Inside, the design system is portable: one module, no Material or third-party UI, and every component reaches the
system through `HaloTheme.runtime`. A host app still hits these walls:

1. **Halo's brand is hard-wired.** Lime / orange / cyan and Satoshi / Paper Mono can't be replaced.
2. **The wiring is manual.** Theme, appearance store, Tide transition, edge-to-edge, system-bar icons and the
   platform's per-app night mode are put together by hand in `MainActivity` and the Showroom's debug host.
3. **Android 13+ only.** The AGSL shaders (glass, grain, relief, logo) need API 33; only the Tide falls back.
4. **Not a library.** It builds only inside this repo (convention plugins, compileSdk 37) and has no version or
   public-API check.
5. **No integration guide or sample.** Haptic switches and strength were also lost on restart.

## Plan
| Step | What | Status |
|---|---|---|
| 1 | **Brand + one-call host.** `HaloBrand` / `haloBrand(accent, …)` (OKLCH-derived roles, AA contrast checked, hue kept), `HaloFonts`; `HaloApp { }` (theme, appearance, Tide, edge-to-edge, bar icons, platform night mode); `HaloAppearance` (built-in store or adapt your own); `HaloThemeSwitch`; haptic and sound settings persist. Showroom → Foundations → Brand. Guide: `docs/INTEGRATION.md` | built |
| 2 | **Android 8+ (API 26).** `HaloPlatform` decides shaders (13+) and blur (12+). Canvas fallbacks keep the look: `CanvasRelief` (gradient bevels, recess walls, stacked contact shadows), bitmap grain, `drawCanvasHalo` (ring, spill, circulating liquid, glints), frosted glass, stepped glow below Android 9, crossfade for the Tide. Haptics: primitives 12+, legacy audio attributes, CONFIRM/REJECT fallbacks; thermal 10+. Debug `?legacy=1` / `shot --legacy` | built |
| 3 | **Library packaging.** `com.halo:designsystem:<HALO_VERSION>` via `maven-publish` (AAR + sources + Gradle module metadata, Compose BOM imported in the POM); `halo.py publish [--local]` → `build/repo` + zip. Public API guarded by `api/designsystem.api` (`halo.api` plugin: `apiDump`, `apiCheck` in `check`). Resources prefixed `halo_`. No consumer R8 rules needed | built |
| 4 | **Release (after the design work).** Switch the API check on (`HALO_API_CHECK=true`, first `apiDump`), pick hosting and the group ID, settle the font licences. **Sample and guide.** A minimal `:sample` app (one screen, own brand, no Hilt) built in CI as the plug-in proof; the guide finished with the published coordinates | |

## Step 1 decisions
- **What a brand is:** palettes for both themes + two type families. Spacing, motion, light, materials, haptics and
  sound are the system's and stay the same in every app (they are the product).
- **One seed per accent family.** The seed is the dark theme's fill; pressed, text, tint and glow and the light
  theme's deeper stops are derived in OKLCH. Accent text is pushed along lightness until it reaches 4.6:1 on the
  canvas; the label on a fill is whichever ink contrasts more. `BrandTest` checks ten seeds across the hue circle.
  Hand-tuned palettes are still possible: build `HaloColors` yourself and pass `HaloBrand(dark, light, fonts)`.
- **Names stay.** The two signal families keep their wallet names (`bitcoin`, `lightning`); a host uses them as its
  own two signal colours. Renaming would churn every component for no visual change.
- **The appearance store belongs to the host** (`HaloAppearance`): the built-in one uses private preferences; apps
  with a settings repository adapt it. A choice shows on the next frame (response first), before the store confirms.
- **Platform night mode only when safe.** `HaloApp` mirrors changes into `UiModeManager.setApplicationNightMode`
  (so the splash and system UI match) only if the activity declares `configChanges="uiMode"`; otherwise a switch
  would restart the activity mid-wave, so it logs a warning instead.

## Step 3 decisions and results (2026-10-12)
- **Paused by the owner:** the library won't be consumed until the design work is done, so the API check is
  dormant (`HALO_API_CHECK=false`, no reference dump) to keep component work free of friction. Publishing stays
  available (`halo.py publish`).
- **API guard:** Kotlin 2.4's built-in ABI validation doesn't see AGP 9's built-in Kotlin compilation (it dumps
  nothing), so a small build-logic plugin reads the release classes through AGP's artifact API (ASM +
  kotlin-metadata-jvm). It drops Kotlin `internal`, private, synthetic and compiler-generated members. The first dump
  has 128 public classes. Checked: adding a public function fails `apiCheck`, and reverting passes.
- **compileSdk 37 stays:** every Compose 1.12 artifact declares `minCompileSdk=37`, so host apps need it whatever
  Halo declares. Lowering it would mean an older Compose.
- **Coordinates:** `com.halo` is a placeholder group. Maven Central needs a namespace you own (e.g.
  `io.github.greatpatel007`); `HALO_GROUP` changes it in one place.
- **Open (owner's call):** where to host it (GitHub Packages, JitPack from tags, or Maven Central). Also, the fonts'
  licences: Paper Mono is OFL (its licence text must ship with it), and Satoshi's ITF licence must allow
  redistribution inside a library. Check both before publishing publicly.

## Step 2 results (emulator, 2026-10-12)
- minSdk 33 → 26 for every module. Android Lint: no `NewApi` errors (it found ~120 call sites; all gated or given a
  fallback). Three older lint errors fixed on the way (primitive constants, LocalActivity, EnterRise read).
- Legacy render (`--legacy`) checked in dark and light: toggles keep their ceramic/graphite relief and contact
  shadows, the hero keeps its grain, the dock frosts, the logo keeps its glowing liquid. The theme switch crossfades.
  No crashes.
- Not verified: a real Android 8–12 device or emulator image (none installed). The legacy simulation exercises the
  same fallback code paths, but OEM drivers and older platform behaviour (e.g. vibration on Android 8) are untested.

## Step 1 results (emulator, 2026-10-12)
- Brand page: ocean / coral / violet / mint / sun seeds re-brand button, toggle and text in both themes.
- The wallet now runs on `HaloApp` through the adapter (`rememberHaloAppearance(mode, onModeChange)`); the theme
  switch, Tide, bar icons and deep-link `?theme=` work as before, and the choice survives a restart.
- 41 design-system unit tests pass (4 new in BrandTest); lint clean.
