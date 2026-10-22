"""Read-only project checks; no server, audio conversion or asset generation.

Java formatting/compilation are separate Gradle checks. Binary assets are not text.
"""

import ast
import json
import re
import subprocess
import sys
from pathlib import Path

import tomllib
import yaml

ROOT = Path(__file__).resolve().parents[1]


def main():
    # Git supplies the authored file list; avoid .gradle, worlds and generated caches.
    names = (
        subprocess.check_output(
            ["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"],
            cwd=ROOT,
        )
        .decode("utf-8")
        .split("\0")
    )
    errors = []
    checked = 0
    for name in sorted(set(names)):
        path = ROOT / name
        if not name or not path.is_file():
            continue
        suffix = path.suffix.lower()
        if suffix not in {".json", ".toml", ".yaml", ".yml", ".py", ".mcmeta"}:
            continue
        try:
            text = path.read_text(encoding="utf-8")
            if suffix in {".json", ".mcmeta"}:
                json.loads(text)
            elif suffix == ".toml":
                # Templates use unquoted ${mod_id} in table headers. Validate after
                # substituting safe tokens; the template itself remains untouched.
                if "templates" in path.parts:
                    text = re.sub(r"\$\{[a-z_]+\}", "example", text)
                tomllib.loads(text)
            elif suffix in {".yaml", ".yml"}:
                yaml.safe_load(text)
            else:
                ast.parse(text, filename=name)
            checked += 1
        except (ValueError, SyntaxError, UnicodeError, yaml.YAMLError) as exc:
            errors.append(f"{name}: {exc}")
    for args in (["check", "tools"], ["format", "--check", "tools"]):
        if subprocess.run([sys.executable, "-m", "ruff", *args], cwd=ROOT).returncode:
            errors.append(f"ruff {' '.join(args)} failed")
    print(f"Validated {checked} structured-text/Python files; {len(errors)} errors")
    if errors:
        print("\n".join(errors))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
