import itertools
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont
from project_paths import font_path, preview_directory

r = Path(__file__).resolve().parents[1]
bs = json.loads((r / "tools/pantheris-model.json").read_text(encoding="utf-8"))
cols = [
    "24232d",
    "44414f",
    "7d252e",
    "777683",
    "5e3b7f",
    "e0b75c",
    "e85a77",
    "48272f",
    "e4dce8",
]
mats = {"root": np.eye(4)}
faces = []
for b in bs:
    ox, oy, oz, rx, ry, rz = b["pose"]
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    R = (
        np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
        @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
        @ np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    )
    M = np.eye(4)
    M[:3, :3] = R
    M[:3, 3] = [ox, oy, oz]
    M = mats[b["parent"]] @ M
    mats[b["name"]] = M
    x, y, z, w, h, d = b["box"]
    pts = np.array(
        [
            (M @ np.array([a, bb, c, 1]))[:3]
            for a, bb, c in itertools.product([x, x + w], [y, y + h], [z, z + d])
        ]
    )
    for ids in [
        [0, 1, 3, 2],
        [4, 6, 7, 5],
        [0, 4, 5, 1],
        [2, 3, 7, 6],
        [0, 2, 6, 4],
        [1, 5, 7, 3],
    ]:
        faces.append((pts[ids], cols[b["color"]]))
for label, yaw in [("front", -0.32), ("rear", 2.8)]:
    im = Image.new("RGB", (1000, 900), "#10131e")
    draw = ImageDraw.Draw(im)
    f = ImageFont.truetype(font_path(), 22)
    cy, sy = math.cos(yaw), math.sin(yaw)
    rx = -0.08
    rot = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    items = []
    for pts, col in faces:
        v = pts @ rot.T
        depth = v[:, 2].mean()
        norm = np.cross(v[1] - v[0], v[2] - v[0])
        norm /= np.linalg.norm(norm)
        shade = 0.65 + 0.35 * abs(np.dot(norm, np.array([0.2, -0.5, -0.84])))
        xy = [(500 + a * 21, 320 + b * 21 - c * 3) for a, b, c in v]
        rgb = tuple(int(v * shade) for v in bytes.fromhex(col))
        items.append((depth, xy, rgb))
    pixels = np.array(im)
    zbuffer = np.full((900, 1000), float("inf"))
    for pts, col in faces:
        v = pts @ rot.T
        screen = np.array([(500 + a * 21, 320 + b * 21 - c * 3) for a, b, c in v])
        norm = np.cross(v[1] - v[0], v[2] - v[0])
        norm /= np.linalg.norm(norm)
        shade = 0.65 + 0.35 * abs(np.dot(norm, np.array([0.2, -0.5, -0.84])))
        rgb = np.array([int(a * shade) for a in bytes.fromhex(col)], dtype="uint8")
        for ids in [[0, 1, 2], [0, 2, 3]]:
            t = screen[ids]
            zs = v[ids, 2]
            x0 = max(0, int(t[:, 0].min()))
            x1 = min(999, int(t[:, 0].max() + 1))
            y0 = max(0, int(t[:, 1].min()))
            y1 = min(899, int(t[:, 1].max() + 1))
            if x1 < x0 or y1 < y0:
                continue
            xx, yy = np.meshgrid(
                np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5
            )
            a, b, c = t
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(den) < 1e-9:
                continue
            u = ((b[1] - c[1]) * (xx - c[0]) + (c[0] - b[0]) * (yy - c[1])) / den
            vv = ((c[1] - a[1]) * (xx - c[0]) + (a[0] - c[0]) * (yy - c[1])) / den
            ww = 1 - u - vv
            depth = u * zs[0] + vv * zs[1] + ww * zs[2]
            zb = zbuffer[y0 : y1 + 1, x0 : x1 + 1]
            mask = (u >= 0) & (vv >= 0) & (ww >= 0) & (depth < zb)
            zb[mask] = depth[mask]
            pixels[y0 : y1 + 1, x0 : x1 + 1][mask] = rgb
    im = Image.fromarray(pixels)
    draw = ImageDraw.Draw(im)
    draw.text((32, 30), "PANTHERIS / seated boat cameo", fill="#e9e5ef", font=f)
    draw.text(
        (32, 68), "Reference-based voxel model • " + label, fill="#a9a2ba", font=f
    )
    draw.text(
        (32, 840),
        "Geometry preview only. Not an in-game screenshot.",
        fill="#a9a2ba",
        font=f,
    )
    im.save(preview_directory() / ("pantheris-" + label + ".png"))
