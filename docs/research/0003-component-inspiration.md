# Research 0003: inspiration for the planned components

**Date:** 2026-10-12 · **For:** the Planned table in `docs/components/REGISTRY.md`

Halo's language, which every reference is filtered through:
- **Materials:** physical, like soft-touch black and matte ceramic. Light comes from one world-fixed key light, and
  shadows only exist as a consequence of that light.
- **Colour:** brand blue `#2A67FF` for actions, orange for on-chain Bitcoin, brand yellow `#F5DE4B` for Lightning (updated 2026-10-12).
- **Type:** Paper Mono for every number.
- **Motion:** Apple's fluid-interface physics.
- **Feedback:** designed haptics and sound for every action.

The rule for each reference: take the *behaviour* (what makes it feel right), never the look. The look is always Halo's
material and light.

## Reference families
| Family | What to take | Not to take |
|---|---|---|
| **Apple fluid interfaces** (WWDC 2018) and iOS sheets / Liquid Glass | Rubber-banding at edges, momentum projected to the nearest detent, critically damped springs, bounce only after a throw, draggable glass indicators that fire a tick when crossing a boundary | The glass-everything look |
| **Family wallet "trays"** (Benji Taylor) | Progressive disclosure: one tray that morphs height between steps instead of stacking screens; continuity through motion | Number animations that stop abruptly (a critic's note) |
| **Vaul / Sonner** (Emil Kowalski) | Snap points, a close threshold by distance *and* velocity, the background scaling back under a sheet, stacked toasts that expand on touch | The web styling |
| **Braun ET66 / Dieter Rams** | Round convex single-function keys, high-contrast function keys, "less but better" | Glossy plastic |
| **Teenage Engineering** (OP-1, EP-133) | Oversized, satisfying mechanical keys; mode shown by colour cues; monospace type; depth and texture without skeuomorphism | Orange as a UI colour (ours means Bitcoin) |
| **Nothing OS** | Widgets as controls, dot-matrix used only where it adds character, monochrome restraint | Dot-matrix everywhere (they pulled it back themselves) |
| **Rauno Freiberg, "Invisible Details of Interaction Design"** | "Fidgetability"; interactions that survive spamming, slow networks and interruption | — |
| **Bitcoin wallets:** Phoenix, Muun, Bitkit, Cake Wallet 6, bitcoin.design guide | Muun's single unified balance and mempool fee estimator; amounts in sats; a customizable balance card; the guide's address and backup patterns | Generic wallet styling |
| **Apple Pay / Wallet / Card, Cash App** | Payment-success choreography, keypad amount entry with shake on error, a card material that reacts to tilt | Brand looks |

## Per component
| # | Component | References | Halo version |
|---|---|---|---|
| 1 | **Sheet** | iOS sheet detents, Fluid Interfaces, Vaul snap points, Family trays | A soft-touch or ceramic slab rising from the bottom. The screen behind recedes (scales slightly, darkens) under frosted glass. Momentum is projected to the nearest detent, with rubber-banding past the top. A Thud haptic plays on settle, and its contact shadow comes from the key light. Trays morph height between steps |
| 2 | **Segmented control** | iOS 26 glass segmented control (draggable indicator, tick per boundary), OP-1 colour cues | The indicator is a toggle-style thumb (graphite/ceramic relief) sliding in a recessed track. You can drag it with rubber-banding; it ticks per segment and lands with a Mechanical spring. Colour follows meaning: blue in general, orange or yellow when choosing a network |
| 3 | **Amount keypad** | Braun ET66 convex keys, EP-133 mechanical keys, Cash App amount entry | Round convex keys lit by the relief painter, with real key travel (press depth plus a contact shadow that shrinks). Paper Mono digits. The amount grows with a rolling digit. Delete accelerates when held. Going over the balance shakes the amount with a Reject haptic |
| 4 | **Text field** | Rauno's robustness notes, Linear inputs | A recessed channel like the toggle track; focus lights a blue rim from inside. A paste chip appears when the clipboard holds an address or invoice. The network is detected as you type (an orange or yellow badge grows in). An error shakes with a Reject haptic |
| 5 | **Transaction row** | Apple Card activity, Phoenix, Muun's unified list | Direction glyph on a small relief disc, title and time, and a Paper Mono amount in network colour. Pending breathes with a slow glow on the shared clock; failed is muted with a warning chip |
| 6 | **Balance card** | Apple Card (metal that reacts to tilt), Cake Wallet 6's customizable card, Muun's single balance | A lit material card whose highlight moves with the tilt light, with the rolling balance in Paper Mono. The privacy hide dissolves digits to grain. A slim on-chain/Lightning split bar sits in the network colours |
| 7 | **List row** | iOS Settings, Nothing's widgets-as-controls | Flat row: icon, title, subtitle, and a trailing toggle, chevron or value. The whole row is pressable (touch-origin light) |
| 8 | **Chip / badge** | OP-1 colour-cued mode labels | Paper Mono micro caps. A status dot with an emitted glow (HDR-aware); pending pulses |
| 9 | **Tab bar** | iOS 26 Liquid Glass tab bar (draggable indicator, haptic per boundary) | The glass dock (as in the Showroom debug dock), with a sliding lit thumb behind the active icon. Draggable, with a tick per tab |
| 10 | **Top bar** | iOS large-title collapse | Large title that shrinks into a glass strip as you scroll (the Home status strip, generalised) |
| 11 | **Toast** | Dynamic Island (one object morphing between compact and expanded), Sonner stacking and swipe-to-dismiss | A pill that grows out of the status-bar area and morphs to fit its content. It stacks and expands on touch, and you can swipe it away. "Payment arrived" plays Ping plus network colour |
| 12 | **QR code** | Apple Pay Cash and Cash App QR with a centre mark | Own encoder (unit-tested). Modules as tiny rounded dots, with the halo mark in the middle. The reveal sweeps outward from the halo like the Tide, and the code stays a scannable high-contrast black on white |
| 13 | **Address** | bitcoin.design guide (chunked display), Phoenix | Paper Mono in groups of 4 with alternating tone. Tap to copy: the chunks ripple and a check rolls in |
| 14 | **Fee selector** | Muun mempool estimator, mempool.space tiers | Three ceramic cards (slow / normal / fast) showing time and sats, or a detented slider with a mechanical tick per detent |
| 15 | **Chart** | Stocks/brokerage scrub charts | The line drawn as a thin emitted filament (HDR-aware). Scrubbing ticks per data point, and the amount above rolls to the value under your finger |
| 16 | **Confirmations** | Apple Watch rings | A ring in the logo's glass tube that fills with liquid per confirmation (0/6 → 6/6) |
| 17 | **Payment success** | Apple Pay check, Cash App success | The halo fills with liquid, morphs into a check, and sparks fly in network colour. A Celebrate haptic plays |
| 18 | **Skeleton** | — | Placeholders with a grain shimmer sweeping in the key light's direction. A gradient band without shaders, static under reduced motion |
| 19 | **Dialog** | iOS alert | A ceramic slab over a frosted backdrop; destructive actions are red with a Threshold haptic |
| 20 | **Context menu** | iOS context menu (the item lifts, the background blurs) | A long press lifts the row (shadow grows from the key light) and a glass menu unfolds from it |
| 21 | **Scanner frame** | iOS Camera QR detection (corners snap onto the code) | Four blue corner brackets that spring onto the detected code, a scan light sweeping, and Threshold on lock |
| 22 | **Avatar** | Cash App avatars | Initials in Paper Mono on a convex relief disc, tinted by a hash of the name |
| 23 | **PIN pad** | iOS passcode | Reuses the keypad keys. Dots fill like small liquid drops, and a wrong PIN shakes with Reject |
| 24 | **Seed phrase** | bitcoin.design backup guide | Words in a numbered grid under grain blur; hold to reveal (a press-and-hold on the shared clock). Verify by tapping words in order |
| 25 | **Swipe to confirm** | iOS "slide to unlock" (light running through the label) | A toggle thumb in a recessed track, with light sweeping through the label. Threshold at the commit point, Celebrate on send |
| 26 | **Pull to refresh** | — | The halo liquid fills as you pull, with rubber-band resistance; release bursts into a spin |
| 27 | **Selection** | Braun switches, the iOS checkmark | A radio as a small dish that a ball rolls into; a checkbox as a key that latches down with a check stroke |

## Owner's decisions (2026-10-12)
Round 1: 18 directions accepted as written above. Rounds 2–3 for the 9 the owner asked to rethink:
| # | Component | Decision |
|---|---|---|
| 6 | Balance card | Electric glass card after `card.mp4` (research 0001): deep-navy glass, Fresnel blue rim, emissive halo inside with bloom and rim light bleed, lifts and ignites on press |
| 14 | Fee selector | Every choice shows arrival time *and* cost (sats + fiat); Custom opens a notched slider |
| 15 | Chart | Glass tube pillars with glowing liquid (the halo's glass); scrub lights a tube and ripples its liquid |
| 16 | Confirmations | Plain words ("Settling · 2 of 3 · ~10 min") plus three small glass tubes that fill per block; on-chain only |
| 17 | Payment success | Must be unique vs Google Pay / PhonePe / Cash App / Revolut (green check + chime). Options on the board: the halo seals; per-network arc vs pour; glass shatters into light |
| 19 | Dialog | The familiar system alert layout, rebuilt in Halo materials. System permission prompts can't be restyled |
| 21 | Scanner | After CRED Scan & Pay: auto-zoom on the code, scanner as a premium object (frosted bezel, lit rim), brackets snap, morph into the pay sheet |
| 22 | Avatar | Flat monogram |
| 27 | Selection | Dropped: reuse toggle, segmented control and checkmark rows |

### Round 4: charts (2026-10-12)
References: `Refs/chart references/` (dashboard card kits, light + dark). Replaces the round-2 row 15 above.
- **Direction: blend.** The references' layout and lightness (emissive strokes, dots, ticks, rays, pills; bloom on
  dark, a pale tinted haze on light; no drop shadows) with Halo glass and liquid touches (glowing liquid in area
  fills and gauges, glass arcs and tubes where they fit).
- **Scope: every chart type**, so a screen can use whatever it needs: line/area with scrub, candlesticks with volume,
  rings / segmented donut / gauge / dot ring, radial burst, heatmap, and Bitcoin-native kinds (UTXO bubbles sized by
  value, mempool fee bands as ridgeline/stream, fee range bars per block).
- **Colour: meaning-based.** Series take Halo roles (orange on-chain, yellow Lightning, blue brand/accent, positive /
  negative for deltas); extra series are tints of those roles. No rainbow palettes.
- **Card anatomy** (shared by all references): title or headline value + delta, the chart as hero, a two-up stat
  pair (label with glyph, mono value, delta, muted secondary unit), then key–value rows with mono values and
  hairlines. All numbers Paper Mono, axis labels included.

Competitor success screens: no teardown found; the shared pattern (green check, rising chime, sometimes confetti) is
from general knowledge. CRED: https://www.digit.in/news/apps/cred-upi-payment-service-is-called-scan-pay-heres-how-it-works-65472.html/amp/,
https://digitalterminal.in/amp/story/news/31206.html, https://analyticsindiamag.com/cred-gets-a-new-design-upgrade-with-charcoal/

## Sources
- Fluid interfaces (secondhand summary of WWDC 2018 "Designing Fluid Interfaces"): https://mcpservers.org/agent-skills/emilkowalski/apple-design
- Family wallet interaction design (newsletter on Benji Taylor's post): https://studio21design.substack.com/p/august-2024-bitcoin-design-monthly
- Vaul drawer: https://bestofjs.org/projects/vaul, https://zenn.dev/hayato94087/articles/ea7c4eb4f4c307
- Sonner: https://ui.shadcn.com/docs/components/aria/sonner
- Teenage Engineering design: https://read.cv/jdsimcoe/inside-design-at-teenage-engineering, https://blakecrosley.com/de/guides/design/teenage-engineering, https://www.soundonsound.com/reviews/teenage-engineering-ep-133-ko-ii?amp
- Braun ET66: https://www.slashgear.com/braun-et66-calculator-classic-gets-reissued-24283441
- Nothing OS: https://nothing.community/d/61774-nothing-os-evolution
- Rauno Freiberg: https://www.uxtools.co/blog/your-ui-needs-more-walt-disney, https://designlobster.substack.com/p/150-fidgetability
- Dynamic Island / Live Activities guidelines: https://9to5mac.com/2022/09/26/iphone-14-pro-live-activities-guidelines/
- Liquid Glass controls (third-party descriptions): https://medium.com/@jaikrishnavj/mastering-ios-26s-liquid-glass-a-comprehensive-developer-s-handbook-2bba9965b024
- Bitcoin wallets: https://bitcoin.design/guide/resources/code-resources/, https://docs.cakewallet.com/release-notes/cake-wallet-6.0, https://alternativeto.net/software/muun-1/about
