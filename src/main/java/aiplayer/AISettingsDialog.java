package aiplayer;

import arc.scene.ui.Dialog;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import mindustry.ui.dialogs.BaseDialog;

public class AISettingsDialog extends BaseDialog {
    public AISettingsDialog() {
        super("AI Player Settings");
        shouldPause = false;
        addCloseButton();
    }

    @Override
    public Dialog show() {
        rebuild();
        return super.show();
    }

    private void rebuild() {
        AIConfig c = AIPlayer.inst.config;
        cont.clearChildren();
        cont.defaults().left().pad(4f);

        cont.add("[accent]AI Player[]").colspan(2).left().row();
        cont.check("Enabled", c.enabled, value -> AIPlayer.inst.setEnabled(value)).colspan(2).left().row();
        cont.check("AI-1 player", c.spawnAiPlayer, value -> AIPlayer.inst.setSpawnAiPlayer(value)).colspan(2).left().row();
        cont.check("Auto build", c.autoBuild, value -> AIPlayer.inst.setAutoBuild(value)).colspan(2).left().row();
        cont.check("Auto attack", c.autoAttack, value -> AIPlayer.inst.setAutoAttack(value)).colspan(2).left().row();
        cont.check("Attack schematics (advanced)", c.allowAttackSchematics, value -> AIPlayer.inst.setAllowAttackSchematics(value)).colspan(2).left().row();
        cont.check("Auto launch sectors", c.autoLaunch, value -> AIPlayer.inst.setAutoLaunch(value)).colspan(2).left().row();
        cont.check("Auto expand outposts", c.autoExpand, value -> AIPlayer.inst.setAutoExpand(value)).colspan(2).left().row();
        cont.check("Auto cleanup unpowered AI turrets", c.autoCleanup, value -> AIPlayer.inst.setAutoCleanup(value)).colspan(2).left().row();
        cont.check("LLM (PC local Ollama)", c.llmEnabled, value -> AIPlayer.inst.setLlmEnabled(value)).colspan(2).left().row();

        cont.add("LLM interval (seconds)").colspan(2).left().row();
        cont.slider(5f, 60f, 1f, c.llmIntervalSeconds, value -> AIPlayer.inst.setLlmInterval(Math.round(value))).width(360f).colspan(2).row();

        cont.add("Miners").left();
        cont.add("Attack squad threshold").left().row();
        cont.slider(0f, 20f, 1f, c.maxMiners, value -> AIPlayer.inst.setMaxMiners(Math.round(value))).width(180f);
        cont.slider(1f, 40f, 1f, c.attackThreshold, value -> AIPlayer.inst.setAttackThreshold(Math.round(value))).width(180f).row();

        cont.add("Army").left();
        cont.add("Builders").left().row();
        cont.slider(0f, 40f, 1f, c.maxArmy, value -> AIPlayer.inst.setMaxArmy(Math.round(value))).width(180f);
        cont.slider(0f, 12f, 1f, c.maxBuilders, value -> AIPlayer.inst.setMaxBuilders(Math.round(value))).width(180f).row();

        TextField model = new TextField(c.llmModel);
        model.setMessageText("qwen2.5:latest");
        cont.add("LLM model").left();
        cont.add(model).width(260f).left().row();
        cont.table(row -> {
            row.button("Apply model", () -> AIPlayer.inst.setLlmModel(model.getText())).size(150f, 38f);
            row.button("Ask LLM now", () -> {
                AIPlayer.inst.askLlmNow();
                rebuild();
            }).size(150f, 38f);
        }).colspan(2).row();

        TextField endpoint = new TextField(c.llmEndpoint);
        endpoint.setMessageText("http://127.0.0.1:11434");
        cont.add("LLM endpoint").left();
        cont.add(endpoint).width(260f).left().row();
        cont.button("Apply endpoint", () -> AIPlayer.inst.setLlmEndpoint(endpoint.getText())).colspan(2).size(160f, 38f).row();

        cont.labelWrap("Learned: " + AIPlayer.inst.learning.summary()
            + "\nLLM status: " + AIPlayer.inst.llm.status()
            + "\nRAG: " + AIPlayer.inst.ragCount() + " lines"
            + "\n" + (AIPlayer.inst.ragPath().isEmpty() ? "built-in knowledge" : AIPlayer.inst.ragPath())
            + "\nSchematics: " + AIPlayer.inst.construction.schematicCount()
            + "\n" + (AIPlayer.inst.construction.schematicPath().isEmpty() ? "game schematics" : AIPlayer.inst.construction.schematicPath()))
            .width(440f).left().colspan(2).row();

        cont.table(row -> {
            row.button("Reset learning", () -> {
                AIPlayer.inst.resetLearning();
                rebuild();
            }).size(150f, 38f);
            row.button("Commands", () -> new AICommandConsole().show()).size(140f, 38f);
            row.button("Reload schematics", () -> {
                AIPlayer.inst.reloadSchematics();
                rebuild();
            }).size(180f, 38f);
        }).colspan(2).row();
    }
}
