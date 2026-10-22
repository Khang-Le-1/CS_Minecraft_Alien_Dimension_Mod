"""Optional audio authoring: intro ambience, or --anthem INPUT [--output DIR].

Not used by Gradle or Minecraft at runtime. FFmpeg is needed for either mode;
NumPy is imported only when regenerating intro ambience.
"""

import argparse
import json
import subprocess
import wave
from pathlib import Path

# Original PA sound-design graph: seconds, Hz and gain, kept unchanged.
FILTER = "[0:a]aformat=channel_layouts=mono,highpass=f=240,lowpass=f=4200,equalizer=f=1700:t=q:w=0.8:g=4,acompressor=threshold=0.1:ratio=3.2:attack=8:release=100:makeup=1.4,volume=1.35,asoftclip=type=atan:threshold=0.85,aecho=0.8:0.9:63|127|219:0.20|0.11|0.055,loudnorm=I=-18:TP=-2:LRA=8,aresample=48000,asplit=2[full][cold];[full]volume='min(t/3.2,1)':eval=frame[steady];[cold]highpass=f=480,lowpass=f=950,vibrato=f=4:d=0.18,tremolo=f=11:d=0.5,volume='(1-min(t/3.2,1))*(0.18+0.82*min(t/2,1))':eval=frame[start];anoisesrc=color=pink:sample_rate=48000:duration=3.2:amplitude=0.013,highpass=f=1000,lowpass=f=6000,tremolo=f=27:d=0.9,afade=t=out:st=0:d=3.2[hiss];[steady][start][hiss]amix=inputs=3:normalize=0:duration=longest,alimiter=limit=0.86:level=0[a]"


def process_anthem(input_path, output_dir):
    if not input_path.is_file():
        raise SystemExit("Input audio file does not exist")
    output_dir.mkdir(parents=True, exist_ok=True)
    ogg = output_dir / "nau_anthem_pa_startup.ogg"
    try:
        subprocess.run(
            [
                "ffmpeg",
                "-hide_banner",
                "-y",
                "-i",
                str(input_path),
                "-filter_complex",
                FILTER,
                "-map",
                "[a]",
                "-ac",
                "1",
                "-ar",
                "48000",
                "-c:a",
                "libvorbis",
                "-q:a",
                "5",
                str(ogg),
            ],
            check=True,
        )
        subprocess.run(
            [
                "ffmpeg",
                "-hide_banner",
                "-y",
                "-i",
                str(ogg),
                "-c:a",
                "libmp3lame",
                "-b:a",
                "160k",
                "-metadata",
                "title=NAU Anthem — PA startup mix",
                str(output_dir / "NAU_Anthem_PA_Startup.mp3"),
            ],
            check=True,
        )
    except FileNotFoundError:
        raise SystemExit("FFmpeg is required on PATH.") from None


def generate_intro():
    import numpy as np

    root = Path(__file__).resolve().parents[1] / "src/main/resources/assets/timelocked"
    (root / "sounds/intro").mkdir(parents=True, exist_ok=True)
    r = np.random.default_rng(88916)
    sr = 24000
    for name, dur in [("tv", 10), ("rain", 10), ("transition", 8), ("surf", 12)]:
        t = np.arange(int(sr * dur)) / sr
        noise = r.normal(0, 1, len(t))
        smooth = np.convolve(noise, np.ones(30) / 30, mode="same")
        if name == "tv":
            a = 0.045 * smooth + 0.008 * np.sin(2 * np.pi * 110 * t) * (
                1 + 0.3 * np.sin(t * 1.7)
            )
        elif name == "rain":
            a = 0.065 * noise + 0.14 * smooth
        elif name == "surf":
            a = (0.13 + 0.1 * np.sin(t * 0.65 - 0.8)) * (smooth * 0.9 + noise * 0.14)
        else:
            a = 0.07 * np.sin(2 * np.pi * 1050 * t) * np.exp(-t * 0.65) + (
                0.015 + 0.07 * np.clip((t - 2) / 5, 0, 1)
            ) * (smooth + noise * 0.15)
        a *= np.minimum(1, t / 0.3) * np.minimum(1, (dur - t) / 0.6)
        path = root / "sounds/intro" / (name + ".wav")
        with wave.open(str(path), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(sr)
            w.writeframes((np.clip(a, -0.8, 0.8) * 32767).astype("<i2").tobytes())
        subprocess.run(
            [
                "ffmpeg",
                "-v",
                "error",
                "-y",
                "-i",
                str(path),
                "-c:a",
                "libvorbis",
                "-q:a",
                "3",
                str(root / "sounds/intro" / (name + ".ogg")),
            ],
            check=True,
        )
        path.unlink()
    p = root / "sounds.json"
    d = json.load(open(p))
    for n in ["tv", "rain", "transition", "surf"]:
        d["intro_" + n] = {
            "sounds": [{"name": "timelocked:intro/" + n, "stream": False}],
            "subtitle": "subtitles.timelocked.intro_" + n,
        }
    p.write_text(json.dumps(d, indent=2) + "\n", encoding="utf-8")
    p = root / "lang/en_us.json"
    d = json.load(open(p))
    d.update(
        {
            "subtitles.timelocked.intro_" + n: s
            for n, s in [
                ("tv", "Television hums"),
                ("rain", "Rain against the windows"),
                ("transition", "Ringing fades into waves"),
                ("surf", "Waves wash ashore"),
            ]
        }
    )
    p.write_text(json.dumps(d, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--anthem", type=Path, help="Original anthem; skip ambience generation"
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "build/audio",
    )
    args = parser.parse_args()
    if args.anthem:
        process_anthem(args.anthem, args.output)
    else:
        generate_intro()


if __name__ == "__main__":
    main()
