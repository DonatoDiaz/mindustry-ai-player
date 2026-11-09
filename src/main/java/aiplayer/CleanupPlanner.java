package aiplayer;

import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.meta.BlockFlag;

import java.util.HashMap;
import java.util.Map;

public class CleanupPlanner {
    private final Map<Long, Float> unpowered = new HashMap<>();
    private float cooldown;
    private String state = "idle";

    public String state() {
        return state;
    }

    public void reset() {
        unpowered.clear();
        cooldown = 0f;
        state = "idle";
    }

    public void update(Team team, boolean enabled, ConstructionPlanner construction, DeconstructionPlanner demolition) {
        if (!enabled) {
            state = "off";
            return;
        }
        if (team == null || Vars.indexer == null) return;
        if (cooldown > 0f) {
            cooldown -= Time.delta;
            return;
        }
        cooldown = 10f;

        Seq<Building> turrets = Vars.indexer.getFlagged(team, BlockFlag.turret);
        int queued = 0;
        for (Building building : turrets) {
            if (building == null || building.tile == null) continue;
            if (!construction.isAiBuilt(building.tile.x, building.tile.y)) continue;

            boolean unpoweredNow = building.block.consumesPower
                && (building.power == null || building.power.status < 0.05f);
            long key = key(building.tile.x, building.tile.y);

            if (unpoweredNow) {
                float since = unpowered.containsKey(key) ? unpowered.get(key) : Time.time;
                if (!unpowered.containsKey(key)) unpowered.put(key, since);
                if (Time.time - since > 30f && queued < 2) {
                    if (demolition.queue(team, building.tile.x, building.tile.y)) {
                        queued++;
                        unpowered.remove(key);
                    }
                }
            } else {
                unpowered.remove(key);
            }
        }

        state = queued > 0 ? "demolish " + queued : "idle";
    }

    private long key(int x, int y) {
        return ((long) x << 32) ^ (y & 0xffffffffL);
    }
}
