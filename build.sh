#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
GAME_DIR="${MINDUSTRY_DIR:-/mnt/w/Games/Steamgames/steamapps/common/Mindustry}"
MINDUSTRY_JAR="${MINDUSTRY_JAR:-$GAME_DIR/jre/desktop.jar}"
OUT="$ROOT/build"

if [ ! -f "$MINDUSTRY_JAR" ]; then
  echo "Mindustry jar not found: $MINDUSTRY_JAR" >&2
  echo "Set MINDUSTRY_JAR or MINDUSTRY_DIR." >&2
  exit 1
fi

rm -rf "$OUT/classes" "$OUT/ai-player.zip" "$OUT/ai-player.jar"
mkdir -p "$OUT/classes"

find "$ROOT/src/main/java" -name '*.java' -print > "$OUT/sources.txt"
javac --release 17 -encoding UTF-8 -cp "$MINDUSTRY_JAR" -d "$OUT/classes" @"$OUT/sources.txt"
cp "$ROOT/mod.json" "$OUT/classes/mod.json"
if [ -f "$ROOT/icon.png" ]; then cp "$ROOT/icon.png" "$OUT/classes/icon.png"; fi

if command -v zip >/dev/null 2>&1; then
  (cd "$OUT/classes" && zip -qr "$OUT/ai-player.zip" .)
else
  (cd "$OUT/classes" && jar --create --file "$OUT/ai-player.zip" .)
fi

cp "$OUT/ai-player.zip" "$OUT/ai-player.jar"

echo "Built: $OUT/ai-player.zip"
echo "Built: $OUT/ai-player.jar"
