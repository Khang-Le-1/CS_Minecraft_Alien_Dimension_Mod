#!/usr/bin/env bash
# Optional Unix shortcut. Cross-platform command is documented in CONTRIBUTING.md.
set -euo pipefail
if (( $# == 0 )); then
  echo "Usage: $0 INPUT [--output DIR]" >&2
  exit 2
fi
exec python3 "$(dirname "$0")/generate_intro_audio.py" --anthem "$1" "${@:2}"
