import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

R = Path("src/main/resources")
A = R / "assets/timelocked"
D = R / "data/timelocked"


def write(p, v):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(v, indent=2), encoding="utf-8")


def cube(a, b, t):
    return {
        "from": a,
        "to": b,
        "faces": {
            f: {"texture": "#" + t}
            for f in ("north", "south", "east", "west", "up", "down")
        },
    }


roles = [
    ("herbalist_bench", "Herbalist’s Bench", (113, 170, 145), "minecraft:glass_bottle"),
    (
        "crystal_shaping_bench",
        "Crystal-shaping Bench",
        (135, 157, 207),
        "minecraft:amethyst_shard",
    ),
    ("cultivation_table", "Cultivation Table", (95, 153, 112), "minecraft:bone_meal"),
    ("celestial_hearth", "Celestial Hearth", (204, 139, 93), "minecraft:furnace"),
    ("attunement_altar", "Attunement Altar", (170, 143, 209), "minecraft:ender_pearl"),
    ("warden_desk", "Warden’s Desk", (105, 158, 190), "minecraft:compass"),
    (
        "celestial_medical_station",
        "Celestial Medical Station",
        (102, 171, 156),
        "timelocked:bandage",
    ),
]
lang = json.loads((A / "lang/en_us.json").read_text(encoding="utf-8"))
for i, (n, label, c, ingredient) in enumerate(roles):
    rng = random.Random(n)
    im = Image.new("RGB", (32, 32))
    px = im.load()
    for y in range(32):
        for x in range(32):
            v = rng.randint(-7, 7) + (8 if y % 8 == 0 else 0)
            px[x, y] = tuple(max(0, min(255, t + v)) for t in c)
    dr = ImageDraw.Draw(im)
    dr.rectangle((1, 1, 30, 30), outline=(213, 220, 213), width=2)
    if i == 6:
        dr.rectangle((13, 6, 18, 25), fill=(239, 243, 235))
        dr.rectangle((6, 13, 25, 18), fill=(239, 243, 235))
    elif i == 5:
        dr.rectangle((6, 6, 25, 25), fill=(206, 214, 184))
        dr.line((9, 21, 14, 11, 21, 18, 24, 9), fill=(80, 110, 122), width=2)
    elif i in (1, 4):
        dr.polygon(
            [(16, 5), (23, 16), (16, 27), (9, 16)],
            fill=(220, 230, 246),
            outline=(94, 90, 145),
        )
    elif i in (0, 2):
        dr.line((16, 26, 16, 7), fill=(219, 233, 199), width=2)
        for y in (11, 17, 23):
            dr.line((8, y - 4, 16, y, 23, y - 5), fill=(209, 232, 184), width=3)
    else:
        dr.ellipse((7, 7, 25, 25), outline=(250, 218, 153), width=3)
    p = A / f"textures/block/{n}.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    im.save(p)
    tex = {
        "wood": "timelocked:block/celestial_bark",
        "top": f"timelocked:block/{n}",
        "metal": "minecraft:block/iron_block",
        "accent": f"timelocked:block/{n}",
        "particle": f"timelocked:block/{n}",
    }
    els = [cube([1, 9, 1], [15, 11, 15], "top")]
    for x, z in [(2, 2), (11, 2), (2, 11), (11, 11)]:
        els.append(cube([x, 0, z], [x + 3, 9, z + 3], "wood"))
    if i == 0:
        els += [
            cube([2, 11, 2], [6, 13, 6], "accent"),
            cube([9, 11, 8], [13, 14, 12], "metal"),
            cube([3, 11, 9], [7, 12, 13], "accent"),
        ]
    elif i == 1:
        els += [
            cube([2, 11, 3], [5, 14, 12], "metal"),
            cube([11, 11, 3], [14, 14, 12], "metal"),
            cube([6, 11, 6], [10, 14, 10], "accent"),
        ]
    elif i == 2:
        els += [cube([2, 11, 2], [14, 12, 14], "wood")] + [
            cube([x, 12, z], [x + 2, 14, z + 2], "accent")
            for x in (3, 7, 11)
            for z in (3, 7, 11)
        ]
    elif i == 3:
        els = [
            cube([1, 0, 1], [15, 12, 15], "wood"),
            cube([2, 3, 0], [14, 9, 2], "metal"),
            cube([2, 12, 2], [14, 14, 14], "top"),
            cube([5, 14, 5], [11, 15, 11], "metal"),
        ]
    elif i == 4:
        els = [
            cube([2, 0, 2], [14, 3, 14], "metal"),
            cube([5, 3, 5], [11, 10, 11], "wood"),
            cube([1, 10, 1], [15, 12, 15], "top"),
            cube([6, 12, 6], [10, 15, 10], "accent"),
        ]
    elif i == 5:
        els += [
            cube([3, 11, 3], [13, 12, 13], "top"),
            cube([11, 11, 1], [14, 14, 4], "metal"),
        ]
    else:
        els += [
            cube([3, 11, 4], [13, 14, 12], "accent"),
            cube([2, 2, 2], [14, 4, 14], "metal"),
        ]
    write(
        A / f"models/block/{n}.json",
        {
            "textures": tex,
            "elements": els,
            "display": {
                "gui": {
                    "rotation": [30, 225, 0],
                    "translation": [0, 0, 0],
                    "scale": [0.8, 0.8, 0.8],
                }
            },
        },
    )
    write(
        A / f"blockstates/{n}.json",
        {"variants": {"": {"model": f"timelocked:block/{n}"}}},
    )
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
    write(
        D / f"recipe/{n}.json",
        {
            "type": "minecraft:crafting_shaped",
            "category": "misc",
            "pattern": [" C ", "PIP", "P P"],
            "key": {
                "C": {"item": "timelocked:void_crystal_cluster"},
                "P": {"tag": "minecraft:planks"},
                "I": {"item": ingredient},
            },
            "result": {"id": f"timelocked:{n}", "count": 1},
        },
    )
    lang["block.timelocked." + n] = label
write(A / "lang/en_us.json", lang)
p = R / "data/minecraft/tags/block/mineable/axe.json"
d = json.loads(p.read_text(encoding="utf-8"))
d["values"] = list(dict.fromkeys(d["values"] + ["timelocked:" + r[0] for r in roles]))
write(p, d)
print("Created seven distinct modeled workstations, recipes and loot")
