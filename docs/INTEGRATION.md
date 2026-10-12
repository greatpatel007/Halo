# Add Halo to your app

Halo is a native Jetpack Compose design system. It brings components, motion, scene lighting, haptics and sound, and
the Tide theme transition. You bring your brand colours and fonts, and you wrap your app in one call.

> **Status (proposal 0004):** step 1 is done (brand + one-call host). Still to come:
> - Android 8+ support (today: **Android 13+**, `minSdk 33`)
> - a published library (today: include the module from source, see [Install](#1-install))
> - a minimal sample app
>
> This guide is updated as each step lands.

## 1. Install

**Requirements:**
- A Compose app with `minSdk 33` and `compileSdk 37`
- Kotlin 2.x and the Compose compiler plugin

**Today (from source):**
1. Copy two folders from this repo into your project:
   - `core/designsystem`
   - `build-logic` (its convention plugins configure the module)
2. Add them to your settings and app build files:

```kotlin
// settings.gradle.kts
pluginManagement { includeBuild("build-logic") }
include(":core:designsystem")

// app/build.gradle.kts
dependencies { implementation(project(":core:designsystem")) }
```

The module also needs Halo's version catalog entries (`gradle/libs.versions.toml`, Compose BOM and androidx core /
lifecycle).

**After step 3 (published):** one line, `implementation("com.halo:designsystem:<version>")`.

The module's manifest brings the `VIBRATE` permission. It needs no other permissions: sensors and audio need none.

## 2. Wrap your app (one call)

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()          // optional: avoids a one-frame inset jump on launch
        setContent {
            HaloApp {
                YourNavHost()
            }
        }
    }
}
```

`HaloApp` sets up:
- the theme and its shared runtime: power, heat and lifecycle policy, the frame clock, sensors, light, HDR, haptics
  and sound;
- the appearance choice (stored for you) and the Tide transition around the whole window;
- edge-to-edge drawing on your canvas colour, and system-bar icons that flip as the wave crosses them.

**Required manifest line** (so a theme switch doesn't restart your activity mid-wave, and the splash screen follows
the user's choice):

```xml
<activity android:name=".MainActivity" android:configChanges="uiMode" ... />
```

Without it Halo still works, but it logs a warning and the splash screen follows the system theme instead.

## 3. Your brand

The quickest route is one accent colour; everything else is derived and contrast-checked in both themes:

```kotlin
val brand = haloBrand(accent = Color(0xFF3D7BFF))

HaloApp(brand = brand) { YourNavHost() }
```

**Full form:**

```kotlin
val brand = haloBrand(
    accent = Color(0xFF3D7BFF),      // buttons, toggles, focus, links
    bitcoin = Color(0xFFFF8A1F),     // your first signal colour (optional)
    lightning = Color(0xFF2EDBFF),   // your second signal colour (optional)
    fonts = HaloFonts(
        sans = FontFamily(Font(R.font.inter_regular), Font(R.font.inter_bold, FontWeight.Bold)),
        mono = FontFamily(Font(R.font.jetbrains_mono_regular)),
    ),
)
```

**What each seed controls:**
- **The seed itself** is the fill of a filled control in the dark theme.
- **Derived from it:** pressed, text, tint and glow, plus the light theme's deeper stops. The hue is kept, accent text
  reaches 4.5:1 on the canvas, and labels on fills get the ink with the most contrast.
- **Fonts:**
  - `sans` sets all UI and body text.
  - `mono` sets numbers, data and labels.
  - Sizes, weights and tracking stay Halo's.

**What stays Halo's in every app:** neutrals, spacing, radii, motion, the scene light, materials, haptics and sound.
That is the product.

**Preview a seed first:** Showroom → Foundations → Brand re-brands live components from a seed.

**Hand-tuned palettes:** build `HaloColors` yourself and pass `HaloBrand(dark = …, light = …, fonts = …)`.

## 4. Appearance (light / dark / system)

`HaloApp` stores the choice for you. To put a dark-mode switch anywhere:

```kotlin
HaloThemeSwitch()   // flips Dark ↔ Light and runs the Tide from the switch
```

Any other control can change the choice through the appearance:

```kotlin
val appearance = LocalHaloAppearance.current!!
appearance.setMode(HaloThemeMode.System)
```

**Already have a settings store** (DataStore, a repository, a ViewModel)? Adapt it instead of the built-in one:

```kotlin
val mode by viewModel.themeMode.collectAsStateWithLifecycle()   // your stored value
val appearance = rememberHaloAppearance(mode) { viewModel.setThemeMode(it) }
HaloApp(brand = brand, appearance = appearance) { YourNavHost() }
```

The switch shows the new choice on the next frame, before your store confirms it. Your store's value takes over once
it arrives.

## 5. Build screens

Use the components and tokens. Every token comes from `HaloTheme`:

```kotlin
Column(Modifier.padding(HaloTheme.spacing.lg)) {
    HaloText("Settings", style = HaloTheme.typography.headline)
    HaloButton("Continue", onClick = { … }, modifier = Modifier.fillMaxWidth())
    HaloToggle(checked = on, onCheckedChange = { on = it }, contentDescription = "Notifications")
}
```

**Components:**
- `HaloButton`
- `HaloToggle`
- `HaloRollingAmount`
- `HaloHoldToSend`
- `HaloLogo`
- `HaloThemeSwitch`

**Primitives:**
- `HaloText`
- `HaloPressable` (press physics + a haptic token)
- `HaloSurface` and the depth/material modifiers
- backdrop glass
- enter motion

**Feedback:** every action plays one token, for example `HaloTheme.haptics.perform(HaloHaptic.Confirm)`. Halo renders it
for the phone's vibration motor, with synthesised sound on the same frame. It respects the system's touch-feedback,
ringer and touch-sound settings. A user's in-app haptics/sound switches and strength are saved across launches.

**Rules for your own components:**
- **Colours, sizes and motion:**
  - Take them from `HaloTheme` (`colors`, `spacing`, `sizes`, `radius`, `motion.spring(…)`).
  - Avoid raw colours, raw dp values and `tween`.
- **Anything that runs continuously** should use `rememberAmbient(HaloTheme.runtime)`. It only runs while visible and
  foregrounded, and pauses under battery saver, heat and reduced motion.
- **Sensors:** use `HaloTheme.runtime.sensors.subscribe(…)`, never `SensorManager`.

## 6. What you get for free

**System behaviour:**
- Reduced motion ("Remove animations") is followed everywhere.
- Ambient effects and sensors pause in the background, under battery saver and when the phone is hot.
- On HDR screens, glows and light-emitting parts get brighter than standard white.

**Light and haptics:**
- The key light is world-fixed: tilt the phone and highlights move together.
- Haptics adapt to the phone: four tiers, from rich envelopes down to basic buzzes.

**Debugging:** Showroom → Foundations → Runtime shows everything live.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Activity restarts and the Tide cuts off on a theme switch | Add `android:configChanges="uiMode"` to the activity |
| Splash screen shows the wrong theme | Same manifest line (Halo then keeps the platform's per-app night mode in step) |
| Content under the status bar on launch for one frame | Call `enableEdgeToEdge()` in `onCreate` before `setContent` |
| `HaloThemeSwitch must be inside HaloApp` | Put it inside `HaloApp { }`, or pass `appearance = …` |
| No vibration on the emulator | Expected: it has no motor. Pass `hapticObserver` to `HaloApp` to log tokens |
