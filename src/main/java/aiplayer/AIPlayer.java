package aiplayer;

import arc.files.Fi;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ai.types.BuilderAI;
import mindustry.ai.types.CommandAI;
import mindustry.ai.types.MinerAI;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

import java.util.function.Consumer;

public final class AIPlayer {
    public static final AIPlayer inst = new AIPlayer();

    public final AIConfig config = new AIConfig();
    public final UnitManager units = new UnitManager();
    public final AICharacter character = new AICharacter();
    public final ConstructionPlanner construction = new ConstructionPlanner();
    public final SquadController squad = new SquadController();
    public final CampaignLauncher campaign = new CampaignLauncher();
    public final LlmAdvisor llm = new LlmAdvisor();
    public final LearningEngine learning = new LearningEngine();
    public final ExpansionPlanner expansion = new ExpansionPlanner();
    public final DeconstructionPlanner demolition = new DeconstructionPlanner();
    public final CleanupPlanner cleanup = new CleanupPlanner();

    private Team team;
    private Snapshot snapshot;
    private float timer;
    private Fi configFolder;
    private String forcedRole;
    private float forcedUntil;
    private boolean llmTestPending;

    private AIPlayer() {
    }

    public void init(Fi configFolder) {
        config.load();
        this.configFolder = configFolder;
        RagKnowledge.load(configFolder == null ? null : configFolder.child("rag"));
        learning.load(configFolder == null ? null : configFolder.child("learning.tsv"));
        if (configFolder != null) {
            Fi flag = configFolder.child("test-llm.flag");
            if (flag.exists()) {
                config.llmEnabled = true;
                config.save();
                llmTestPending = true;
                try {
                    flag.delete();
                } catch (Exception ignored) {
                }
                Log.info("[AI] LLM auto-test queued");
            }
        }
    }

    public void loadSchematics() {
        construction.load(configFolder == null ? null : configFolder.child("schematics"));
        RagKnowledge.load(configFolder == null ? null : configFolder.child("rag"));
        learning.load(configFolder == null ? null : configFolder.child("learning.tsv"));
    }

    public void reset() {
        forcedRole = null;
        forcedUntil = 0f;
        if (team != null) team.data().plans.clear();
        team = null;
        snapshot = null;
        timer = 0f;
        construction.reset();
        squad.reset();
        campaign.reset();
        llm.reset();
        learning.resetActive();
        expansion.reset();
        demolition.reset();
        cleanup.reset();
        character.remove();
        units.clear();
    }

    public Team team() {
        return team;
    }

    public String forcedRole() {
        if (forcedRole == null || Time.time > forcedUntil) return null;
        return forcedRole;
    }

    public void setForcedRole(String value) {
        if (value == null || value.equalsIgnoreCase("auto") || value.equalsIgnoreCase("none")) {
            forcedRole = null;
            forcedUntil = 0f;
        } else {
            String role = value.toLowerCase();
            if (role.equals("economy")) role = "production";
            forcedRole = role;
            forcedUntil = Time.time + 300f;
            if (role.equals("attack")) {
                config.autoAttack = true;
                config.save();
            }
        }
    }

    public String orderContext() {
        return "wave=" + Vars.state.wave
            + " enemies=" + (snapshot == null ? 0 : snapshot.enemies)
            + " miners=" + units.miners.size + "/" + config.maxMiners
            + " army=" + units.army.size + "/" + config.maxArmy
            + " builders=" + units.builders.size + "/" + config.maxBuilders
            + " currentRole=" + construction.lastRole()
            + " forcedRole=" + (forcedRole == null ? "none" : forcedRole);
    }

    public void interpretOrder(String order, Consumer<String> callback) {
        if (!config.llmEnabled) {
            callback.accept(AICommandHandler.applyOrder("", order));
            return;
        }
        llm.interpretOrder(order, orderContext(), raw -> callback.accept(AICommandHandler.applyOrder(raw, order)));
    }

    public void askLlm(String question, Consumer<String> callback) {
        if (!config.llmEnabled) {
            callback.accept("LLM выключен. Включи в Setup или командой /ai llm on");
            return;
        }
        llm.query(question, orderContext(), callback);
    }

    public void update() {
        if (!Vars.state.isPlaying() || Vars.state.isPaused() || Vars.net.client() || Vars.player == null) return;

        Team current = Vars.player.team();
        if (current == null) return;
        if (team != current) {
            team = current;
            units.clear();
            snapshot = null;
        }

        if (!config.enabled) {
            character.remove();
            return;
        }

        units.prune();
        demolition.update(team, units);
        cleanup.update(team, config.autoCleanup, construction, demolition);
        timer += Time.delta;
        if (timer < 1f) return;
        timer = 0f;

        snapshot = Snapshot.capture(team);
        if (snapshot.core == null) {
            if (config.debug) Log.info("[AI] no core for team @", team.id);
            return;
        }
        if (config.debug) Log.info("[AI] tick units=@/@ miners=@ army=@ aiPlayer=@ enemies=@", snapshot.unitCount, snapshot.unitCap, units.miners.size, units.army.size, character.alive(), snapshot.enemies);

        maintainMiners();
        maintainArmy();
        maintainBuilders();

        if (llmTestPending) {
            llmTestPending = false;
            llm.requestNow(team, snapshot, units, construction);
        }
        llm.update(team, snapshot, units, construction);
        squad.update(team, snapshot.core, units, snapshot, config.autoAttack && llm.attackAllowed());
        construction.update(team, snapshot.core, config.autoBuild, snapshot, units, llm.advisedRole());
        learning.update(snapshot.core, snapshot.enemies > 0, EconomyAnalyzer.needsPower(team));
        expansion.update(team, snapshot.core, config.autoExpand, construction.queue);
        campaign.update(team, config.autoLaunch);

        try {
            character.update(team, snapshot.core);
        } catch (Throwable t) {
            Log.err("[AI] AI player update failed", t);
        }
    }

    private void maintainMiners() {
        if (units.miners.size >= config.maxMiners) return;
        if (snapshot.unitCount >= snapshot.unitCap) return;

        UnitType type = ContentIndex.bestMiner(team, snapshot.core);
        if (type == null) return;

        Unit unit = units.spawn(type, team, snapshot.core, new MinerAI());
        if (unit != null) {
            units.miners.add(unit);
            if (config.debug) Log.info("[AI] miner: @", type.name);
        }
    }

    private void maintainArmy() {
        if (units.army.size >= config.maxArmy) return;
        if (snapshot.unitCount >= snapshot.unitCap) return;

        UnitType type = ContentIndex.bestCombat(team, snapshot.core);
        if (type == null) return;

        Unit unit = units.spawn(type, team, snapshot.core, new CommandAI());
        if (unit != null) {
            units.army.add(unit);
            if (config.debug) Log.info("[AI] army: @", type.name);
        }
    }

    private void maintainBuilders() {
        if (units.builders.size >= config.maxBuilders) return;
        if (snapshot.unitCount >= snapshot.unitCap) return;

        UnitType type = ContentIndex.bestBuilder(team, snapshot.core);
        if (type == null) return;

        Unit unit = units.spawn(type, team, snapshot.core, new BuilderAI());
        if (unit != null) {
            units.builders.add(unit);
            if (config.debug) Log.info("[AI] builder: @", type.name);
        }
    }

    public String status() {
        String state = config.enabled ? "[green]ON[]" : "[red]OFF[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[accent]AI Player[] ").append(state);
        sb.append("\nminers ").append(units.miners.size).append('/').append(config.maxMiners);
        sb.append("  army ").append(units.army.size).append('/').append(config.maxArmy);
        sb.append("\nbuilders ").append(units.builders.size).append('/').append(config.maxBuilders);
        sb.append("  plans ").append(construction.queueSize(team));
        sb.append("\nsquad ").append(squad.state());
        sb.append("  llm ").append(llm.status());
        sb.append("\nplayer ");
        if (!config.spawnAiPlayer) {
            sb.append("[gray]off[]");
        } else {
            sb.append(character.alive() ? "[green]alive[]" : "[yellow]respawning[]");
        }
        sb.append("\nexpand ").append(expansion.state());
        sb.append("  cleanup ").append(cleanup.state());
        if (forcedRole() != null) sb.append("\norder ").append(forcedRole());
        if (config.autoLaunch) sb.append("\nlaunch ").append(campaign.state());
        if (snapshot != null) {
            sb.append("\nenemies ").append(snapshot.enemies);
            sb.append("  units ").append(snapshot.unitCount).append('/').append(snapshot.unitCap);
        } else {
            sb.append("\n[gray]waiting for world[]");
        }
        return sb.toString();
    }

    public void toggle() {
        config.enabled = !config.enabled;
        config.save();
    }

    public void setEnabled(boolean value) {
        config.enabled = value;
        config.save();
    }

    public void reloadSchematics() {
        loadSchematics();
    }

    public void setMaxMiners(int value) {
        config.maxMiners = value;
        config.clamp();
        config.save();
    }

    public void setMaxArmy(int value) {
        config.maxArmy = value;
        config.clamp();
        config.save();
    }

    public void setMaxBuilders(int value) {
        config.maxBuilders = value;
        config.clamp();
        config.save();
    }

    public void setAutoBuild(boolean value) {
        config.autoBuild = value;
        config.save();
    }

    public void setAutoAttack(boolean value) {
        config.autoAttack = value;
        config.save();
    }

    public void setAllowAttackSchematics(boolean value) {
        config.allowAttackSchematics = value;
        config.save();
    }

    public void resetLearning() {
        learning.resetScores();
    }

    public boolean queueDemolish(int x, int y) {
        return demolition.queue(team, x, y);
    }

    public void setAttackThreshold(int value) {
        config.attackThreshold = value;
        config.clamp();
        config.save();
    }

    public void setAutoLaunch(boolean value) {
        config.autoLaunch = value;
        config.save();
    }

    public void setAutoExpand(boolean value) {
        config.autoExpand = value;
        config.save();
    }

    public void setAutoCleanup(boolean value) {
        config.autoCleanup = value;
        config.save();
    }

    public void setLlmEnabled(boolean value) {
        config.llmEnabled = value;
        config.save();
        if (!value) llm.reset();
    }

    public void setLlmModel(String value) {
        config.llmModel = value;
        config.clamp();
        config.save();
    }

    public void setLlmEndpoint(String value) {
        config.llmEndpoint = value;
        config.clamp();
        config.save();
    }

    public void setLlmInterval(int value) {
        config.llmIntervalSeconds = value;
        config.clamp();
        config.save();
    }

    public void askLlmNow() {
        Snapshot current = snapshot;
        if (current == null && team != null) current = Snapshot.capture(team);
        if (team == null || current == null || current.core == null) {
            AIPlayerMod.message("AI: no active world core");
            return;
        }
        llm.requestNow(team, current, units, construction);
    }

    public int ragCount() {
        return RagKnowledge.count();
    }

    public String ragPath() {
        return RagKnowledge.path();
    }

    public void setSpawnAiPlayer(boolean value) {
        config.spawnAiPlayer = value;
        config.save();
        if (!value) character.remove();
    }

    public void setAiPlayerName(String value) {
        config.aiPlayerName = value;
        config.clamp();
        config.save();
        character.rename(config.aiPlayerName);
    }

    public void toggleHud() {
        config.showHud = !config.showHud;
        config.save();
        HudPanel.rebuild();
    }

    public void toggleDebug() {
        config.debug = !config.debug;
        config.save();
    }

    public String calcText() {
        String text = Calculator.summary(team);
        if (team != null && team.core() != null) {
            text += "\n[accent]Build:[] plans=" + construction.queueSize(team)
                + " schematics=" + construction.schematicCount()
                + " role=" + construction.lastRole()
                + " next=" + construction.nextSchematicName(team, team.core())
                + "\n[accent]LLM:[] " + llm.status()
                + "  rag=" + RagKnowledge.count()
                + "  learned=" + learning.summary();
        }
        return text;
    }

    public void printCalc() {
        AIPlayerMod.message(calcText());
    }
}
