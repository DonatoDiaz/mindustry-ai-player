package aiplayer;

import arc.files.Fi;
import arc.util.Log;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

import java.util.HashMap;
import java.util.Map;

public class LearningEngine {
    private final Map<String, Float> scores = new HashMap<>();
    private final Map<String, Integer> attempts = new HashMap<>();
    private Fi file;
    private String active;
    private String activeRole;
    private float baseline;
    private float timer;

    public void load(Fi file) {
        this.file = file;
        scores.clear();
        attempts.clear();
        if (file == null || !file.exists()) return;
        try {
            for (String line : file.readString().split("\\R")) {
                String[] parts = line.split("\\t");
                if (parts.length < 2) continue;
                try {
                    scores.put(parts[0], Float.parseFloat(parts[1]));
                    attempts.put(parts[0], parts.length > 2 ? Integer.parseInt(parts[2]) : 0);
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception e) {
            Log.err("[AI] failed to load learning data", e);
        }
    }

    public float score(String name) {
        return scores.getOrDefault(name, 0f);
    }

    public String summary() {
        if (scores.isEmpty()) return "none";
        return scores.entrySet().stream()
            .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
            .limit(3)
            .map(e -> e.getKey() + ":" + String.format(java.util.Locale.ROOT, "%.1f", e.getValue()))
            .reduce((a, b) -> a + ", " + b)
            .orElse("none");
    }

    public void recordBuilt(String name, String role, CoreBuild core) {
        if (core == null) return;
        active = name;
        activeRole = role;
        baseline = core.items.total();
        timer = 0f;
    }

    public void update(CoreBuild core, boolean threat, boolean needsPower) {
        if (active == null || core == null) return;
        timer += 1f;
        if (timer < 30f) return;

        float delta = (core.items.total() - baseline) / 100f;
        float reward;
        if ("defense".equals(activeRole) || "attack".equals(activeRole)) {
            reward = threat ? 2f : -0.3f;
        } else if ("power".equals(activeRole)) {
            reward = (needsPower ? -0.3f : 1f) + delta / 200f;
        } else {
            reward = delta;
        }
        float old = score(active);
        scores.put(active, old * 0.8f + reward * 0.2f);
        attempts.merge(active, 1, Integer::sum);
        Log.info("[AI] learned @ (@): score=@", active, activeRole, String.format(java.util.Locale.ROOT, "%.2f", scores.get(active)));
        active = null;
        activeRole = null;
        timer = 0f;
        save();
    }

    public void resetScores() {
        scores.clear();
        attempts.clear();
        active = null;
        activeRole = null;
        timer = 0f;
        save();
    }

    public void resetActive() {
        active = null;
        timer = 0f;
    }

    private void save() {
        if (file == null) return;
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Float> entry : scores.entrySet()) {
                sb.append(entry.getKey()).append('\t')
                    .append(entry.getValue()).append('\t')
                    .append(attempts.getOrDefault(entry.getKey(), 0)).append('\n');
            }
            file.writeString(sb.toString());
        } catch (Exception e) {
            Log.err("[AI] failed to save learning data", e);
        }
    }
}
