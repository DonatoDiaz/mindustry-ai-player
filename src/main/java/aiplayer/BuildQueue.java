package aiplayer;

import mindustry.Vars;
import mindustry.game.Team;
import mindustry.game.Teams.BlockPlan;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

public class BuildQueue {
    public void clear(Team team) {
        if (team != null) team.data().plans.clear();
    }

    public int size(Team team) {
        return team == null ? 0 : team.data().plans.size;
    }

    public boolean enqueue(Team team, CoreBuild core, mindustry.world.Block block, int x, int y, int rotation, Object config) {
        if (team == null || core == null || block == null) return false;
        if (!Build.validPlace(block, team, x, y, rotation)) return false;
        if (block.requirements != null && !core.items.has(block.requirements, Vars.state.rules.buildCostMultiplier)) return false;

        for (BlockPlan plan : team.data().plans) {
            if (plan.x == x && plan.y == y && plan.block == block) return false;
        }

        team.data().plans.addLast(new BlockPlan(x, y, (short) rotation, block, config));
        return true;
    }

    public void prune(Team team) {
        if (team == null) return;

        boolean removed;
        do {
            removed = team.data().plans.remove(plan -> {
                if (plan == null || plan.removed) return true;

                Tile tile = Vars.world.tile(plan.x, plan.y);
                if (tile == null) return true;
                if (tile.block() == plan.block) return true;
                if (tile.build instanceof ConstructBuild build && build.current == plan.block) return false;

                return !Build.validPlace(plan.block, team, plan.x, plan.y, plan.rotation);
            });
        } while (removed);
    }
}
