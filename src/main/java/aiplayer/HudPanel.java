package aiplayer;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.ui.Styles;

public final class HudPanel {
    private static Table table;

    private HudPanel() {
    }

    public static void install() {
        if (Vars.ui == null || Vars.ui.hudGroup == null) return;

        if (table != null && table.parent == Vars.ui.hudGroup) {
            rebuild();
            return;
        }
        if (table != null) table.remove();

        table = new Table(Styles.black6);
        table.margin(6f);
        table.visible(() -> Vars.state.isPlaying() && AIPlayer.inst.config.showHud);
        rebuild();
        Vars.ui.hudGroup.addChild(table);
        reposition();
    }

    public static void rebuild() {
        if (table == null) return;

        table.clearChildren();

        if (AIPlayer.inst.config.hudCollapsed) {
            table.button("AI", () -> {
                AIPlayer.inst.config.hudCollapsed = false;
                AIPlayer.inst.config.save();
                rebuild();
            }).size(46f);
        } else {
            table.label(() -> AIPlayer.inst.status()).left().row();
            table.button("Calc", () -> AIPlayer.inst.printCalc()).size(60f, 36f);
            table.button("Setup", () -> new AISettingsDialog().show()).size(70f, 36f);
            table.button("Cmd", () -> new AICommandConsole().show()).size(60f, 36f);
            table.button("Hide", () -> {
                AIPlayer.inst.config.hudCollapsed = true;
                AIPlayer.inst.config.save();
                rebuild();
            }).size(50f, 36f);
            table.row();
            table.button("AI on/off", () -> {
                AIPlayer.inst.toggle();
                rebuild();
            }).size(240f, 36f);
        }

        table.pack();
        reposition();
    }

    public static void reposition() {
        if (table == null) return;
        float x = Core.graphics.getWidth() - table.getWidth() - 10f;
        float y = Core.graphics.getHeight() / 2f - table.getHeight() / 2f;
        table.setPosition(Math.max(10f, x), y);
    }
}
