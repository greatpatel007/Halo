#!/usr/bin/env python3
"""
Halo dev tooling: the device half of the halo-component quality gate. Cross-platform (Windows, macOS, Linux),
standard library only.

Usage (from anywhere; paths are resolved against the repo root):
    python3 -I .claude/skills/halo-component/scripts/halo.py gradle :app:assembleDebug
    python3 -I .claude/skills/halo-component/scripts/halo.py shot button
    python3 -I .claude/skills/halo-component/scripts/halo.py shot button --no-build --slowmo 5 --grid 8 --out shots/button-grid.png
    python3 -I .claude/skills/halo-component/scripts/halo.py find "Hold to send"
    python3 -I .claude/skills/halo-component/scripts/halo.py zoom shots/toggle-dark.png 400 1200 --w 300 --h 160 --scale 4
    python3 -I .claude/skills/halo-component/scripts/halo.py doctor

On Windows use `python` (or `py`) instead of `python3`.

Toolchain discovery (first match wins):
  JDK:  $JAVA_HOME → Android Studio's bundled JBR in its default install location → `java` on PATH.
  SDK:  $ANDROID_HOME → $ANDROID_SDK_ROOT → sdk.dir in local.properties → the OS default SDK location.
  AVD:  --avd → $HALO_AVD → Pixel_10_Pro.
"""
from __future__ import annotations

import argparse
import os
import platform
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
PACKAGE = "com.halo.wallet"
IS_WINDOWS = os.name == "nt"
IS_MAC = platform.system() == "Darwin"
EXE = ".exe" if IS_WINDOWS else ""
HOME = Path.home()


def die(message: str) -> None:
    print(f"error: {message}", file=sys.stderr)
    sys.exit(1)


# ── Toolchain discovery ─────────────────────────────────────────────────────────────────────────────────────


def _studio_jbr_candidates() -> list[Path]:
    if IS_WINDOWS:
        bases = [os.environ.get("ProgramFiles", r"C:\Program Files"), os.environ.get("LOCALAPPDATA", "")]
        return [Path(b) / sub / "jbr" for b in bases if b
                for sub in ("Android/Android Studio", "Programs/Android Studio")]
    if IS_MAC:
        apps = []
        for base in (Path("/Applications"), HOME / "Applications"):
            apps += sorted(base.glob("Android Studio*.app"))
        return [a / "Contents/jbr/Contents/Home" for a in apps]
    return [Path("/opt/android-studio/jbr"), HOME / "android-studio/jbr",
            Path("/snap/android-studio/current/jbr"), Path("/usr/local/android-studio/jbr"),
            *sorted((HOME / ".local/share/JetBrains/Toolbox/apps").glob("android-studio*/jbr"))]


def java_home() -> Path | None:
    env = os.environ.get("JAVA_HOME")
    if env and (Path(env) / "bin" / f"java{EXE}").exists():
        return Path(env)
    for candidate in _studio_jbr_candidates():
        if (candidate / "bin" / f"java{EXE}").exists():
            return candidate
    return None  # gradlew falls back to `java` on PATH


def _sdk_from_local_properties() -> Path | None:
    props = ROOT / "local.properties"
    if not props.exists():
        return None
    for line in props.read_text(encoding="utf-8").splitlines():
        if line.strip().startswith("sdk.dir="):
            # local.properties escapes ':' and '\' (Windows paths are written as C\:\\Users\\...).
            value = line.split("=", 1)[1].strip().replace("\\:", ":").replace("\\\\", "\\")
            return Path(value)
    return None


def android_sdk() -> Path | None:
    candidates = [os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT"), _sdk_from_local_properties()]
    if IS_WINDOWS:
        candidates.append(Path(os.environ.get("LOCALAPPDATA", HOME / "AppData/Local")) / "Android/Sdk")
    elif IS_MAC:
        candidates.append(HOME / "Library/Android/sdk")
    else:
        candidates.append(HOME / "Android/Sdk")
    for c in candidates:
        if c and Path(c).is_dir():
            return Path(c)
    return None


def sdk_tool(subdir: str, name: str) -> str:
    sdk = android_sdk()
    if sdk and (sdk / subdir / f"{name}{EXE}").exists():
        return str(sdk / subdir / f"{name}{EXE}")
    found = shutil.which(name)
    if found:
        return found
    die(f"{name} not found. Install the Android SDK and set ANDROID_HOME (or sdk.dir in local.properties).")


def adb(*args: str, check: bool = True) -> subprocess.CompletedProcess:
    result = subprocess.run([sdk_tool("platform-tools", "adb"), *args], capture_output=True, text=True)
    if check and result.returncode != 0:
        die(f"adb {' '.join(args)} failed: {(result.stdout + result.stderr).strip()}")
    return result


def gradle_env() -> dict[str, str]:
    env = dict(os.environ)
    jdk = java_home()
    if jdk:
        env["JAVA_HOME"] = str(jdk)
    sdk = android_sdk()
    if sdk and not env.get("ANDROID_HOME") and not (ROOT / "local.properties").exists():
        env["ANDROID_HOME"] = str(sdk)
    return env


def run_gradle(tasks: list[str]) -> int:
    wrapper = [str(ROOT / "gradlew.bat")] if IS_WINDOWS else ["sh", str(ROOT / "gradlew")]
    return subprocess.run([*wrapper, *tasks], cwd=ROOT, env=gradle_env()).returncode


# ── Commands ────────────────────────────────────────────────────────────────────────────────────────────────


def cmd_doctor(_: argparse.Namespace) -> None:
    print(f"OS        {platform.system()} {platform.machine()}")
    print(f"repo      {ROOT}")
    print(f"JDK       {java_home() or 'not found (falls back to java on PATH)'}")
    print(f"SDK       {android_sdk() or 'not found'}")
    print(f"adb       {sdk_tool('platform-tools', 'adb')}")


def ensure_device(avd: str) -> None:
    adb("start-server", check=False)
    devices = adb("devices").stdout
    if re.search(r"\tdevice$", devices, re.MULTILINE):
        return
    print(f"Booting {avd}...")
    detach = ({"creationflags": subprocess.DETACHED_PROCESS | subprocess.CREATE_NEW_PROCESS_GROUP}
              if IS_WINDOWS else {"start_new_session": True})
    subprocess.Popen([sdk_tool("emulator", "emulator"), "-avd", avd, "-no-snapshot-save", "-no-boot-anim"],
                     stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, **detach)
    # Poll rather than `adb wait-for-device`, which can fault while the emulator is still coming up.
    deadline = time.monotonic() + 240
    while adb("shell", "getprop", "sys.boot_completed", check=False).stdout.strip() != "1":
        if time.monotonic() > deadline:
            die("Emulator did not finish booting")
        time.sleep(2)


def cmd_shot(args: argparse.Namespace) -> None:
    out = ROOT / (args.out or f"shots/{args.page}.png")
    out.parent.mkdir(parents=True, exist_ok=True)

    # 1. Device: reuse a running one, otherwise boot the AVD and wait for boot_completed.
    ensure_device(args.avd)

    # 2. Build + install (installing restarts the app process, so every shot starts clean).
    if not args.no_build:
        if run_gradle([":app:assembleDebug", "--console=plain", "-q"]) != 0:
            die("Build failed")
        adb("install", "-r", "-d", str(ROOT / "app/build/outputs/apk/debug/app-debug.apk"))
    else:
        # Start clean either way: a running app routes the link in place and keeps process-wide debug state.
        adb("shell", "am", "force-stop", PACKAGE, check=False)

    # 3. Open the page (+ debug dock parameters) and let enter choreography settle.
    query = f"?slowmo={args.slowmo:g}&grid={args.grid}&rm={1 if args.reduced else 0}&legacy={1 if args.legacy else 0}"
    if args.theme:
        query += f"&theme={args.theme}"
    # Single-quoted so the device shell doesn't treat '&' as a separator.
    adb("shell", "am", "start", "-W", "-a", "android.intent.action.VIEW",
        "-d", f"'halo://showroom/{args.page}{query}'", PACKAGE)
    time.sleep(args.settle_ms / 1000)

    # 4. Screenshot (binary-safe: capture on device, then pull).
    adb("shell", "screencap", "-p", "/sdcard/halo_shot.png")
    adb("pull", "/sdcard/halo_shot.png", str(out))
    adb("shell", "rm", "/sdcard/halo_shot.png", check=False)
    print(f"Saved {out}")


def cmd_find(args: argparse.Namespace) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        local = Path(tmp) / "halo_ui.xml"
        # uiautomator intermittently fails ("null root node") while the UI is animating; retry, and never read a
        # stale dump left over from an earlier run.
        for _ in range(5):
            adb("shell", "rm", "-f", "/sdcard/halo_ui.xml", check=False)
            dump = adb("shell", "uiautomator", "dump", "/sdcard/halo_ui.xml", check=False)
            if "dumped to" in dump.stdout:
                break
            time.sleep(1)
        else:
            die(f"uiautomator dump failed: {(dump.stdout + dump.stderr).strip()}")
        adb("pull", "/sdcard/halo_ui.xml", str(local))
        tree = ET.parse(local)
    found = False
    for node in tree.iter("node"):
        desc, text = node.get("content-desc", ""), node.get("text", "")
        if args.desc.lower() not in desc.lower() and args.desc.lower() not in text.lower():
            continue
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            print(f"{desc or text:<40} {node.get('bounds'):<26} centre {(x1 + x2) // 2} {(y1 + y2) // 2}")
            found = True
    if not found:
        print(f"No element matching '{args.desc}'")
        sys.exit(1)


# ── zoom: nearest-neighbour crop + enlarge, pure Python PNG codec (8-bit, non-interlaced) ─────────────────────

_CHANNELS = {0: 1, 2: 3, 4: 2, 6: 4}  # grey, RGB, grey+alpha, RGBA


def _read_png(path: Path) -> tuple[int, int, int, list[bytes]]:
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    pos, idat, header = 8, bytearray(), None
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        chunk = data[pos + 8:pos + 8 + length]
        if kind == b"IHDR":
            header = struct.unpack(">IIBBBBB", chunk)
        elif kind == b"IDAT":
            idat += chunk
        elif kind == b"IEND":
            break
        pos += 12 + length
    width, height, depth, colour, _, _, interlace = header
    if depth != 8 or colour not in _CHANNELS or interlace:
        raise ValueError(f"unsupported PNG (depth {depth}, colour type {colour}, interlace {interlace})")
    bpp = _CHANNELS[colour]
    stride = width * bpp
    raw = zlib.decompress(bytes(idat))
    rows, prev = [], bytearray(stride)
    for y in range(height):
        start = y * (stride + 1)
        ftype, row = raw[start], bytearray(raw[start + 1:start + 1 + stride])
        for i in range(stride):
            a = row[i - bpp] if i >= bpp else 0
            b = prev[i]
            c = prev[i - bpp] if i >= bpp else 0
            if ftype == 1:
                row[i] = (row[i] + a) & 0xFF
            elif ftype == 2:
                row[i] = (row[i] + b) & 0xFF
            elif ftype == 3:
                row[i] = (row[i] + ((a + b) >> 1)) & 0xFF
            elif ftype == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                row[i] = (row[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 0xFF
        rows.append(bytes(row))
        prev = row
    return width, height, colour, rows


def _write_png(path: Path, width: int, height: int, colour: int, rows: list[bytes]) -> None:
    def chunk(kind: bytes, payload: bytes) -> bytes:
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload))
    raw = b"".join(b"\x00" + r for r in rows)
    path.write_bytes(b"\x89PNG\r\n\x1a\n"
                     + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, colour, 0, 0, 0))
                     + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def cmd_zoom(args: argparse.Namespace) -> None:
    src = Path(args.input)
    if not src.is_absolute() and not src.exists():
        src = ROOT / src
    out = Path(args.out) if args.out else src.with_name(f"{src.stem}-zoom-{args.x}-{args.y}.png")
    try:
        width, height, colour, rows = _read_png(src)
    except ValueError as e:
        ffmpeg = shutil.which("ffmpeg")
        if not ffmpeg:
            die(f"{e}; install ffmpeg to zoom this file")
        vf = f"crop={args.w}:{args.h}:{args.x}:{args.y},scale=iw*{args.scale}:ih*{args.scale}:flags=neighbor"
        if subprocess.run([ffmpeg, "-v", "error", "-y", "-i", str(src), "-vf", vf, str(out)]).returncode != 0:
            die("zoom failed")
        print(f"Saved {out}")
        return
    if args.x < 0 or args.y < 0 or args.x + args.w > width or args.y + args.h > height:
        die(f"region {args.w}x{args.h}+{args.x}+{args.y} is outside the {width}x{height} image")
    bpp = _CHANNELS[colour]
    zoomed = []
    for row in rows[args.y:args.y + args.h]:
        crop = row[args.x * bpp:(args.x + args.w) * bpp]
        line = b"".join(crop[i:i + bpp] * args.scale for i in range(0, len(crop), bpp))
        zoomed += [line] * args.scale
    _write_png(out, args.w * args.scale, args.h * args.scale, colour, zoomed)
    print(f"Saved {out}")


def main() -> None:
    # Device labels can hold characters a legacy Windows console code page can't encode; don't crash on them.
    sys.stdout.reconfigure(errors="replace")
    # Pass everything after `gradle` through untouched (argparse would swallow flags like --version).
    if len(sys.argv) > 1 and sys.argv[1] == "gradle":
        sys.exit(run_gradle(sys.argv[2:]))

    parser = argparse.ArgumentParser(description="Halo dev tooling (build, screenshot, find, zoom).")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("gradle", help="run the Gradle wrapper with the discovered JDK/SDK (args passed through)")

    p = sub.add_parser("doctor", help="print the discovered toolchain")
    p.set_defaults(func=cmd_doctor)

    p = sub.add_parser("shot", help="build → install → open a Showroom page → screenshot")
    p.add_argument("page", help="Showroom page id (halo://showroom/<id>)")
    p.add_argument("--out", help="PNG path relative to the repo root (default shots/<page>.png)")
    p.add_argument("--no-build", action="store_true", help="skip Gradle and reuse the installed APK")
    p.add_argument("--settle-ms", type=int, default=1500)
    p.add_argument("--slowmo", type=float, default=1)
    p.add_argument("--grid", type=int, choices=(0, 4, 8), default=0)
    p.add_argument("--reduced", action="store_true", help="reduced motion")
    p.add_argument("--legacy", action="store_true", help="render as Android 8-12 (no shaders, no blur)")
    p.add_argument("--theme", choices=("dark", "light", "system"))
    p.add_argument("--avd", default=os.environ.get("HALO_AVD", "Pixel_10_Pro"))
    p.set_defaults(func=cmd_shot)

    p = sub.add_parser("find", help="print bounds + centre (device px) of elements by accessibility label")
    p.add_argument("desc", help="substring of content-desc or text")
    p.set_defaults(func=cmd_find)

    p = sub.add_parser("zoom", help="crop a screenshot region and enlarge it with nearest-neighbour scaling")
    p.add_argument("input")
    p.add_argument("x", type=int)
    p.add_argument("y", type=int)
    p.add_argument("--w", type=int, default=240)
    p.add_argument("--h", type=int, default=140)
    p.add_argument("--scale", type=int, default=4)
    p.add_argument("--out")
    p.set_defaults(func=cmd_zoom)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
