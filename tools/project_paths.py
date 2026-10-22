"""Shared, portable paths for optional asset-preview tools."""

import os
from pathlib import Path

from PIL import ImageFont

ROOT = Path(__file__).resolve().parents[1]
PREVIEWS = ROOT / "build/previews"


def preview_directory():
    PREVIEWS.mkdir(parents=True, exist_ok=True)
    return PREVIEWS


def font_path():
    """Use TIMELOCKED_FONT for reproducible artwork; otherwise try platform fonts.

    Different fonts change generated textures. Do not commit regenerated sign art
    unless that visual change is intentional. No fontconfig or Linux-only path required.
    """
    configured = os.environ.get("TIMELOCKED_FONT")
    if configured:
        path = Path(configured).expanduser()
        if not path.is_file():
            raise FileNotFoundError(f"TIMELOCKED_FONT does not exist: {path}")
        return str(path)
    for candidate in [
        "Arial.ttf",
        "arial.ttf",
        "DejaVuSans.ttf",
        "/System/Library/Fonts/Supplemental/Arial.ttf",
        str(Path(os.environ.get("WINDIR", "C:/Windows")) / "Fonts/arial.ttf"),
        "/usr/share/fonts/msttcore/arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]:
        try:
            return ImageFont.truetype(candidate, 20).path
        except OSError:
            continue
    raise FileNotFoundError(
        "Set TIMELOCKED_FONT to a TrueType font path before generating artwork"
    )
