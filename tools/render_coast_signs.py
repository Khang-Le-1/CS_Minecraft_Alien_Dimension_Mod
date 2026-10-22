import json
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont
from project_paths import font_path, preview_directory

r = Path(__file__).resolve().parents[1]
a = r / "src/main/resources/assets/timelocked"
out = preview_directory()
f = ImageFont.truetype(font_path(), 20)
# Native asset model preview, orthographic CPU rasterizer; no invented in-game capture.
for key in ["coastal_restriction_sign", "boardwalk_direction_sign"]:
    model = json.loads((a / "models/block" / f"{key}.json").read_text(encoding="utf-8"))
    for label, theta in [("front", 0.27), ("rear", math.pi + 0.27)]:
        W, H = 800, 900
        pixels = np.full((H, W, 3), [16, 20, 30], dtype=np.uint8)
        zb = np.full((H, W), np.inf)
        right = np.array([-math.cos(theta), 0, -math.sin(theta)])
        up = np.array(
            [-math.sin(theta) * 0.15, math.sqrt(1 - 0.15**2), math.cos(theta) * 0.15]
        )
        forward = np.cross(right, up) * -1
        for e in model["elements"]:
            x0, y0, z0 = e["from"]
            x1, y1, z1 = e["to"]
            facepoints = {
                "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
                "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
                "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
                "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
                "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
                "down": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
            }
            for face, xyz in facepoints.items():
                pts = np.array(xyz) - [8, 16, 8]
                screen = np.stack([400 + pts @ right * 18, 450 - pts @ up * 18], axis=1)
                zs = pts @ forward
                tex = (
                    np.asarray(
                        Image.open(a / "textures/block" / f"{key}.png").convert("RGB")
                    )
                    if e["faces"][face]["texture"] == "#front"
                    else np.full((16, 16, 3), [94, 104, 111], dtype=np.uint8)
                )
                shade = {
                    "north": 1,
                    "south": 0.72,
                    "east": 0.8,
                    "west": 0.7,
                    "up": 1.1,
                    "down": 0.6,
                }[face]
                uv = np.array([[0, 0], [1, 0], [1, 1], [0, 1]])
                for inds in [[0, 1, 2], [0, 2, 3]]:
                    t = screen[inds]
                    z = zs[inds]
                    xlo = max(0, int(t[:, 0].min()))
                    xhi = min(W - 1, int(t[:, 0].max() + 1))
                    ylo = max(0, int(t[:, 1].min()))
                    yhi = min(H - 1, int(t[:, 1].max() + 1))
                    xx, yy = np.meshgrid(
                        np.arange(xlo, xhi + 1) + 0.5, np.arange(ylo, yhi + 1) + 0.5
                    )
                    aa, b, c = t
                    den = (b[1] - c[1]) * (aa[0] - c[0]) + (c[0] - b[0]) * (
                        aa[1] - c[1]
                    )
                    if abs(den) < 1e-9:
                        continue
                    u = (
                        (b[1] - c[1]) * (xx - c[0]) + (c[0] - b[0]) * (yy - c[1])
                    ) / den
                    v = (
                        (c[1] - aa[1]) * (xx - c[0]) + (aa[0] - c[0]) * (yy - c[1])
                    ) / den
                    w = 1 - u - v
                    depth = u * z[0] + v * z[1] + w * z[2]
                    section = zb[ylo : yhi + 1, xlo : xhi + 1]
                    mask = (u >= 0) & (v >= 0) & (w >= 0) & (depth < section)
                    section[mask] = depth[mask]
                    coords = (
                        u[..., None] * uv[inds[0]]
                        + v[..., None] * uv[inds[1]]
                        + w[..., None] * uv[inds[2]]
                    )
                    tx = np.clip(
                        (coords[:, :, 0] * (tex.shape[1] - 1)).astype(int),
                        0,
                        tex.shape[1] - 1,
                    )
                    ty = np.clip(
                        (coords[:, :, 1] * (tex.shape[0] - 1)).astype(int),
                        0,
                        tex.shape[0] - 1,
                    )
                    rgb = np.clip(tex[ty, tx] * shade, 0, 255).astype("uint8")
                    pixels[ylo : yhi + 1, xlo : xhi + 1][mask] = rgb[mask]
        im = Image.fromarray(pixels)
        d = ImageDraw.Draw(im)
        d.text(
            (32, 30),
            "TIMELOCKED: Zero Point / modeled beach signs",
            font=f,
            fill="#e2eaf1",
        )
        d.text((32, 66), key.replace("_", " ") + " / " + label, font=f, fill="#a5b6c6")
        d.text(
            (32, 844),
            "Exact JSON geometry + texture preview. Not in-game.",
            font=f,
            fill="#a5b6c6",
        )
        im.save(out / f"{key}-{label}.png")
