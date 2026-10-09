# Handoff: where work stopped (2026-10-09)

Last commit on `main`: the Tide theme transition, polished and moved onto spring physics (pushed).

## Tide theme transition: done, fine-tuning later

On the theme toggle, a full-screen AGSL wave with a sand texture sweeps across the screen
(`foundation/material/TideShader.kt`, `foundation/ThemeReveal.kt`). Verified on the emulator at 1× and 5×, in both
directions, including a mid-wave reversal.

- **Shape.** A conic front focused on the switch: `m = mix(along, r, TIDE_BEND = 0.6)`. It starts as an egg around
  the switch and flattens as it travels. The band has a constant pixel width (distances are divided by |∇m|).
- **Motion (Apple's fluid-interface principles).**
  - Driven by `HaloSpring.Tide`, a critically damped spring with stiffness 7, roughly Apple's "smooth" spring with a
    2.4s perceptual duration.
  - It launches with a kick (`TIDE_KICK = 0.5`), so it starts at full speed and only decelerates, like water up a
    beach.
  - It is on screen for ~1.2s at 1×.
  - The meander grows in over the first band of travel, so the wave is born at the switch with no pop.
- **Interruptible.**
  - Flipping the switch again mid-wave reverses the front, keeping its momentum, and the theme follows it back.
  - The washed ("behind") and unwashed ("ahead") regions keep their themes. Whichever one isn't live comes from a
    frozen frame (`swap` uniform).
  - Only the latest reveal or reversal owns clean-up (a `generation` counter).
- **Reduced motion, or no shader support:** a crossfade (`motion.fade()`, 120ms when reduced) instead of a hard cut.
- **System bars.** `HaloThemeRevealHost` owns the icon appearance. Each bar flips at the midpoint of the fronts
  touching its two ends.
- **Sand and smear.**
  - Tokens: `tideBand` 80dp, `tideDissolve` 10dp, `tideSmear` 20dp.
  - Tap offsets are jittered per grain.
  - Grain thresholds stay in [0.06, 0.94]. Before, the full 0–1 range left ~5% speckle across the whole band.
- **Shader warm-up.** The shader is drawn invisibly for 2 frames at start-up, in a mode where it outputs exactly the
  live content.

### Fine-tuning backlog: tap-to-wave latency (owner: "work with it for now")
After the tap, the wave appears about 200ms later in a **debug** build.
- Measured timeline:
  - frame capture: 24ms
  - `setApplicationNightMode`: 2.5ms
  - **full-tree recomposition with the new palette: ~126ms**
  - the next frame then picks up the flip
- Done so far:
  - `PlatformAppearanceRepository` emits the choice before the DataStore write (~200ms).
  - `MainActivity` drives the palette from the stored mode (`darkOverride`), not the uiMode config change.
- Next steps:
  - Measure on a release build. Its APK isn't signed; sign it with the debug key to install.
  - Reduce recomposition cost on a palette change, e.g. read colours in draw/lambdas, check stability.
  - Consider re-tuning the first-use shader compile on a cold start (~235ms seen once; the warm-up may not cover the
    hardware-bitmap input).
- Other taste calls:
  - the bend (0.6 grows backward 5× faster)
  - glints that read like smoke wisps on the dark side

### How to capture
```
$adb="$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"; & $adb install -r -d app\build\outputs\apk\debug\app-debug.apk
powershell -ExecutionPolicy Bypass -File .claude/skills/halo-component/scripts/shot.ps1 -Page toggle -NoBuild -Theme dark -SlowMo 5
```
Record with `adb shell screenrecord --size 640x1428 --bit-rate 40000000 --time-limit 9 /sdcard/tide.mp4`.
- Native 1280×2856 fails to encode, and the automatic fallback stretches to 720×1280.
- While recording, tap the switch with `adb shell input tap 1004 914`.
- Pull the file, then keep only the changing frames:
  `ffmpeg -i tide.mp4 -vf "mpdecimate,fps=4" -fps_mode vfr f%03d.png`.

## Backlog after Tide
1. Put the AppearanceSwitch in the Home hero, as a slot from `ShowroomHomeScreen`.
2. Re-verify every page and component in the light theme (Button, RollingAmount, HoldToSend, Home, Color, Depth…) and fix what's off.
3. Update the docs:
   - `references/tokens.md`: light palette, lightning, Mechanical, HaloLight, HaloMaterials, switch sizes, Tide
     (incl. `HaloEasing.Sweep`).
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
