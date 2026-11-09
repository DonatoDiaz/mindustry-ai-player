#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/build"
NAME="ai-player-mobile"

rm -rf "$OUT"
mkdir -p "$OUT/pkg/scripts"

cp "$ROOT/mod.json" "$OUT/pkg/mod.json"
cp "$ROOT/scripts/main.js" "$OUT/pkg/scripts/main.js"
if [ -f "$ROOT/icon.png" ]; then cp "$ROOT/icon.png" "$OUT/pkg/icon.png"; fi

(cd "$OUT/pkg" && zip -qr "../$NAME.zip" .)
echo "Built: $OUT/$NAME.zip"
