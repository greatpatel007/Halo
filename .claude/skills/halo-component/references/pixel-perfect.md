# Pixel-perfect checklist

Run this against every screenshot (`shots/<id>.png`). Turn on the dock's **grid 4/8** overlay and the **slow-mo** for motion.
Screenshots from the Pixel 10 Pro emulator are 1280×2856 at 480 dpi, so **1dp = 3px**.

## Geometry
- [ ] Every padding, gap and size is a token: the 4dp grid, gutter 20, controls 36/48/56/64.
- [ ] Content aligns to the 20dp gutter. Edges inside cards align to the card padding (`spacing.lg`).
- [ ] Nested corners are concentric: inner radius = `radius.nested(outer, padding)`.
- [ ] Hairlines are 1 physical px (`stroke.hairline`). Borders sit *inside* the shape (inset by half the stroke) and never blur on edges.
- [ ] Offsets are whole pixels: `IntOffset(x.roundToInt(), …)` in `offset {}`, so nothing shimmers at rest.
- [ ] Touch targets are ≥ 48dp, even when the visual is smaller (pad the pressable, not the visual).

- [ ] Mixed-content labels (Satoshi text + Paper Mono amount) use one gap token between runs. Mono spaces are a
      full cell wide, so unit gaps use `AmountFormat.satsWithUnitAnnotated` (narrowed), not `" sats"`.

## Type
- [ ] Line boxes are trimmed (theme styles do this). Vertical rhythm is the line height, not font padding.
- [ ] Mixed-size text in a row aligns on the **baseline** (`Modifier.alignByBaseline()`), not the centre.
- [ ] Numbers that are values use Paper Mono. Digits don't shift width while animating.
- [ ] `₿` and `⚡` are vectors sized to the text cap height. No fallback-font glyphs anywhere: scan for odd shapes.
- [ ] Nothing truncates at 1.0× font scale. At 2.0× it wraps or ellipsizes gracefully and controls grow in height.

## Optical
- [ ] Icons inside circles are optically centred (asymmetric glyphs like play or send may need a 0.5–1dp nudge;
      justify it with `// lint-ignore: dp-literal <reason>`).
- [ ] Icon stroke matches the text weight next to it (1.75 at 24dp, scaled at 20dp).
- [ ] Lime appears only on actionable or selected things. Orange appears only on Bitcoin/Lightning meaning.
- [ ] Glow reads as light, not as a shadow or smudge: check the edges against `canvas`.

## Motion (review at 5× slow-mo)
- [ ] Each moving property uses the token named in the spec's motion table.
- [ ] No pops: every appearing element fades and/or rises, and nothing teleports unless reduced motion is on.
- [ ] Interrupt every animation mid-flight (tap again, reverse the drag). It retargets smoothly.
- [ ] Layout doesn't jump during animation: sizes are reserved or animate.
- [ ] Reduced motion (dock → rm): positions snap, nothing essential is lost, decoration is gone.

## States
- [ ] Every state in the spec's matrix is visible on the Showroom page and matches the spec.
- [ ] The disabled state uses `effects.disabledAlpha`, no indication and no haptic.
- [ ] The focus ring appears with a keyboard: `adb shell input keyevent KEYCODE_TAB`.
