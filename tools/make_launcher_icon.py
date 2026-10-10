"""Build the Android adaptive launcher icon from the approved logo, unchanged.

The drawing (clock, arrow, "OnTime") is kept whole and centred so that it fits
the 66/108 safe circle of adaptive icons; only the empty paper margin is
reframed, extended with the logo's own paper colour. No stroke is altered.
Usage: python3 -I tools/make_launcher_icon.py
"""
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
LOGO = ROOT / "assets/branding/ontime-logo.png"
RES = ROOT / "app/src/main/res"
PAPER = (0xDD, 0xD9, 0xD0)  # median logo background, see Palette.kt
SAFE_RATIO = 33 / 108       # safe-zone radius over adaptive icon side
DENSITIES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}

logo = Image.open(LOGO).convert("RGB")
ink = logo.convert("L").point(lambda v: 255 if v < 160 else 0)
left, top, right, bottom = ink.getbbox()
cx, cy = (left + right) / 2, (top + bottom) / 2
pixels = ink.load()
radius = max(
    math.hypot(x - cx, y - cy)
    for y in range(top, bottom) for x in range(left, right) if pixels[x, y]
)
side = math.ceil(radius / SAFE_RATIO)
origin = (round(cx - side / 2), round(cy - side / 2))

canvas = Image.new("RGB", (side, side), PAPER)
canvas.paste(logo, (-origin[0], -origin[1]))
for name, size in DENSITIES.items():
    out = RES / f"mipmap-{name}" / "ic_launcher_foreground.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    canvas.resize((size, size), Image.LANCZOS).save(out, optimize=True)
print(f"drawing radius {radius:.0f}px, icon side {side}px")
