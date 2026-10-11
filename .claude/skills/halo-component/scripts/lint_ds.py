#!/usr/bin/env python3
"""
Halo design-system lint — the static half of the halo-component quality gate.

Usage (from the repo root):
    python3 -I .claude/skills/halo-component/scripts/lint_ds.py            # whole design system
    python3 -I .claude/skills/halo-component/scripts/lint_ds.py HaloButton # one component

Suppress a finding on one line with a trailing comment that states why:
    val x = 3.dp // lint-ignore: dp-literal optical nudge for the bolt glyph
Exit code 1 if any error is found.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
DS = ROOT / "core/designsystem/src/main/kotlin/com/halo/core/designsystem"
COMPONENTS = DS / "component"
REGISTRY = ROOT / "docs/components/REGISTRY.md"
SPECS = ROOT / "docs/components"
CATALOG = ROOT / "feature/showroom/src/main/kotlin/com/halo/feature/showroom/catalog/ShowroomCatalog.kt"

# Folders allowed to hold raw values (they *define* the tokens). `feedback` is the platform layer behind the
# haptic/sound tokens: the only place that may call vibration and audio APIs.
TOKEN_DIRS = {"theme", "icons", "feedback"}

BANNED_IMPORTS = [
    (r"^import androidx\.compose\.material3?\b", "material-import", "Material is not used: build the Halo equivalent from foundations."),
    (r"^import androidx\.compose\.material\.ripple", "ripple-import", "Use HaloIndication (via HaloPressable), not ripple."),
    (r"^import (com\.airbnb\.lottie|dev\.chrisbanes\.haze|com\.google\.accompanist)", "third-party-ui", "No third-party UI/animation libraries: hand-build it."),
]

LITERAL_RULES = [
    (r"Color\(0x[0-9A-Fa-f]+\)", "hex-color", "Raw color: use a semantic role from HaloTheme.colors."),
    (r"(?<![\w.])(?!0\.dp\b)(?!0f\.dp\b)\d+(\.\d+)?f?\.dp\b", "dp-literal", "Raw dp: use HaloTheme.spacing / sizes / radius / stroke."),
    (r"(?<![\w.])\d+(\.\d+)?f?\.(sp|em)\b", "sp-literal", "Raw text size: use a HaloTheme.typography style."),
    (r"(?<![.\w])(spring|tween|keyframes)\s*\(", "raw-animation-spec", "Raw animation spec: use HaloTheme.motion.spring(HaloSpring.X) / fade() / timed()."),
    (r"\b(HapticFeedbackConstants|performHapticFeedback|LocalHapticFeedback|VibratorManager)\b", "raw-haptics", "Raw haptics: use HaloTheme.haptics.perform(HaloHaptic.X)."),
    (r"\bFontFamily\.(Default|SansSerif|Monospace|Serif)\b", "system-font", "System font: use a HaloTheme.typography style."),
]

COMPONENT_RULES = [
    (r"\.(clickable|combinedClickable)\s*[({]", "raw-clickable", "Use HaloPressable: it bundles press scale, indication, haptic and semantics."),
    (r"\bTextStyle\s*\(", "adhoc-text-style", "Ad-hoc TextStyle: use (or .copy) a HaloTheme.typography token."),
]

errors: list[str] = []
warnings: list[str] = []


def rel(p: Path) -> str:
    return p.relative_to(ROOT).as_posix()


def ignored(line: str, rule: str) -> bool:
    m = re.search(r"//\s*lint-ignore:\s*([\w-]+)\s+\S", line)
    return bool(m and m.group(1) == rule)


def scan_file(path: Path, is_component: bool) -> None:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()
    in_token_dir = any(part in TOKEN_DIRS for part in path.relative_to(DS).parts[:-1])
    for no, line in enumerate(lines, 1):
        code = line.split("//")[0] if "lint-ignore" not in line else line
        stripped = line.strip()
        if stripped.startswith(("*", "/*", "//")):
            continue
        for pattern, rule, msg in BANNED_IMPORTS:
            if re.search(pattern, stripped):
                errors.append(f"{rel(path)}:{no}: [{rule}] {msg}")
        if in_token_dir or stripped.startswith("@Preview"):
            continue
        rules = LITERAL_RULES + (COMPONENT_RULES if is_component else [])
        for pattern, rule, msg in rules:
            if re.search(pattern, code.split("//")[0]) and not ignored(line, rule):
                errors.append(f"{rel(path)}:{no}: [{rule}] {msg}\n      {stripped}")
    if is_component:
        check_component_contract(path, text)


def check_component_contract(path: Path, text: str) -> None:
    name = path.stem
    if "@Preview" not in text:
        errors.append(f"{rel(path)}: [no-preview] Add at least one @Preview per key state (wrap in HaloPreview).")
    if re.search(r"pointerInput\s*\(|draggable\s*\(|anchoredDraggable\s*\(", text) and "semantics" not in text:
        errors.append(f"{rel(path)}: [gesture-semantics] Custom gestures need semantics (role / stateDescription / customActions).")
    if re.search(r"fun\s+\w+\s*\([^)]*onClick", text, re.S) and "HaloPressable" not in text:
        warnings.append(f"{rel(path)}: [pressable] Takes onClick but never uses HaloPressable; confirm press scale, indication, haptic and semantics are covered.")
    public_fns = re.findall(r"^@Composable\s*\n\s*fun\s+(\w+)", text, re.M)
    if not public_fns:
        warnings.append(f"{rel(path)}: [no-public-api] No public @Composable found.")
    if "modifier: Modifier = Modifier" not in text:
        errors.append(f"{rel(path)}: [modifier-param] Public components take `modifier: Modifier = Modifier` as the first optional parameter.")
    spec = SPECS / f"{name}.md"
    if not spec.exists():
        errors.append(f"{rel(path)}: [missing-spec] Write docs/components/{name}.md from templates/ComponentSpec.md.")
    registry = REGISTRY.read_text(encoding="utf-8") if REGISTRY.exists() else ""
    if f"| {name} |" not in registry:
        errors.append(f"{rel(path)}: [missing-registry] Add a row for {name} to docs/components/REGISTRY.md.")
    else:
        row = next((l for l in registry.splitlines() if l.startswith(f"| {name} |")), "")
        page_id = re.search(r"halo://showroom/([\w-]+)", row)
        catalog = CATALOG.read_text(encoding="utf-8") if CATALOG.exists() else ""
        if not page_id:
            errors.append(f"REGISTRY.md: [missing-deeplink] Row for {name} needs its Showroom deep link (halo://showroom/<id>).")
        elif f'ShowroomPage("{page_id.group(1)}"' not in catalog:
            errors.append(f"ShowroomCatalog.kt: [missing-showroom] No ShowroomPage(\"{page_id.group(1)}\", ...) registered for {name}.")


def main() -> int:
    only = sys.argv[1] if len(sys.argv) > 1 else None
    files = sorted(DS.rglob("*.kt"))
    if only:
        files = [f for f in files if f.stem == only]
        if not files:
            print(f"No design-system file named {only}.kt")
            return 1
    for f in files:
        scan_file(f, is_component=COMPONENTS in f.parents)
    for w in warnings:
        print(f"warning: {w}")
    for e in errors:
        print(f"error: {e}")
    count = len(files)
    if errors:
        print(f"\nFAILED - {len(errors)} error(s), {len(warnings)} warning(s) in {count} file(s).")
        return 1
    print(f"OK - {count} file(s) clean, {len(warnings)} warning(s).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
