package aiplayer;

import mindustry.Vars;
import mindustry.game.Gamemode;
import mindustry.maps.Map;

import java.util.Locale;
import java.util.function.Consumer;

public final class AICommandHandler {
    private AICommandHandler() {
    }

    public static String execute(String raw) {
        return execute(raw, null);
    }

    public static String execute(String raw, Consumer<String> output) {
        if (raw == null) return "";
        String value = raw.trim();
        if (value.startsWith("/ai")) value = value.substring(3).trim();
        else if (value.startsWith("ai ")) value = value.substring(2).trim();
        if (value.isEmpty()) return execute(new String[0], output);
        return execute(value.split("\\s+"), output);
    }

    public static String execute(String[] args) {
        return execute(args, null);
    }

    public static String execute(String[] args, Consumer<String> output) {
        if (args == null || args.length == 0) {
            AIPlayer.inst.toggle();
            return "AI: " + (AIPlayer.inst.config.enabled ? "ON" : "OFF");
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "on":
                AIPlayer.inst.setEnabled(true);
                return "AI: ON";
            case "off":
                AIPlayer.inst.setEnabled(false);
                return "AI: OFF";
            case "status":
                return AIPlayer.inst.status();
            case "calc":
                return AIPlayer.inst.calcText();
            case "role":
                if (args.length < 2) return "Usage: /ai role defense|power|production|attack|auto";
                AIPlayer.inst.setForcedRole(args[1]);
                return "AI role: " + (AIPlayer.inst.forcedRole() == null ? "auto" : AIPlayer.inst.forcedRole());
            case "miners":
                if (args.length < 2) return "Usage: /ai miners <0-20>";
                try {
                    AIPlayer.inst.setMaxMiners(Integer.parseInt(args[1]));
                    return "AI miners: " + AIPlayer.inst.config.maxMiners;
                } catch (NumberFormatException e) {
                    return "Invalid number";
                }
            case "army":
                if (args.length < 2) return "Usage: /ai army <0-40>";
                try {
                    AIPlayer.inst.setMaxArmy(Integer.parseInt(args[1]));
                    return "AI army: " + AIPlayer.inst.config.maxArmy;
                } catch (NumberFormatException e) {
                    return "Invalid number";
                }
            case "builders":
                if (args.length < 2) return "Usage: /ai builders <0-12>";
                try {
                    AIPlayer.inst.setMaxBuilders(Integer.parseInt(args[1]));
                    return "AI builders: " + AIPlayer.inst.config.maxBuilders;
                } catch (NumberFormatException e) {
                    return "Invalid number";
                }
            case "build":
                if (args.length < 2) AIPlayer.inst.setAutoBuild(!AIPlayer.inst.config.autoBuild);
                else AIPlayer.inst.setAutoBuild(args[1].equalsIgnoreCase("on"));
                return "AI build: " + (AIPlayer.inst.config.autoBuild ? "ON" : "OFF");
            case "learning":
                if (args.length > 1 && args[1].equalsIgnoreCase("reset")) {
                    AIPlayer.inst.resetLearning();
                    return "AI learning reset";
                }
                return "Usage: /ai learning reset";
            case "attackschematics":
                if (args.length < 2) AIPlayer.inst.setAllowAttackSchematics(!AIPlayer.inst.config.allowAttackSchematics);
                else AIPlayer.inst.setAllowAttackSchematics(args[1].equalsIgnoreCase("on"));
                return "AI attack schematics: " + (AIPlayer.inst.config.allowAttackSchematics ? "ON" : "OFF");
            case "attack":
                if (args.length < 2) AIPlayer.inst.setAutoAttack(!AIPlayer.inst.config.autoAttack);
                else AIPlayer.inst.setAutoAttack(args[1].equalsIgnoreCase("on"));
                return "AI attack: " + (AIPlayer.inst.config.autoAttack ? "ON" : "OFF");
            case "threshold":
                if (args.length < 2) return "Usage: /ai threshold <1-40>";
                try {
                    AIPlayer.inst.setAttackThreshold(Integer.parseInt(args[1]));
                    return "AI attack threshold: " + AIPlayer.inst.config.attackThreshold;
                } catch (NumberFormatException e) {
                    return "Invalid number";
                }
            case "cleanup":
                if (args.length < 2) AIPlayer.inst.setAutoCleanup(!AIPlayer.inst.config.autoCleanup);
                else AIPlayer.inst.setAutoCleanup(args[1].equalsIgnoreCase("on"));
                return "AI cleanup: " + (AIPlayer.inst.config.autoCleanup ? "ON" : "OFF");
            case "expand":
                if (args.length < 2) AIPlayer.inst.setAutoExpand(!AIPlayer.inst.config.autoExpand);
                else AIPlayer.inst.setAutoExpand(args[1].equalsIgnoreCase("on"));
                return "AI outpost expansion: " + (AIPlayer.inst.config.autoExpand ? "ON" : "OFF");
            case "launch":
                if (args.length < 2) AIPlayer.inst.setAutoLaunch(!AIPlayer.inst.config.autoLaunch);
                else AIPlayer.inst.setAutoLaunch(args[1].equalsIgnoreCase("on"));
                return "AI campaign launch: " + (AIPlayer.inst.config.autoLaunch ? "ON" : "OFF");
            case "player":
                if (args.length < 2) AIPlayer.inst.setSpawnAiPlayer(!AIPlayer.inst.config.spawnAiPlayer);
                else AIPlayer.inst.setSpawnAiPlayer(args[1].equalsIgnoreCase("on"));
                return "AI player: " + (AIPlayer.inst.config.spawnAiPlayer ? "ON" : "OFF");
            case "name":
                if (args.length < 2) return "Usage: /ai name <name>";
                AIPlayer.inst.setAiPlayerName(args[1]);
                return "AI player name: " + AIPlayer.inst.config.aiPlayerName;
            case "llm":
                return llm(args);
            case "ask":
                if (args.length < 2) return "Usage: /ai ask <вопрос>";
                AIPlayer.inst.askLlm(join(args, 1), answer -> respond(output, answer));
                return "AI: thinking...";
            case "order":
                if (args.length < 2) return "Usage: /ai order <приказ>";
                AIPlayer.inst.interpretOrder(join(args, 1), result -> respond(output, "[AI order] " + result));
                return "AI: interpreting order...";
            case "demolish":
                if (args.length < 2) return "Usage: /ai demolish <x> <y>|here";
                if (args[1].equalsIgnoreCase("here")) {
                    if (Vars.player == null) return "No local player";
                    int x = mindustry.core.World.toTile(Vars.player.mouseX);
                    int y = mindustry.core.World.toTile(Vars.player.mouseY);
                    return AIPlayer.inst.queueDemolish(x, y) ? "Demolish queued: " + x + "," + y : "Cannot demolish here";
                }
                if (args.length < 3) return "Usage: /ai demolish <x> <y>|here";
                try {
                    int x = Integer.parseInt(args[1]);
                    int y = Integer.parseInt(args[2]);
                    return AIPlayer.inst.queueDemolish(x, y) ? "Demolish queued: " + x + "," + y : "Cannot demolish there";
                } catch (NumberFormatException e) {
                    return "Invalid coordinates";
                }
            case "schematics":
                return "AI schematics: " + AIPlayer.inst.construction.schematicCount()
                    + "\npath: " + (AIPlayer.inst.construction.schematicPath().isEmpty() ? "game schematics" : AIPlayer.inst.construction.schematicPath());
            case "hud":
                AIPlayer.inst.toggleHud();
                return "AI HUD: " + (AIPlayer.inst.config.showHud ? "ON" : "OFF");
            case "debug":
                AIPlayer.inst.toggleDebug();
                return "AI debug: " + (AIPlayer.inst.config.debug ? "ON" : "OFF");
            case "setup":
                new AISettingsDialog().show();
                return "AI settings opened";
            case "chat":
            case "cmd":
                new AICommandConsole().show();
                return "";
            case "testmap":
                if (!Vars.state.isMenu()) return "Test map can only be started from the menu";
                Map map = args.length > 1 ? Vars.maps.byName(args[1]) : Vars.maps.defaultMaps().first();
                if (map == null) return "Map not found";
                Vars.control.playMap(map, map.applyRules(Gamemode.survival));
                return "Starting: " + map.name();
            default:
                return "Unknown command. Use /ai order <приказ> or /ai ask <вопрос>. Type /ai for usage.";
        }
    }

    public static String applyOrder(String raw, String original) {
        StringBuilder applied = new StringBuilder();
        if (raw != null) {
            for (String line : raw.split("\\R")) {
                String clean = line.trim();
                if (clean.isEmpty() || clean.equalsIgnoreCase("idle")) continue;
                String lower = clean.toLowerCase(Locale.ROOT);
                if (lower.equals("attack")) clean = "attack on";
                else if (lower.equals("build")) clean = "build on";
                else if (lower.equals("expand")) clean = "expand on";
                else if (lower.equals("launch")) clean = "launch on";
                String[] args = clean.split("\\s+");
                if (!allowedOrder(args[0])) continue;
                String result = execute(args);
                if (result.startsWith("Usage") || result.startsWith("Unknown")) continue;
                if (applied.length() > 0) applied.append("; ");
                applied.append(clean);
            }
        }
        if (applied.length() == 0) {
            String fallback = keywordOrder(original);
            if (fallback != null) {
                String[] args = fallback.split("\\s+");
                String result = execute(args);
                if (!result.startsWith("Usage")) applied.append(fallback).append(" (").append(result).append(')');
            }
        }
        return applied.length() == 0 ? "Не понял приказ" : applied.toString();
    }

    private static boolean allowedOrder(String action) {
        String value = action.toLowerCase(Locale.ROOT);
        return value.equals("role") || value.equals("miners") || value.equals("army") || value.equals("builders")
            || value.equals("build") || value.equals("attack") || value.equals("expand") || value.equals("launch")
            || value.equals("name") || value.equals("threshold") || value.equals("attackschematics");
    }

    private static String keywordOrder(String original) {
        if (original == null) return null;
        String text = original.toLowerCase(Locale.ROOT);
        if (text.contains("оборон") || text.contains("защит") || text.contains("турел") || text.contains("defen")) return "role defense";
        if (text.contains("атак") || text.contains("attack")) return "role attack";
        if (text.contains("энерг") || text.contains("power") || text.contains("генератор")) return "role power";
        if (text.contains("эконом") || text.contains("копай") || text.contains("ресурс") || text.contains("шахт") || text.contains("производ")) return "role production";
        if (text.contains("аутпост") || text.contains("экспан") || text.contains("expand")) return "expand on";
        if (text.contains("запуск") || text.contains("сектор") || text.contains("launch")) return "launch on";
        return null;
    }

    private static void respond(Consumer<String> output, String text) {
        if (output != null) output.accept(text);
        else AIPlayerMod.message(text);
    }

    private static String join(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private static String llm(String[] args) {
        if (args.length < 2) {
            AIPlayer.inst.setLlmEnabled(!AIPlayer.inst.config.llmEnabled);
            return "AI LLM: " + (AIPlayer.inst.config.llmEnabled ? "ON" : "OFF");
        }
        if (args[1].equalsIgnoreCase("status")) {
            return "AI LLM: " + AIPlayer.inst.llm.status()
                + "\nmodel: " + AIPlayer.inst.config.llmModel
                + "\nendpoint: " + AIPlayer.inst.config.llmEndpoint
                + "\nRAG: " + AIPlayer.inst.ragCount() + " lines"
                + (AIPlayer.inst.llm.error().isEmpty() ? "" : "\nerror: " + AIPlayer.inst.llm.error());
        }
        if (args[1].equalsIgnoreCase("now")) {
            AIPlayer.inst.askLlmNow();
            return "AI LLM: request sent";
        }
        if (args[1].equalsIgnoreCase("interval")) {
            if (args.length < 3) return "Usage: /ai llm interval <5-300>";
            try {
                AIPlayer.inst.setLlmInterval(Integer.parseInt(args[2]));
                return "AI LLM interval: " + AIPlayer.inst.config.llmIntervalSeconds + "s";
            } catch (NumberFormatException e) {
                return "Invalid number";
            }
        }
        if (args[1].equalsIgnoreCase("endpoint")) {
            if (args.length < 3) return "Usage: /ai llm endpoint URL";
            AIPlayer.inst.setLlmEndpoint(args[2]);
            return "AI LLM endpoint: " + AIPlayer.inst.config.llmEndpoint;
        }
        if (args[1].equalsIgnoreCase("model")) {
            if (args.length < 3) return "Usage: /ai llm model NAME";
            AIPlayer.inst.setLlmModel(args[2]);
            return "AI LLM model: " + AIPlayer.inst.config.llmModel;
        }
        AIPlayer.inst.setLlmEnabled(args[1].equalsIgnoreCase("on"));
        return "AI LLM: " + (AIPlayer.inst.config.llmEnabled ? "ON" : "OFF");
    }
}
