import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from project_paths import font_path

r = Path(__file__).resolve().parents[1]
a = r / "src/main/resources/assets/timelocked"
data = r / "src/main/resources/data/timelocked"
font = font_path()


def write(p, obj):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def text(draw, xy, s, size, fill):
    draw.text(xy, s, font=ImageFont.truetype(font, size), fill=fill, anchor="mm")


for key, title, sub, third, arrow in [
    (
        "coastal_restriction_sign",
        "SAPPHIRE",
        "ADVANCED RESEARCH CORPORATION",
        "RESTRICTED COAST",
        False,
    ),
    ("boardwalk_direction_sign", "SARC", "BOARDWALK ACCESS", "AUTHORIZED STAFF", True),
]:
    im = Image.new("RGB", (512, 384), "#20303b")
    d = ImageDraw.Draw(im)
    d.rectangle((5, 5, 506, 378), outline="#9daeb8", width=8)
    d.rectangle((21, 21, 490, 362), outline="#445764", width=2)
    text(d, (256, 80), title, 53, "#edf3f4")
    text(d, (256, 126), sub, 18, "#c0d1db")
    d.line((42, 159, 470, 159), fill="#d6b967", width=4)
    if arrow:
        d.polygon(
            [
                (256, 180),
                (310, 227),
                (276, 227),
                (276, 281),
                (236, 281),
                (236, 227),
                (202, 227),
            ],
            fill="#d6b967",
        )
        text(d, (256, 320), third, 22, "#dae2e6")
    else:
        text(d, (256, 219), third, 34, "#d6b967")
        text(d, (256, 278), "AUTHORIZED ACCESS ONLY", 23, "#dae2e6")
        text(d, (256, 326), "COASTAL RESEARCH SITE", 17, "#9dafba")
    for x in [18, 493]:
        for y in [18, 365]:
            d.ellipse((x - 4, y - 4, x + 4, y + 4), fill="#c4d0d6")
            d.line((x - 3, y, x + 3, y), fill="#596873", width=2)
    tp = a / "textures/block" / f"{key}.png"
    tp.parent.mkdir(parents=True, exist_ok=True)
    im.save(tp)

    # Raw native model: posts, feet, frame and raised front enamel plate.
    def box(fr, to, tex):
        return {
            "from": fr,
            "to": to,
            "faces": {
                f: {"texture": tex, "uv": [0, 0, 16, 16]}
                for f in ["north", "south", "east", "west", "up", "down"]
            },
        }

    el = [
        box([2, 0, 7], [4, 24, 9], "#steel"),
        box([12, 0, 7], [14, 24, 9], "#steel"),
        box([1, 0, 5], [5, 1, 11], "#steel"),
        box([11, 0, 5], [15, 1, 11], "#steel"),
        box([0, 19, 6], [16, 32, 10], "#steel"),
    ]
    plate = box([0.5, 19.5, 5.8], [15.5, 31.5, 6.1], "#steel")
    plate["faces"]["north"] = {"texture": "#front", "uv": [0, 0, 16, 16]}
    el.append(plate)
    write(
        a / "models/block" / f"{key}.json",
        {
            "textures": {
                "particle": "minecraft:block/gray_concrete",
                "steel": "minecraft:block/gray_concrete",
                "front": f"timelocked:block/{key}",
            },
            "elements": el,
        },
    )
    import copy

    for half, y0, y1 in [("lower", 0, 16), ("upper", 16, 32)]:
        clipped = []
        for e in el:
            low = max(y0, e["from"][1])
            high = min(y1, e["to"][1])
            if high <= low:
                continue
            part = copy.deepcopy(e)
            part["from"][1] = low - y0
            part["to"][1] = high - y0
            clipped.append(part)
        write(
            a / "models/block" / f"{key}_{half}.json",
            {
                "textures": {
                    "particle": "minecraft:block/gray_concrete",
                    "steel": "minecraft:block/gray_concrete",
                    "front": f"timelocked:block/{key}",
                },
                "elements": clipped,
            },
        )
    write(
        a / "blockstates" / f"{key}.json",
        {
            "variants": {
                f"facing={f},half={half}": {
                    "model": f"timelocked:block/{key}_{half}",
                    "y": y,
                }
                for half in ["lower", "upper"]
                for f, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]
            }
        },
    )
    write(
        a / "models/item" / f"{key}.json",
        {
            "parent": f"timelocked:block/{key}",
            "display": {
                "gui": {
                    "rotation": [25, 225, 0],
                    "translation": [0, -4, 0],
                    "scale": [0.45, 0.45, 0.45],
                }
            },
        },
    )
    write(
        data / "loot_table/blocks" / f"{key}.json",
        {
            "type": "minecraft:block",
            "pools": [
                {
                    "rolls": 1,
                    "entries": [
                        {"type": "minecraft:item", "name": f"timelocked:{key}"}
                    ],
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                }
            ],
        },
    )
f = a / "lang/en_us.json"
lang = json.loads(f.read_text(encoding="utf-8"))
lang.update(
    {
        "block.timelocked.coastal_restriction_sign": "Coastal Restriction Sign",
        "block.timelocked.boardwalk_direction_sign": "Boardwalk Direction Sign",
        "gamerule.timelockedStory": "TIMELOCKED Story",
        "gamerule.timelockedStory.description": "Start the home-to-beach opening for new Survival players in this campaign world.",
    }
)
write(f, lang)
