# Handoff: where work stopped (2026-10-09)

Last commit on `main`: `896363a` (pushed). Everything below is **uncommitted, on disk only**.

## In progress: "Tide" theme transition (replaces the circular reveal)

Request: on the theme toggle, a full-screen wave sweeps across the screen, like the iPhone Duo's fold-open
animation. It turns the screen light or dark as it moves. The wave should have a sand-like structure and
grain, and move like water. It is shader-driven.

### Changed files
- `core/designsystem/.../foundation/material/TideShader.kt` (new): `TIDE_AGSL`, the full-screen AGSL wave.
  - Inputs: `content` (live new theme) and `previous` (frozen old frame).
  - Shape: a domain-warped fbm meander plus a swell bend the front.
  - Detail: 5-tap directional smear, sand-grain dissolve, wet-sand darkening, broken foam crest, caustic glints.
- `core/designsystem/.../foundation/ThemeReveal.kt` (rewritten). Public API unchanged: `HaloThemeRevealState.reveal(origin, motion, change)`,
  `LocalHaloThemeReveal`, `HaloThemeRevealHost(isDark)`.
  - Snapshot: freezes the frame with `layer.toImageBitmap()` and wraps it in a `BitmapShader`.
  - Theme flip: calls `change()`, then waits up to 1s for `isDark` to flip.
  - Animation: drives `front` and `time` with a `withFrameNanos` loop. Duration is 1300ms × slow-mo, eased with `HaloEasing.Standard`.
  - Render: sets `RenderEffect.createRuntimeShaderEffect(shader, "content")` on the root GraphicsLayer each frame.
  - Fallback: reduced motion, or no shader support, switches instantly.
- `core/designsystem/.../theme/Effects.kt`: Tide tokens `tideBand` 140dp, `tideDissolve` 14dp, `tideGrain` 1dp,
  `tideAmplitude` 28dp, `tideSmear` 36dp.

Lint passes and `:app:assembleDebug` builds.

### First device check (dark → light, 5× slow-mo): it works, but needs polish
Recording: `shots/tide-d2l.mp4` (gitignored).
1. **Status bar flips too early.** System-bar icons restyle the moment `change()` runs, so for about 1s there are
   dark icons on the still-dark frozen frame.
   - Fix: defer the bar restyle in `MainActivity` (its `DisposableEffect(dark)`) until the front has passed the
     top of the screen.
   - For example, expose a `chromeDark` state from `HaloThemeRevealState` that flips when `front` crosses the
     status-bar region.
2. **Wrong easing.** `HaloEasing.Standard` is front-loaded: the visible sweep is over by about 60% of the duration,
   and the rest is the front crawling off-screen.
   - Fix: add a sweep easing token, e.g. `HaloEasing.Sweep = CubicBezier(0.45, 0, 0.25, 1)`.
   - Also trim the `start`/`end` margin so the motion happens on screen.
3. **Smear too wide and stepped.** The 5 discrete taps show up as ghosted copies (the "Toggle" title is
   double-imaged, and there are white slashes in the dark area ahead of the front).
   - Fix: reduce `tideBand` (~80dp) and `tideSmear`.
   - Jitter the tap offsets per grain with `hash(cell)` (or use more taps), so the smear reads as a continuous
     stretch.
4. **Salt-and-pepper speckle reaches too far** into the old side. Tighten the dissolve and the `near` sand term.
5. **The wave starts at the corner opposite the far corner, not at the toggle.** That's a planar sweep from the
   minimum corner. Decide whether the front should grow from the trigger instead (curved near the origin,
   flattening as it travels).
6. **Light → dark is not verified yet.**

### How to capture
`shot.ps1 -NoBuild` also skips the install step, so install manually after building:
```
$adb="$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"; & $adb install -r -d app\build\outputs\apk\debug\app-debug.apk
powershell -ExecutionPolicy Bypass -File .claude/skills/halo-component/scripts/shot.ps1 -Page toggle -NoBuild -Theme dark -SlowMo 5
powershell -ExecutionPolicy Bypass -File .claude/skills/halo-component/scripts/find.ps1 -Desc "Dark mode"   # switch centre ≈ 1004 914
```
Then record: start `adb shell screenrecord --time-limit 10 --bit-rate 40000000 /sdcard/tide.mp4`, then `adb shell input tap 1004 914`,
then pull the file. Make a contact sheet with `ffmpeg -i tide.mp4 -vf "fps=5,scale=320:-2,tile=6x2" -frames:v 1 sheet.png`.

## Backlog after Tide
1. Put the AppearanceSwitch in the Home hero, as a slot from `ShowroomHomeScreen`.
2. Re-verify every page and component in the light theme (Button, RollingAmount, HoldToSend, Home, Color, Depth…) and fix what's off.
3. Update the docs:
   - `references/tokens.md`: light palette, lightning, Mechanical, HaloLight, HaloMaterials, switch sizes, Tide.
   - HaloToggle spec: velvet derivation, dish depth.
   - HoldToSend spec: network param.
4. Add `.gitattributes` (LF normalisation). Commit and push only when the owner asks.
5. Next phases of `docs/proposals/0001`: material engine (8 layers), touch/tilt light, card landing animation, textures.
   - CC0 texture downloads need the owner's approval.

Owner preferences:
- Pixel-perfect, with no visible flaw.
- Verify everything in both themes.
- No Material or third-party UI libraries.
- Commit and push only on request.
