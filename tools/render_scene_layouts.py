import json
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from project_paths import font_path, preview_directory

r = Path(__file__).resolve().parents[1]
out = preview_directory()
fontfile = font_path()


def font(n):
    return ImageFont.truetype(fontfile, n)


def label(d, xy, t, size=20, fill="#c6d3df"):
    d.text(xy, t, font=font(size), fill=fill)


data = json.loads((r / "validation/coast-layout.json").read_text(encoding="utf-8"))
im = Image.new("RGB", (1400, 1080), "#10141e")
d = ImageDraw.Draw(im)
label(d, (40, 28), "TIMELOCKED: Zero Point / coast & access", 30, "#e6eef5")
label(
    d, (40, 74), "Generated block layout • parking, pedestrian route and boardwalk", 22
)
colors = {
    "black_concrete": "#2c3039",
    "gray_concrete": "#57616b",
    "light_gray_concrete": "#a8b1b7",
    "smooth_stone": "#c4c9c8",
    "white_concrete": "#edece3",
    "blue_concrete": "#547aaa",
    "sand": "#cfbf96",
    "gravel": "#87817a",
    "spruce_planks": "#946d48",
    "water": "#365969",
    "sea_lantern": "#b8e5d9",
    "coastal_restriction_sign": "#cfab58",
    "boardwalk_direction_sign": "#cfab58",
    "iron_bars": "#b8e5d9",
}
x0, x1, z0, z1 = [data[k] for k in ["x0", "x1", "z0", "z1"]]
scale = 10
ox, oy = 40, 146
for n, key in enumerate(data["cells"]):
    x = n % (x1 - x0 + 1)
    z = n // (x1 - x0 + 1)
    col = colors.get(
        key.removeprefix("minecraft:").removeprefix("timelocked:"), "#525d4b"
    )
    d.rectangle(
        (
            ox + x * scale,
            oy + z * scale,
            ox + (x + 1) * scale - 1,
            oy + (z + 1) * scale - 1,
        ),
        fill=col,
    )


def point(x, z):
    return ox + (x - x0 + 0.5) * scale, oy + (z - z0 + 0.5) * scale


# Pin only the actual recorded wake anchor; no invented cinematic boat position.
x, y = point(280, 0)
d.ellipse((x - 9, y - 9, x + 9, y + 9), fill="#e9f5f6", outline="#162330", width=3)
label(d, (1090, 146), "SITE KEY", 20, "#e6eef5")
for i, (key, name) in enumerate(
    [
        ("black_concrete", "Marked parking bays"),
        ("blue_concrete", "Reserved bays"),
        ("gray_concrete", "Vehicle access"),
        ("smooth_stone", "Pedestrian route"),
        ("spruce_planks", "Boardwalk to SARC"),
        ("coastal_restriction_sign", "Modeled signs"),
        ("sea_lantern", "Corner lighting"),
        ("water", "Coastal water"),
    ]
):
    y = 195 + i * 63
    d.rectangle((1090, y, 1114, y + 24), fill=colors[key])
    label(d, (1126, y), name, 18)
label(d, (1090, 748), "N", 24)
d.line((1101, 806, 1101, 782), fill="#c6d3df", width=3)
d.polygon([(1101, 773), (1093, 786), (1109, 786)], fill="#c6d3df")
label(
    d,
    (40, 963),
    "Vehicle access continues west to the graded SARC approach road.",
    22,
    "#e0e8ee",
)
label(
    d,
    (40, 1004),
    "Native server block-layout plan. Not an in-game screenshot.",
    20,
    "#95a7b9",
)
im.save(out / "coast-parking-plan.png")
# The TV is a source-renderer layout study, not a screenshot or image-based asset.
s = (r / "src/main/java/com/timelocked/client/MonitorRenderer.java").read_text(
    encoding="utf-8"
)
bar = re.search(r"int\[\] bars=\{(.*?)\}", s).group(1)
bars = [int(v, 16) for v in bar.split(",")]
for off in [False, True]:
    im = Image.new("RGB", (1400, 900), "#10141e")
    d = ImageDraw.Draw(im)
    label(
        d, (40, 28), "TIMELOCKED: Zero Point / one continuous television", 30, "#e6eef5"
    )
    label(
        d,
        (40, 74),
        "6 × 3 native monitor wall • "
        + ("standby" if off else "local late-night broadcast"),
        22,
    )
    cx, cy = 660, 425
    sc = 150
    hw, hh = 2.94, 1.43

    def rect(x0, y0, x1, y1, c):
        d.rectangle((cx + x0 * sc, cy - y1 * sc, cx + x1 * sc, cy - y0 * sc), fill=c)

    rect(-3.08, -1.58, 3.08, 1.58, "#1e2931")
    rect(-3.025, -1.525, 3.025, 1.525, "#53616a")
    rect(-3, -1.5, 3, 1.5, "#202831")
    rect(-hw, -hh, hw, hh, "#06090d")
    # Cabinet supports the full wall; button is mounted beside, not in front of screen.
    rect(-3, -2.5, 3, -1.59, "#473c32")
    rect(-0.025, -2.48, 0.025, -1.6, "#292625")
    rect(3.08, -2.5, 4, -1.5, "#473c32")
    rect(3.08, -1.5, 4, -0.5, "#53616a")
    rect(3.42, -1.08, 3.59, -0.92, "#9ca3a5")
    rect(hw - 0.12, -hh + 0.04, hw - 0.08, -hh + 0.06, "#b83e39" if off else "#78c9b2")
    if not off:
        bw = (2 * hw - 0.16) / len(bars)
        for i, c in enumerate(bars):
            rect(
                -hw + 0.08 + i * bw,
                -hh * 0.5,
                -hw + 0.08 + (i + 1) * bw,
                hh * 0.45,
                "#%06x" % c,
            )
        label(
            d,
            (cx + (-hw + 0.15) * sc, cy - (hh - 0.25) * sc - 12),
            "LOCAL BROADCAST",
            27,
            "#e6f2f2",
        )
        label(
            d,
            (cx + (-hw + 0.15) * sc, cy - (-hh + 0.25) * sc - 8),
            "LATE NIGHT  /  RAIN",
            18,
            "#aec4d2",
        )
    label(
        d,
        (40, 842),
        "Source-renderer layout study. Not an in-game capture.",
        20,
        "#95a7b9",
    )
    im.save(out / ("television-standby.png" if off else "television-on.png"))
