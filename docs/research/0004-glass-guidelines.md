# Research 0004: what the best glass UIs do (Apple, Samsung, Nothing, practitioners)

2026-10-13. Input for [proposal 0005](../proposals/0005-clean-glass.md). Compared against the Surface lab
(`halo://showroom/surface`).

## Apple: Liquid Glass (HIG Materials, "Adopting Liquid Glass", WWDC25 "Meet Liquid Glass")
**Layers**
- Glass is the **functional layer** (navigation, toolbars, tab bars, sheets, menus) that floats *above* content.
  The content layer uses standard materials, not glass: "glass there would compete … and muddy the hierarchy."
- **Never glass on glass.** Content placed on glass uses fills, transparency and vibrancy, so it reads as part of the material.
- Controls in the content layer (toggles, sliders, segmented controls) are quiet at rest and **become glass only while
  touched**: the knob lifts into glass during the interaction.
- Use it sparingly; reserve custom glass for the most important functional elements.

**Optics (what makes it read as glass)**
- **Lensing is the defining property:** it bends and concentrates light at the edges, which separates it from the
  background while staying transparent. Elements materialise by ramping the lensing up, not by fading.
- **Specular highlights** follow the geometry and move with the lights and device motion.
- **Adaptive shadows:** stronger over text, weaker over plain light backgrounds; larger glass casts deeper shadows.
- **Light spill:** colourful content nearby spills subtly onto the glass and into its shadow.
- **Size changes thickness:** bigger glass gets deeper shadows, stronger lensing, softer scattering.

**Adaptivity and legibility**
- Small glass (bars, buttons) **flips light/dark with the background**; large glass (menus, sidebars) adapts but doesn't flip.
- Tint and dynamic range shift continuously so glyphs stay legible.
- Variants: **Regular** (default, adaptive, for anything with text) and **Clear** (only over rich media, needs a ~35%
  dimming layer). Never mix them.
- Text on materials uses **vibrant** label colours (primary/secondary/tertiary), never flat greys.
- Scroll edge effect: content dissolves under floating bars, so titles stay clear.

**Interaction**
- The material **illuminates from within, starting under the fingertip**, spreading through the element and to nearby glass.
- **Gel-like flex:** it flexes with the input and springs back.

**Colour**
- **Tint only for emphasis**, the primary action. "When every element is tinted, nothing stands out." Put brand colour
  in the content layer instead.

**Shape and accessibility**
- Corners are **concentric** with their containers and the hardware.
- Reduce Transparency → frostier; Increase Contrast → mostly black/white with a contrasting border; Reduce Motion →
  no elastic effects.

## Samsung: One UI "Visual depth"
- Blur, dim and shadow exist only to build hierarchy; don't overuse them.
- **Blur never alone:** pair it with a dim (light dim on light, dark dim on dark). Too weak blurs the information,
  too strong blurs the hierarchy. Apply it evenly.
- Unrelated next screen: blur + dim the previous one. Related screen: shadow only.
- Shadows are soft and short. **Not 3D depth**; no long or high-contrast shadows. **Never dim and shadow together.**
- Don't show several structural levels on one screen.
- (Leaks for One UI 9.5 point to shinier glass edges and widgets as elevated translucent layers, unconfirmed.)

## Nothing
- Restraint: monochrome, functional widgets, dot-matrix used deliberately and never at the cost of readability.
- Light as ambient information (Glyph) that doesn't demand attention.

## Practitioners (glassmorphism and accessibility)
- Legibility first: a tint/dim layer behind text, an edge to define the panel, test WCAG contrast at the worst
  backdrop, never put critical information on busy or animated backgrounds.

## Android (Material 3 / Expressive, AOSP, Android developers)
**Surfaces: tone, not glass**
- Material 3 replaced elevation-tinted surfaces with **tone-based surface roles**: `surface` (the base) and a
  `surfaceContainer` ladder (`Lowest`, `Low`, default, `High`, `Highest`) chosen by containment need, not elevation.
  Shadows are optional emphasis on top. App content sits on these opaque tonal containers.
- **Blur is a system-layer pattern**, not an app-content one: Android 16 added it to notifications, Quick Settings and
  Recents ("depth while keeping context"); Android 17 extends it to the volume panel, power menu and widget picker,
  **tinted by the Dynamic Color theme** and kept subtle. Reports note blur is not part of Material 3 Expressive for apps.

**Window blur (AOSP guidance, Android 12+)**
- Blur behind: ~20 px reads as depth of field, ~80 px as frosted glass; avoid > 150 px (cost).
- **Always pair blur with a translucent colour layer** for legibility; use **less dim with blur** (sample: 0.1 with blur,
  0.4 without).
- Blur can be **disabled at runtime** (battery saver, weak GPUs): listen with `addCrossWindowBlurEnabledListener` and
  switch to the non-blur design.

**Material 3 Expressive (2025)**
- Emphasis comes from **size, colour, shape and containment**: the key action is larger and coloured; like elements are
  grouped; a **floating toolbar** pill doesn't span the screen and lets the background show at its sides.
- Spring-based motion (standard and expressive schemes; *spatial* vs *effects* springs) and **shape morphing on press**.

**Haptics (developer.android.com)**
- Less is more; prefer **clear, crisp** primitives, then rich sequences, buzzy only as a fallback for urgent events.
- Plan for device tiers (primitives / amplitude / on-off); ≥ 50 ms between primitives, ≥ 1.4× intensity steps to be
  perceptible; use `HapticFeedbackConstants` for system consistency. (Halo's tiered engine already follows this.)

**Accessibility and power**
- Android 14+: the user's **contrast setting** (`UiModeManager.getContrast()`, −1…1, with a change listener) is meant to
  be honoured by apps that render their own UI outside Material: raise opacity, add borders, deepen text at high contrast.
- Remove-animations (animator scale 0), font scale up to 200% (non-linear), battery saver (blur off).

**Performance (AGSL / RenderEffect)**
- Blur via `RenderEffect` (12+); refraction / lensing via AGSL `RuntimeShader` (13+), with a fallback below.
- Cost grows with glass **area and count** and with how often the captured backdrop changes: keep few, small glass
  surfaces and re-capture the backdrop only when it changes. Test on mid-range phones under thermal load.

## Android-native synthesis (what Halo should do)
Apple and Android agree, Android more strictly:
1. **Content = tonal surfaces**, opaque, on Halo's grain base (an `surfaceContainer`-style ladder). No glass, no blur.
   This alone fixes most of the contrast problem.
2. **Glass = the floating/system layer only**: a floating toolbar/tab bar, the top bar, sheets, menus, toasts, the
   dock. Blur + a translucent tint (less dim when blurred), lensing at the edge on 13+, a designed non-blur fallback
   that also kicks in when the system disables blur.
3. **Controls**: tactile and expressive on press (spring shape morph, light from the touch point); glass only while
   touched; colour only for the key action and real meaning.
4. **Honour the platform**: contrast setting, battery saver, remove-animations, font scale, haptic tiers.

## Where the Surface lab falls short
| # | Gap | What the best do |
|---|---|---|
| 1 | Every card is glass (balance, actions, settings, activity) and controls sit on it: glass on glass | Content on a calm material; glass only for the floating layer and for controls *while touched* |
| 2 | No lensing: our glass is blur + tint + rims, so panes read as cards | Edge lensing (refraction of the backdrop at the rim) is what says "glass" |
| 3 | Press = sink only | Light blooms from the fingertip inside the glass; gel flex; the control lifts into glass |
| 4 | Every control is tinted (units tab, keys, toggles) | Tint only the primary action and real meaning (network). Neutral otherwise |
| 5 | Static shadows and tints | Adaptive: shadow stronger over text, small glass flips light/dark, tint keeps glyphs legible |
| 6 | Flat label colours | Vibrant label levels tuned per material |
| 7 | Blur without dim; dim and shadow mixed | Blur always with a matching dim; shadow or dim, never both |
| 8 | Tab track (pill) inside a 28 dp pane with 20 dp padding | Concentric corners (`radius.nested`) |
| 9 | No accessibility modes | Reduce transparency, increase contrast, reduced motion variants |

## Sources
- AOSP, Window blurs: https://source.android.com/docs/core/display/window-blurs
- Material blog, tone-based surfaces: https://material.io/blog/tone-based-surface-color-m3
- Android 16 Material 3 Expressive: https://9to5google.com/2025/05/13/android-16-material-3-expressive-redesign/
- Android 17 blur: https://9to5google.com/2026/05/19/android-17-beta-1-blur/ · https://9to5google.com/2026/01/25/android-17-blur/
- Material 3 Expressive research (leak coverage): https://9to5google.com/2025/05/05/material-3-expressive-leak/
- Android haptics principles: https://developer.android.com/develop/ui/views/haptics/haptics-principles
- UiModeManager contrast (API 34): https://developer.android.com/reference/kotlin/android/app/UiModeManager
- Using AGSL: https://developer.android.com/guide/topics/graphics/agsl/using-agsl
- Haze (glass performance notes): https://docsearch.algolia.com/mcp/docs/repo/chrisbanes/haze
- Apple, Liquid Glass overview: https://developer.apple.com/documentation/technologyoverviews/liquid-glass
- Apple, Adopting Liquid Glass: https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass
- Apple, HIG Materials: https://developer.apple.com/design/human-interface-guidelines/materials
- Apple, WWDC25 "Meet Liquid Glass": https://developer.apple.com/videos/play/wwdc2025/219/
- Samsung, One UI Visual depth: https://developer.samsung.com/one-ui/structure/visual-depth.html
- One UI 9.5 glass leaks: https://www.digitaltrends.com/phones/samsung-may-be-going-in-for-the-full-liquid-glass-look-in-oneui-9-5/
- Nothing OS evolution: https://nothing.community/d/61774-nothing-os-evolution
- Glassmorphism and accessibility: https://axesslab.com/?p=5111 · https://clay.global/blog/glassmorphism-ui
