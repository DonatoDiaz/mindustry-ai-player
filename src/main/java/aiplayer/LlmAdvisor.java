package aiplayer;

import arc.Core;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.type.Item;
import mindustry.world.meta.BlockFlag;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LlmAdvisor {
    private static final Pattern RESPONSE = Pattern.compile("\"response\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private volatile String role;
    private volatile String lastError = "";
    private volatile boolean pending;
    private float timer;

    public String advisedRole() {
        return role;
    }

    public boolean attackAllowed() {
        return role == null || "attack".equals(role);
    }

    public String status() {
        if (!AIPlayer.inst.config.llmEnabled) return "off";
        if (pending) return "thinking";
        if (!lastError.isEmpty()) return "error";
        return role == null ? "waiting" : role;
    }

    public String error() {
        return lastError;
    }

    public void interpretOrder(String order, String context, Consumer<String> callback) {
        String endpoint = AIPlayer.inst.config.llmEndpoint;
        if (!endpoint.contains("/api/")) endpoint = endpoint.replaceAll("/+$", "") + "/api/generate";
        String prompt = "You are an order parser for a Mindustry AI. Convert the player order into command lines.\n"
            + "Allowed commands:\nrole defense|power|production|attack|auto\nminers N\narmy N\nbuilders N\nbuild on|off\nattack on|off\nexpand on|off\nlaunch on|off\nname NAME\n"
            + "Output only command lines, one per line, no explanations. If no action is needed output idle.\n"
            + "Examples:\nпострой больше турелей -> role defense\nатакуй врага -> role attack\nattack on\nкопай ресурсы -> role production\nбольше шахтёров -> miners 6\n\n"
            + "State: " + (context == null ? "" : context) + "\nPlayer order: " + order;
        String body = "{\"model\":\"" + escape(AIPlayer.inst.config.llmModel)
            + "\",\"prompt\":\"" + escape(prompt)
            + "\",\"stream\":false,\"keep_alive\":\"10m\",\"options\":{\"temperature\":0.0,\"num_predict\":64,\"num_ctx\":2048}}";
        final String target = endpoint;
        Thread thread = new Thread(() -> requestText(target, body, callback), "AI-LLM-Order");
        thread.setDaemon(true);
        thread.start();
    }

    public void query(String question, String context, Consumer<String> callback) {
        if (question == null || question.trim().isEmpty()) {
            callback.accept("Пустой запрос");
            return;
        }
        String endpoint = AIPlayer.inst.config.llmEndpoint;
        if (!endpoint.contains("/api/")) endpoint = endpoint.replaceAll("/+$", "") + "/api/generate";
        String prompt = "Ты — AI-помощник внутри Mindustry. Отвечай кратко и по делу на языке вопроса.\n"
            + "Состояние игры: " + (context == null ? "" : context) + "\n"
            + "Знания:\n" + RagKnowledge.retrieve(question + " " + context, 3) + "\n"
            + "Вопрос игрока: " + question;
        String body = "{\"model\":\"" + escape(AIPlayer.inst.config.llmModel)
            + "\",\"prompt\":\"" + escape(prompt)
            + "\",\"stream\":false,\"keep_alive\":\"10m\",\"options\":{\"temperature\":0.3,\"num_predict\":160,\"num_ctx\":3072}}";
        final String target = endpoint;
        Thread thread = new Thread(() -> requestText(target, body, callback), "AI-LLM-Query");
        thread.setDaemon(true);
        thread.start();
    }

    private void requestText(String endpoint, String body, Consumer<String> callback) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                post(callback, "LLM error: HTTP " + code);
                return;
            }
            InputStream stream = connection.getInputStream();
            String response = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            String text = extract(response);
            post(callback, text == null || text.trim().isEmpty() ? "LLM вернул пустой ответ" : text.trim());
        } catch (Throwable t) {
            post(callback, "LLM error: " + t.getMessage());
            Log.warn("[AI] LLM query failed: @", t.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void post(Consumer<String> callback, String text) {
        Core.app.post(() -> callback.accept(text));
    }

    public void reset() {
        role = null;
        lastError = "";
        pending = false;
        timer = 0f;
    }

    public void update(Team team, Snapshot snapshot, UnitManager units, ConstructionPlanner construction) {
        if (!AIPlayer.inst.config.llmEnabled) {
            role = null;
            timer = 0f;
            return;
        }
        if (pending || snapshot == null || snapshot.core == null) return;

        timer += Time.delta;
        int interval = Math.max(5, AIPlayer.inst.config.llmIntervalSeconds);
        if (timer < interval) return;
        timer = 0f;

        startRequest(team, snapshot, units, construction);
    }

    public void requestNow(Team team, Snapshot snapshot, UnitManager units, ConstructionPlanner construction) {
        if (!AIPlayer.inst.config.llmEnabled || pending || snapshot == null || snapshot.core == null) return;
        timer = 0f;
        startRequest(team, snapshot, units, construction);
    }

    private void startRequest(Team team, Snapshot snapshot, UnitManager units, ConstructionPlanner construction) {
        String prompt = buildPrompt(team, snapshot, units, construction);
        String endpoint = AIPlayer.inst.config.llmEndpoint;
        if (!endpoint.contains("/api/")) endpoint = endpoint.replaceAll("/+$", "") + "/api/generate";

        String body = "{\"model\":\"" + escape(AIPlayer.inst.config.llmModel)
            + "\",\"prompt\":\"" + escape(prompt)
            + "\",\"stream\":false,\"keep_alive\":\"10m\",\"options\":{\"temperature\":0.1,\"num_predict\":12,\"num_ctx\":2048}}";

        pending = true;
        lastError = "";
        Log.info("[AI] LLM ask: @ (@)", AIPlayer.inst.config.llmModel, endpoint);
        final String target = endpoint;
        Thread thread = new Thread(() -> request(target, body), "AI-LLM");
        thread.setDaemon(true);
        try {
            thread.start();
        } catch (Throwable t) {
            pending = false;
            lastError = String.valueOf(t.getMessage());
        }
    }

    private void request(String endpoint, String body) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(30000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                lastError = "HTTP " + code;
                return;
            }

            InputStream stream = connection.getInputStream();
            String response = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            String chosen = normalize(extract(response));
            if (chosen == null) {
                lastError = "unparsed response";
            } else {
                role = chosen;
                lastError = "";
                Log.info("[AI] LLM role: @", role);
            }
        } catch (Throwable t) {
            lastError = String.valueOf(t.getMessage());
            Log.warn("[AI] LLM request failed: @", lastError);
        } finally {
            pending = false;
            if (connection != null) connection.disconnect();
        }
    }

    private String buildPrompt(Team team, Snapshot snapshot, UnitManager units, ConstructionPlanner construction) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the strategic brain of a fair Mindustry RTS AI. Choose exactly one role: economy, defense, power, attack, idle.\n");
        sb.append("Strict decision order:\n1) enemies > 0 or damaged buildings > 0 => defense\n2) generators == 0 or batteries < 2 => power\n3) core resources are low => economy\n4) army >= threshold and enemies == 0 => attack\n5) otherwise idle\n");
        sb.append("Examples: enemies=3 -> defense. no generator -> power. low copper and no enemies -> economy. army=8 and enemies=0 -> attack.\n");
        sb.append("If enemyCore=no or army is below attackThreshold, never choose attack.\n");
        sb.append("State: wave=").append(Vars.state.wave);
        sb.append(", enemies=").append(snapshot.enemies);
        int damaged = Vars.indexer == null ? 0 : Vars.indexer.getDamaged(team).size;
        int generators = Vars.indexer == null ? 0 : Vars.indexer.getFlagged(team, BlockFlag.generator).size;
        int batteries = Vars.indexer == null ? 0 : Vars.indexer.getFlagged(team, BlockFlag.battery).size;
        sb.append(", damaged=").append(damaged);
        sb.append(", generators=").append(generators);
        sb.append(", batteries=").append(batteries);
        sb.append(", units=").append(snapshot.unitCount).append('/').append(snapshot.unitCap);
        sb.append(", miners=").append(units.miners.size).append('/').append(AIPlayer.inst.config.maxMiners);
        sb.append(", army=").append(units.army.size).append('/').append(AIPlayer.inst.config.maxArmy);
        boolean enemyCore = Vars.state.teams.closestEnemyCore(snapshot.core.x, snapshot.core.y, team) != null;
        sb.append(", enemyCore=").append(enemyCore ? "yes" : "no");
        sb.append(", attackThreshold=").append(AIPlayer.inst.config.attackThreshold);
        sb.append(", builders=").append(units.builders.size).append('/').append(AIPlayer.inst.config.maxBuilders);
        sb.append(", sector=").append(Vars.state.isCampaign() && Vars.state.getSector() != null ? Vars.state.getSector().name() : "custom");
        sb.append(", currentRole=").append(construction.lastRole());
        sb.append("\nCore: ");
        int shown = 0;
        for (Item item : Vars.content.items()) {
            if (item == null) continue;
            int amount = snapshot.core.items.get(item);
            float flow = snapshot.core.items.getFlowRate(item);
            if (amount <= 0 && flow <= 0f) continue;
            if (shown++ >= 10) break;
            sb.append(item.name).append('=').append(amount).append('(').append(String.format(java.util.Locale.ROOT, "%+.1f", flow)).append(") ");
        }
        sb.append("\nAvailable roles: ");
        for (String value : new String[]{"production", "defense", "power", "attack"}) {
            if (construction.library.hasRole(value)) sb.append(value).append(' ');
        }
        sb.append("\nOres: ");
        if (Vars.indexer != null) {
            int ores = 0;
            for (Item item : Vars.indexer.getAllPresentOres()) {
                if (item == null) continue;
                if (ores++ >= 8) break;
                sb.append(item.name).append(' ');
            }
        }
        sb.append("\nLearned scores: ").append(AIPlayer.inst.learning.summary());
        String knowledge = RagKnowledge.retrieve(sb.toString(), 3);
        if (!knowledge.isEmpty()) sb.append("\nKnowledge:\n").append(knowledge);
        sb.append("\nAnswer with one word only.");
        return sb.toString();
    }

    private String extract(String body) {
        Matcher matcher = RESPONSE.matcher(body);
        if (!matcher.find()) return "";
        return unescape(matcher.group(1));
    }

    private String normalize(String text) {
        if (text == null) return null;
        String value = text.trim().toLowerCase();
        if (value.contains("defense") || value.contains("defence")) return "defense";
        if (value.contains("power") || value.contains("energy")) return "power";
        if (value.contains("attack") || value.contains("military")) return "attack";
        if (value.contains("economy") || value.contains("production") || value.contains("expand")) return "production";
        return null;
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private String unescape(String value) {
        return value.replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }
}
