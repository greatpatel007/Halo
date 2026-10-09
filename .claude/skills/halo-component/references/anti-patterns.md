# Anti-patterns (reject on sight)

## AI-design tells
- Purple-blue gradients, rainbow mesh backgrounds, gradient text, emoji as icons.
- Glassmorphism everywhere. Glass is **L3 only**: floating chrome over moving content.
- A glow on everything. One lit element per view, the thing you want tapped.
- Over-rounded "pill everything", random radii, inconsistent paddings (13dp, 18dp).
- Centred everything; three equal cards in a row as the default layout.
- Lorem ipsum or "John Doe". Use realistic, fictional wallet content (see `DemoData`).

## Motion
- Duration-based easing on spatial movement: `tween(300)` for position or scale. Use springs.
- Keyframed wiggle shakes. Use the spring-impulse shake.
- Animations that can't be interrupted, or block taps until they finish.
- Everything animating at once. Stagger with a cap, and keep one signature moment per flow.
- Bounce on serious states (errors, warnings) or on large surfaces (screens, sheets use `Gentle`, ζ = 1).
- Infinite pulsing on more than one element, or with reduced motion on.
- Per-frame recomposition: `val x by animateFloatAsState(...)` passed into `Modifier.offset(x.dp)`.

## Android
- The default ripple, M3 components, `MaterialTheme` colors leaking in.
- Hard-coded status-bar heights, `fitsSystemWindows` hacks, ignoring the IME.
- Intercepting back without predictive feedback.
- Using `Toast` or system `AlertDialog`: build Halo feedback/overlay components.

## Light & shadow
- A decorative drop shadow: a fixed offset/blur shadow that ignores the scene light. Shadows must come from the
  material Occlusion layer, cast from the shared key light (top-left).
- Highlights or rims on different sides of neighbouring elements (inconsistent light direction).
- Emission without consequence. A glowing element should light its surroundings (rim light, bleed) or it reads as a sticker.
- Checking only one theme. Dark and light ship together.

## Fintech
- A spinner where a skeleton fits; layout shift when data arrives.
- Proportional digits for amounts; numbers that jump width while updating.
- Orange used decoratively; green/red as the only carrier of meaning (add sign, icon or label).
- Amount precision lost (doubles for money). Domain uses `Sats` (Long) and `BigDecimal`; the design system receives `Long` sats.
- Sending on a single tap. Commits need intent (hold, swipe or a review step).

## Code
- Literals instead of tokens (the lint catches these). Fix the token set rather than suppressing.
- **A parameter named like a scope member.** `size`, `alpha`, `shape`, `color` and `density` parameters shadow
  `DrawScope.size` and `GraphicsLayerScope.alpha` inside lambdas. Either the compiler picks the parameter or, worse, it
  compiles and draws wrong. Write `this.size` in draw lambdas, and name locals `contentAlpha`, not `alpha`.
- **Overlays that ignore a morphing container.** When a container collapses or expands (pill → circle), every layer
  inside it (labels, icons, highlights) must clip to the *live* geometry or fade out before the edge reaches it.
  Otherwise text spills outside the shape mid-transition.
- **Private copies of a shared primitive.** The second time a visual (spinner, check draw-on, burst) is needed, move it
  to `foundation/` and delete the copy.
- Business logic, formatting rules or data loading inside a design-system component.
- `composed {}` modifiers, `remember` inside draw lambdas, allocation in draw.
- Public API without `modifier`, or with `modifier` not as the first optional parameter.
