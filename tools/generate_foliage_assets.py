import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

R = Path("src/main/resources")
A = R / "assets/timelocked"
D = R / "data/timelocked"


def write(p, o):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(o, indent=2), encoding="utf-8")


def texture(n, base, kind):
    rng = random.Random(n)
    im = Image.new("RGBA", (32, 32))
    pix = im.load()
    for y in range(32):
        for x in range(32):
            v = rng.randint(-12, 12) + (
                int(11 * math.sin(x * 1.7 + math.sin(y / 8)))
                if kind == "bark"
                else int(9 * math.sin(x * 0.7 + y * 0.4))
                if kind == "leaf"
                else 0
            )
            pix[x, y] = tuple(max(0, min(255, c + v)) for c in base) + (255,)
    d = ImageDraw.Draw(im)
    if kind == "bark":
        for x in [3, 10, 19, 27]:
            d.line(
                [(x, 0), (x - 1, 9), (x + 1, 19), (x, 31)],
                fill=(58, 62, 83, 255),
                width=1,
            )
        d.arc((12, 8, 22, 22), 0, 340, fill=(160, 164, 184, 255), width=1)
    elif kind == "leaf":
        for q in range(22):
            x, y = rng.randrange(32), rng.randrange(32)
            d.rectangle((x, y, x + 2, y + 1), fill=(0, 0, 0, 0))
        for x, y in [(4, 7), (16, 20), (26, 4), (21, 29)]:
            d.point((x, y), fill=(227, 244, 226, 255))
    else:
        d.line(
            (15, 0, 15, 31),
            fill=tuple(min(255, c + 30) for c in base) + (255,),
            width=2,
        )
        for y in range(4, 32, 6):
            d.line(
                (5, y, 15, y + 5, 26, y),
                fill=tuple(max(0, c - 24) for c in base) + (255,),
                width=1,
            )
    p = A / f"textures/block/{n}.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    im.save(p)


def cub(a, b):
    return {
        "from": a,
        "to": b,
        "faces": {
            f: {"texture": "#all", "uv": [0, 0, 16, 16]}
            for f in ["north", "south", "east", "west", "up", "down"]
        },
    }


blocks = {
    "celestial_bark": ((108, 114, 141), "bark"),
    "silverleaf_canopy": ((115, 163, 157), "leaf"),
    "moonveil_canopy": ((106, 110, 168), "leaf"),
    "hanging_starleaf": ((129, 188, 192), "leaf"),
}
for n, (c, k) in blocks.items():
    texture(n, c, k)
    if n == "celestial_bark":
        write(
            A / f"models/block/{n}.json",
            {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": f"timelocked:block/{n}"},
            },
        )
        write(
            A / f"blockstates/{n}.json",
            {
                "variants": {
                    f"axis={axis}": dict({"model": f"timelocked:block/{n}"}, **extra)
                    for axis, extra in [
                        ("y", {}),
                        ("x", {"x": 90, "y": 90}),
                        ("z", {"x": 90}),
                    ]
                }
            },
        )
    else:
        if n == "hanging_starleaf":
            o = {
                "render_type": "minecraft:cutout",
                "textures": {
                    "all": f"timelocked:block/{n}",
                    "particle": f"timelocked:block/{n}",
                },
                "elements": [
                    cub([6, 0, 6], [10, 16, 10]),
                    cub([2, 1, 4], [5, 12, 8]),
                    cub([11, 3, 8], [14, 15, 12]),
                ],
            }
        else:
            o = {
                "parent": "minecraft:block/cube_all",
                "render_type": "minecraft:cutout",
                "textures": {"all": f"timelocked:block/{n}"},
            }
        write(A / f"models/block/{n}.json", o)
        write(
            A / f"blockstates/{n}.json",
            {"variants": {"": {"model": f"timelocked:block/{n}"}}},
        )
plants = {
    "moon_fern": (96, 160, 166),
    "lantern_bloom": (238, 186, 102),
    "prism_reed": (105, 184, 200),
    "dusk_lotus": (161, 120, 180),
    "star_bloom": (183, 190, 231),
    "spore_sprout": (121, 145, 186),
    "ashen_tuft": (114, 105, 124),
    "celestial_frond": (133, 181, 172),
}
for n, c in plants.items():
    texture(n, c, "plant")
    if n in ["moon_fern", "celestial_frond", "ashen_tuft"]:
        els = [cub([7, 0, 7], [9, 11, 9])]
        for y, w in [(2, 6), (5, 5), (8, 3)]:
            els += [
                cub([8 - w, y, 6], [8 + w, y + 2, 10]),
                cub([6, y, 8 - w], [10, y + 2, 8 + w]),
            ]
        els.append(cub([7, 10, 7], [9, 14, 9]))
    elif n == "prism_reed":
        els = []
        for x, z, h in [(4, 5, 13), (10, 9, 16), (7, 12, 11)]:
            els += [
                cub([x, 0, z], [x + 1, h, z + 1]),
                cub([x - 1, h - 4, z - 1], [x + 2, h, z + 2]),
            ]
    elif n == "lantern_bloom":
        els = [
            cub([7, 0, 7], [9, 10, 9]),
            cub([5, 9, 5], [11, 15, 11]),
            cub([6, 8, 6], [10, 9, 10]),
            cub([6, 15, 6], [10, 16, 10]),
            cub([2, 3, 6], [7, 5, 9]),
            cub([9, 5, 7], [14, 7, 10]),
        ]
    elif n == "spore_sprout":
        els = []
        for x, z, h in [(4, 5, 7), (9, 9, 11), (11, 4, 6)]:
            els += [
                cub([x, 0, z], [x + 2, h, z + 2]),
                cub([x - 2, h, z - 2], [x + 4, h + 2, z + 4]),
            ]
    else:
        h = 5 if n == "dusk_lotus" else 10
        els = [
            cub([7, 0, 7], [9, h, 9]),
            cub([5, h, 5], [11, h + 2, 11]),
            cub([1, h - 1, 6], [5, h + 1, 10]),
            cub([11, h - 1, 6], [15, h + 1, 10]),
            cub([6, h - 1, 1], [10, h + 1, 5]),
            cub([6, h - 1, 11], [10, h + 1, 15]),
            cub([7, h + 2, 7], [9, h + 3, 9]),
        ]
    write(
        A / f"models/block/{n}.json",
        {
            "render_type": "minecraft:cutout",
            "textures": {
                "all": f"timelocked:block/{n}",
                "particle": f"timelocked:block/{n}",
            },
            "elements": els,
        },
    )
    if n in list(plants)[:4]:
        write(
            A / f"blockstates/{n}.json",
            {
                "variants": {
                    "": [
                        {"model": f"timelocked:block/{n}", "y": y}
                        for y in [0, 90, 180, 270]
                    ]
                }
            },
        )
for n in list(blocks) + list(plants)[:4]:
    write(A / f"models/item/{n}.json", {"parent": f"timelocked:block/{n}"})
    write(
        D / f"loot_table/blocks/{n}.json",
        {
            "type": "minecraft:block",
            "pools": [
                {
                    "rolls": 1,
                    "entries": [{"type": "minecraft:item", "name": f"timelocked:{n}"}],
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                }
            ],
        },
    )
p = R / "data/minecraft/tags/block/mineable/axe.json"
o = (
    json.loads(p.read_text(encoding="utf-8"))
    if p.exists()
    else {"replace": False, "values": []}
)
o["values"] = list(dict.fromkeys(o["values"] + ["timelocked:celestial_bark"]))
write(p, o)
print("Generated tree materials and eight modeled plant assets")
