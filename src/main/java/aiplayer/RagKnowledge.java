package aiplayer;

import arc.files.Fi;
import arc.struct.Seq;

import java.util.Locale;

public final class RagKnowledge {
    private static final Seq<String> lines = new Seq<>();
    private static String path = "";

    private static final String BUILTIN =
        "Defense: if enemies are near the core, a wave is active, or buildings are damaged, prefer defense.\n"
        + "Defense: walls and turrets should stay near the core build radius; do not place them on the frontline without support.\n"
        + "Power: no generator or fewer than two batteries means power is unstable; build solar, battery and power nodes.\n"
        + "Economy: low core items with positive ore availability means economy; keep miners alive and prefer mineable ores.\n"
        + "Campaign: sectors can lack resources; never require an item that has no ore, no flow and no stock.\n"
        + "Attack: attack only when the army is at or above the configured threshold and an enemy core exists.\n"
        + "Mods: Exogenesis and New Horizon add units and ores; always select content dynamically by unlock, cost and availability.\n"
        + "LLM: choose from economy, defense, power, attack, idle; return one word only.\n";

    private RagKnowledge() {
    }

    public static void load(Fi folder) {
        lines.clear();
        add(BUILTIN);
        path = "";
        if (folder == null) return;

        folder.mkdirs();
        path = folder.toString();
        for (Fi file : folder.list()) {
            if (!file.extEquals("md") && !file.extEquals("txt")) continue;
            try {
                add(file.readString());
            } catch (Exception ignored) {
            }
        }
    }

    private static void add(String text) {
        if (text == null) return;
        for (String line : text.split("\\R")) {
            String clean = line.trim();
            if (clean.length() >= 8 && clean.length() <= 500) lines.add(clean);
        }
    }

    public static String retrieve(String context, int max) {
        if (lines.isEmpty()) return "";
        String query = context == null ? "" : context.toLowerCase(Locale.ROOT);
        String[] tokens = query.split("[^a-z0-9а-я]+");
        Seq<String> best = new Seq<>();
        Seq<Integer> scores = new Seq<>();
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            int score = 0;
            for (String token : tokens) {
                if (token.length() >= 3 && lower.contains(token)) score++;
            }
            int index = -1;
            for (int i = 0; i < scores.size; i++) {
                if (score > scores.get(i)) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                best.insert(index, line);
                scores.insert(index, score);
            } else if (best.size < max) {
                best.add(line);
                scores.add(score);
            }
            while (best.size > max) {
                best.remove(best.size - 1);
                scores.remove(scores.size - 1);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String line : best) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
        }
        return sb.toString();
    }

    public static int count() {
        return lines.size;
    }

    public static String path() {
        return path;
    }
}
