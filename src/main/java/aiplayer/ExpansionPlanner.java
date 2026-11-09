package aiplayer;

import arc.math.Mathf;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

public class ExpansionPlanner {
    private boolean pending;
    private float cooldown;
    private String state = "off";

    public String state() {
        return state;
    }

    public void reset() {
        pending = false;
        cooldown = 0f;
        state = "off";
    }

    public void update(Team team, CoreBuild main, boolean enabled, BuildQueue queue) {
        if (!enabled) {
            state = "off";
            return;
        }
        if (team == null || main == null) {
            state = "waiting-core";
            return;
        }
        if (team.data().cores.size > 1) {
            pending = false;
            state = "outpost";
            return;
        }
        if (!Vars.state.rules.coreBuildAndConfig) {
            state = "core-build-disabled";
            return;
        }
        if (pending) {
            if (queue.size(team) == 0) {
                pending = false;
                state = "retry";
                cooldown = 10f;
            } else {
                state = "building";
            }
            return;
        }
        if (cooldown > 0f) {
            cooldown -= Time.delta;
            return;
        }
        if (main.items.total() < 600) {
            state = "waiting-resources";
            return;
        }

        Block core = bestCore(team, main);
        if (core == null) {
            state = "no-core-block";
            cooldown = 20f;
            return;
        }

        if (findAndQueue(team, main, core, queue)) {
            pending = true;
            state = "queued";
            cooldown = 20f;
            Log.info("[AI] outpost core queued: @", core.name);
        } else {
            state = "no-site";
            cooldown = 20f;
        }
    }

    private boolean findAndQueue(Team team, CoreBuild main, Block core, BuildQueue queue) {
        int width = Vars.world.width();
        int height = Vars.world.height();
        for (int i = 0; i < 80; i++) {
            int x = Mathf.random(8, Math.max(9, width - 8));
            int y = Mathf.random(8, Math.max(9, height - 8));
            Tile tile = Vars.world.tile(x, y);
            if (tile == null || tile.drop() == null) continue;

            float dst = Mathf.dst(main.tile.x, main.tile.y, x, y);
            if (dst < 30f || dst > 100f) continue;

            for (int ox = -2; ox <= 2; ox++) {
                for (int oy = -2; oy <= 2; oy++) {
                    int cx = x + ox;
                    int cy = y + oy;
                    if (Build.validPlace(core, team, cx, cy, 0)) {
                        if (queue.enqueue(team, main, core, cx, cy, 0, null)) return true;
                    }
                }
            }
        }
        return false;
    }

    private Block bestCore(Team team, CoreBuild main) {
        Block best = null;
        float bestCost = Float.MAX_VALUE;
        for (Block block : Vars.content.blocks()) {
            if (!(block instanceof CoreBlock) || block.size < 3 || !block.unlockedNow()) continue;
            if (Vars.state.rules.isBanned(block) || block.requirements == null) continue;
            if (!main.items.has(block.requirements, Vars.state.rules.buildCostMultiplier)) continue;
            boolean sustainable = true;
            for (ItemStack stack : block.requirements) {
                if (stack != null && stack.item != null && !EconomyAnalyzer.availableItem(stack.item, main)) sustainable = false;
            }
            if (!sustainable) continue;
            float cost = 0f;
            for (ItemStack stack : block.requirements) {
                if (stack != null) cost += stack.amount;
            }
            if (cost < bestCost) {
                bestCost = cost;
                best = block;
            }
        }
        return best;
    }
}
