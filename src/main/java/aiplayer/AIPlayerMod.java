package aiplayer;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.mod.Mod;

public class AIPlayerMod extends Mod {
    @Override
    public void init() {
        Fi configFolder = getConfigFolder();
        if (configFolder == null) configFolder = Core.settings.getDataDirectory().child("mod-configs/ai-player");
        AIPlayer.inst.init(configFolder);

        Events.on(EventType.WorldLoadEvent.class, event -> {
            Log.info("[AI] world loaded");
            AIPlayer.inst.reset();
            AIPlayer.inst.loadSchematics();
            HudPanel.install();
        });
        Events.on(EventType.StateChangeEvent.class, event -> {
            if (Vars.state.isMenu()) AIPlayer.inst.reset();
        });
        Events.on(EventType.ClientLoadEvent.class, event -> HudPanel.install());
        Events.on(EventType.ResizeEvent.class, event -> HudPanel.reposition());
        Events.run(EventType.Trigger.update, AIPlayer.inst::update);

        Log.info("AI Player 0.9.6 loaded");
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.register("ai", "[action] [value]", "AI player control", args -> {
            String result = AICommandHandler.execute(args);
            if (result != null && !result.isEmpty()) message(result);
        });
    }

    public static void message(String text) {
        if (Vars.ui != null && Vars.ui.chatfrag != null) {
            Vars.ui.chatfrag.addMessage(text);
        } else {
            Log.info(text);
        }
    }
}
