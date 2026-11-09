package aiplayer;

import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.type.Sector;

public class CampaignLauncher {
    private float cooldown;
    private String state = "off";

    public String state() {
        return state;
    }

    public void reset() {
        cooldown = 0f;
        state = "off";
    }

    public void update(Team team, boolean enabled) {
        if (!enabled) {
            state = "off";
            return;
        }
        if (!Vars.state.isCampaign() || Vars.state.getSector() == null) {
            state = "not-campaign";
            return;
        }

        Sector current = Vars.state.getSector();
        if (!current.isCaptured()) {
            state = "waiting-capture";
            return;
        }
        if (cooldown > 0f) {
            cooldown -= Time.delta;
            return;
        }

        Sector target = null;
        for (Sector next : current.near()) {
            if (next == null || next == current || next.isCaptured() || next.locked() || next.isAttacked() || next.isBeingPlayed()) continue;
            target = next;
            break;
        }

        if (target == null) {
            state = "no-target";
            return;
        }

        state = "launching " + target.name();
        Log.info("[AI] auto-launch @ -> @", current.name(), target.name());
        Vars.control.playSector(target, current);
        cooldown = 30f;
    }
}
