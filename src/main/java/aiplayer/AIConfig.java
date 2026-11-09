package aiplayer;

import arc.Core;

public class AIConfig {
    public boolean enabled = true;
    public int maxMiners = 4;
    public int maxArmy = 6;
    public int maxBuilders = 2;
    public boolean autoBuild = true;
    public boolean autoAttack = true;
    public boolean allowAttackSchematics = false;
    public int attackThreshold = 4;
    public boolean autoLaunch = false;
    public boolean autoExpand = true;
    public boolean autoCleanup = true;
    public boolean llmEnabled = false;
    public String llmEndpoint = "http://127.0.0.1:11434";
    public String llmModel = "qwen2.5-coder:1.5b";
    public int llmIntervalSeconds = 15;
    public boolean spawnAiPlayer = true;
    public String aiPlayerName = "AI-1";
    public boolean showHud = true;
    public boolean hudCollapsed = true;
    public boolean debug = false;

    public void load() {
        enabled = Core.settings.getBool("ai-player.enabled", true);
        maxMiners = Core.settings.getInt("ai-player.max-miners", 4);
        maxArmy = Core.settings.getInt("ai-player.max-army", 6);
        maxBuilders = Core.settings.getInt("ai-player.max-builders", 2);
        autoBuild = Core.settings.getBool("ai-player.auto-build", true);
        autoAttack = Core.settings.getBool("ai-player.auto-attack", true);
        allowAttackSchematics = Core.settings.getBool("ai-player.attack-schematics", false);
        attackThreshold = Core.settings.getInt("ai-player.attack-threshold", 4);
        autoLaunch = Core.settings.getBool("ai-player.auto-launch", false);
        autoExpand = Core.settings.getBool("ai-player.auto-expand", true);
        autoCleanup = Core.settings.getBool("ai-player.auto-cleanup", true);
        llmEnabled = Core.settings.getBool("ai-player.llm-enabled", false);
        llmEndpoint = Core.settings.getString("ai-player.llm-endpoint", "http://127.0.0.1:11434");
        llmModel = Core.settings.getString("ai-player.llm-model", "qwen2.5-coder:1.5b");
        llmIntervalSeconds = Core.settings.getInt("ai-player.llm-interval", 15);
        spawnAiPlayer = Core.settings.getBool("ai-player.spawn-ai-player", true);
        aiPlayerName = Core.settings.getString("ai-player.name", "AI-1");
        showHud = Core.settings.getBool("ai-player.show-hud", true);
        hudCollapsed = Core.settings.getBool("ai-player.hud-collapsed", true);
        debug = Core.settings.getBool("ai-player.debug", false);
        clamp();
    }

    public void save() {
        Core.settings.put("ai-player.enabled", enabled);
        Core.settings.put("ai-player.max-miners", maxMiners);
        Core.settings.put("ai-player.max-army", maxArmy);
        Core.settings.put("ai-player.max-builders", maxBuilders);
        Core.settings.put("ai-player.auto-build", autoBuild);
        Core.settings.put("ai-player.auto-attack", autoAttack);
        Core.settings.put("ai-player.attack-schematics", allowAttackSchematics);
        Core.settings.put("ai-player.attack-threshold", attackThreshold);
        Core.settings.put("ai-player.auto-launch", autoLaunch);
        Core.settings.put("ai-player.auto-expand", autoExpand);
        Core.settings.put("ai-player.auto-cleanup", autoCleanup);
        Core.settings.put("ai-player.llm-enabled", llmEnabled);
        Core.settings.put("ai-player.llm-endpoint", llmEndpoint);
        Core.settings.put("ai-player.llm-model", llmModel);
        Core.settings.put("ai-player.llm-interval", llmIntervalSeconds);
        Core.settings.put("ai-player.spawn-ai-player", spawnAiPlayer);
        Core.settings.put("ai-player.name", aiPlayerName);
        Core.settings.put("ai-player.show-hud", showHud);
        Core.settings.put("ai-player.hud-collapsed", hudCollapsed);
        Core.settings.put("ai-player.debug", debug);
    }

    public void clamp() {
        maxMiners = Math.max(0, Math.min(20, maxMiners));
        maxArmy = Math.max(0, Math.min(40, maxArmy));
        maxBuilders = Math.max(0, Math.min(12, maxBuilders));
        attackThreshold = Math.max(1, Math.min(40, attackThreshold));
        llmIntervalSeconds = Math.max(5, Math.min(300, llmIntervalSeconds));
        if (llmEndpoint == null || llmEndpoint.trim().isEmpty()) llmEndpoint = "http://127.0.0.1:11434";
        if (llmModel == null || llmModel.trim().isEmpty()) llmModel = "qwen2.5-coder:1.5b";
        if (aiPlayerName == null || aiPlayerName.trim().isEmpty()) aiPlayerName = "AI-1";
        if (aiPlayerName.length() > 24) aiPlayerName = aiPlayerName.substring(0, 24);
    }
}
