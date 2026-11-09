# AI Player

Fair autonomous AI co-pilot for **Mindustry v8 (build 160)**.

The mod adds a second AI-piloted player, AI-1, and a co-pilot that mines,
builds, defends, attacks, and can be controlled with natural-language orders.

## Features

- AI-1 is a real `Player` entity with a name and respawning unit.
- Miners, builders, and army units are spawned with real resource costs.
- Dynamic support for Exogenesis and New Horizon content.
- Smart construction roles: economy, defense, power, and attack.
- Generated power, defense, and powered-turret schematics; your own `.msch`
  schematics are supported through role folders.
- Sustainable-resource checks: skips content that needs absent ore or
  production.
- Optional PC-only local LLM through Ollama, with RAG strategy files.
- Natural-language orders: `order построй больше турелей`, `order атакуй врага`.
- Compact collapsible HUD and an in-game AI command console.
- Self-learning schematic scoring and automatic cleanup of unpowered AI
  turrets.
- Outpost expansion and optional campaign sector auto-launch.

## Requirements

- Mindustry build 160 (desktop).
- Java 17 or newer for building.
- Optional: Ollama at `http://127.0.0.1:11434` for LLM strategy/orders.

## Install

### From the mod browser

The repository is tagged with the `mindustry-mod` topic. The browser list is
refreshed automatically.

### Manual

1. Build or download `ai-player.jar`.
2. Put it into the game's `mods` directory.
3. Restart Mindustry and enable **AI Player** in the mods menu.

The mod browser release is built as `ai-player.jar` (target `[v160]`).

## Build

```bash
./build.sh
```

By default the script compiles against the installed Steam desktop jar. Override
with:

```bash
MINDUSTRY_JAR=/path/to/desktop.jar ./build.sh
```

Outputs:

- `build/ai-player.zip`
- `build/ai-player.jar`

## Commands

- `/ai` — toggle AI
- `/ai status`, `/ai calc`
- `/ai miners N`, `/ai army N`, `/ai builders N`
- `/ai build on|off`, `/ai attack on|off`, `/ai expand on|off`
- `/ai order <приказ>` — natural-language order through LLM
- `/ai ask <вопрос>` — free-form LLM question
- `/ai demolish here`, `/ai demolish x y` — fair deconstruction
- `/ai llm on|off`, `/ai llm status`, `/ai llm now`
- `/ai setup` — settings dialog

## Mobile

`mobile/` contains a separate JS/JSON baseline for Android. It does not use a
local LLM. Build it with `mobile/build.sh`.

## License

MIT. See [LICENSE](LICENSE).
