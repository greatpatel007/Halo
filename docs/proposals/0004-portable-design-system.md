# Proposal 0004: Portable design system, plug Halo into any app

**Status:** In progress (2026-10-12): steps 1–2 built · **Raised by:** product direction: "we can easily plug this into
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
| 3 | **Library packaging.** Standalone Gradle setup (no convention plugins needed by consumers), `maven-publish` with a version, consumer R8 rules, binary-compatibility API dump checked in CI, compileSdk lowered to what the APIs need | |
| 4 | **Sample and guide.** A minimal `:sample` app (one screen, own brand, no Hilt) built in CI as the plug-in proof; the guide finished with the published coordinates | |

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
