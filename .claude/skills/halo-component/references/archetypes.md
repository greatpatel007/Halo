# Archetypes

Classify every request into one archetype. Each archetype has mandatory states, behaviour and semantics.
A hybrid (e.g. a toggle chip) satisfies the checklists of **both** archetypes.

---

## Pressable
*Buttons, icon buttons, tappable rows and cards, chips.*

**States:**
- default
- pressed
- focused (keyboard ring)
- disabled (`effects.disabledAlpha`, no indication, no haptic)
- loading, if it triggers async work

**Behaviour:**
- `HaloPressable`: scale 0.97 on Snap, release on Quick, touch-origin highlight.
- Fires the `Tap` haptic on click.
- Loading replaces the label in place; width must not jump unless the jump is the designed morph.
- Optional signature: a corner morph on press (`interactionSource.collectIsPressedAsState()` → animated radius, Snap).

**Semantics:**
- `Role.Button` and `onClickLabel` when the verb isn't the visible label.
- While loading, `stateDescription = "Loading"` and clicks are disabled.

**Checklist:**
- [ ] 48dp touch target even when the visual is smaller.
- [ ] The label never truncates at 1.0× font scale; it ellipsizes at 2×.
- [ ] Icon + label spacing is `spacing.sm`, and the icon is `iconMd` (20) inside controls of 48dp and up.

## Toggle
*Switch, checkbox, segmented control, selectable chip.*

**States:** off, on, pressed, focused and disabled, plus indeterminate where applicable.

**Behaviour:**
- **Physical toggles** (switch thumb, as in the reference `Refs/toggle.mp4`) move on `Mechanical` (ζ 1.0, k 400;
  a token added with HaloToggle): about 330ms, critically damped, no overshoot. A heavy, mechanical thumb never bounces.
  While pressed, the thumb leans about 15% toward its target (anticipation), then completes on release.
  Measurements: `docs/research/0001-reference-analysis.md`.
- Selection pills in segmented controls move on `Expressive`.
- A fill revealed under a moving thumb brightens as it's uncovered: it starts in the thumb's occlusion and ends
  fully lit.
- Color crossfades on `fade(Fast)`.
- Haptic is `ToggleOn` or `ToggleOff`; a segmented control uses `Tick`.

**Semantics:** `Modifier.toggleable` / `selectable` with `Role.Switch` / `Checkbox` / `Tab`, plus `stateDescription`.

**Checklist:**
- [ ] State is readable without color alone (position, icon or weight).
- [ ] A drag on the thumb works and follows the finger 1:1.

## Input
*Text and amount fields, keypad, address field.*

**States:** empty (placeholder), focused, filled, error (message + `Reject` + shake), disabled and read-only.

**Behaviour:**
- Built on `BasicTextField`. The cursor uses `accent.fill`; selection colors come from the theme.
- On error, play the `shake` recipe paired with the `Reject` haptic.
- Amount inputs use Paper Mono; leading zeros and the decimal separator follow `AmountFormat`.
- The IME never covers the field (`imePadding` on the screen).

**Semantics:** a label, an error via `error(...)`, and the paste action exposed.

**Checklist:**
- [ ] The caret is visible on every background.
- [ ] Error copy says how to fix the problem, not just what failed.
- [ ] Every keypad digit plays `Tick`; holding backspace repeats.

## Value display
*Balance, amount, rate ticker, progress, badge, QR.*

**States:** loading (skeleton, not a spinner), value, updating (animated), hidden (privacy mask) and error.

**Behaviour:**
- A changing number animates per digit (odometer, `Digit` spring). Direction follows the sign of the change.
- Units switch with a crossfade plus a slide.
- Privacy mask: digits morph to `•` and the width stays stable.

**Semantics:** the full spoken value ("4,812,350 sats, about 5,016 dollars"), not the individual glyphs.

**Checklist:**
- [ ] No layout jump while digits roll (every mono glyph has the same width).
- [ ] Trailing BTC zeros are dimmed; sats groups are narrowed.
- [ ] `₿` and `⚡` are vectors aligned to cap height.

## Gesture-commit
*Hold-to-send, swipe-to-confirm, pull-to-refresh, drag-to-dismiss.*

**States:** idle, engaged (finger down), progressing (0..1), threshold reached, committed (success), cancelled (springs back) and disabled.

**Behaviour:**
- Progress tracks the finger or time **linearly**. Release hands the velocity to a spring.
- Haptics: `Crescendo` or rising `tick(intensity)` while progressing, `Threshold` exactly once on crossing, `Confirm` or `Celebrate` on commit.
- Cancelling early springs back on `Quick` and calls `haptics.cancel()`.

**Semantics:** a custom action with the verb ("Send"), so TalkBack users never perform the gesture.

**Checklist:**
- [ ] Cannot commit by accident: a minimum hold or distance, and a scroll that starts in the area is not stolen.
- [ ] Interrupting mid-animation is clean, with no stuck state.
- [ ] The committed state is idempotent; double commits are impossible.

## Container / Overlay
*Card, bottom sheet, dialog, popover, toast, top bar.*

**States:** closed, opening, open, dragging, closing, and the predictive-back preview.

**Behaviour:**
- Enter on `Gentle`.
- Exit at about 60% of the enter time with `EmphasizedAccelerate`.
- Overlays use `HaloDepth.Raised` or `Glass` over a `scrim`.
- Sheets snap to anchors with velocity, and a downward drag dismisses.
- **Predictive back:** progress drives scale 1→0.92, corner radius → `radius.sheet` and translation; settles on commit or cancel.

**Semantics:** `paneTitle`, focus moves inside on open and returns to the trigger on close, and the scrim is clickable to dismiss.

**Checklist:**
- [ ] Respects insets (status bar, navigation bar, IME, cutouts).
- [ ] Never traps focus or the back gesture.

## Feedback
*Success, error, empty states, skeletons, toasts, confetti, celebration.*

**Behaviour:**
- One signature moment per flow.
- Success = check draw-on (PathMeasure) + `Confirm`/`Celebrate` + optional particle burst.
- Errors carry copy plus a recovery action.
- Skeletons shimmer (AGSL or a gradient sweep) and keep the final layout's dimensions.

**Semantics:** announce outcomes with `liveRegion = LiveRegionMode.Polite`.

**Checklist:**
- [ ] Particles and pulses are removed under `motion.reduced`.
- [ ] A toast doesn't block navigation or the dock.
