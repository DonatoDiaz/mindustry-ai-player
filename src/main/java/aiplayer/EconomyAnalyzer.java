package aiplayer;

import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.game.Team;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.world.meta.BlockFlag;

public final class EconomyAnalyzer {
    private EconomyAnalyzer() {
    }

    public static boolean availableItem(Item item, CoreBuild core) {
        if (item == null) return false;
        if (core != null && core.items.getFlowRate(item) > 0.01f) return true;
        return Vars.indexer != null && (Vars.indexer.hasOre(item) || Vars.indexer.hasWallOre(item));
    }

    public static boolean available(Schematic schematic, CoreBuild core) {
        if (schematic == null) return false;
        for (ItemStack stack : schematic.requirements()) {
            if (stack == null || stack.item == null || stack.amount <= 0) continue;
            if (!availableItem(stack.item, core)) return false;
        }
        return true;
    }

    public static int deficits(CoreBuild core) {
        if (core == null) return 0;
        int count = 0;
        for (Item item : Vars.content.items()) {
            if (item == null) continue;
            if (core.items.get(item) < 100 && core.items.getFlowRate(item) <= 0.01f && availableItem(item, core)) {
                count++;
            }
        }
        return count;
    }

    public static boolean needsPower(Team team) {
        if (team == null || Vars.indexer == null) return false;
        int generators = Vars.indexer.getFlagged(team, BlockFlag.generator).size;
        int batteries = Vars.indexer.getFlagged(team, BlockFlag.battery).size;
        return generators == 0 || batteries < 2;
    }

    public static boolean underAttack(Team team, Snapshot snapshot) {
        if (snapshot != null && snapshot.enemies > 0) return true;
        if (team == null) return false;
        CoreBuild core = team.core();
        return core != null && core.healthf() < 0.8f;
    }

    public static String chooseRole(Team team, CoreBuild core, Snapshot snapshot, UnitManager units, SchematicLibrary library, String llmRole) {
        String forced = AIPlayer.inst.forcedRole();
        if (forced != null && library.hasRole(forced)) return forced;
        boolean threat = underAttack(team, snapshot);
        if (needsPower(team) && library.hasRole("power")) return "power";
        if (threat && library.hasRole("defense")) return "defense";
        if (llmRole != null && library.hasRole(llmRole)) {
            boolean attack = "attack".equals(llmRole);
            boolean defense = "defense".equals(llmRole);
            boolean attackSchematicBlocked = attack && !AIPlayer.inst.config.allowAttackSchematics;
            boolean hasTarget = core != null && Vars.state.teams.closestEnemyCore(core.x, core.y, team) != null;
            boolean strongEnough = units != null && units.army.size >= AIPlayer.inst.config.attackThreshold;
            if (defense && !threat) {
            } else if (!attackSchematicBlocked && (!attack || (!needsPower(team) && hasTarget && strongEnough))) {
                return llmRole;
            }
        }
        if (needsPower(team) && library.hasRole("power")) return "power";
        if (deficits(core) > 0 && library.hasRole("production")) return "production";
        if (units != null && units.army.size >= 4 && AIPlayer.inst.config.allowAttackSchematics && library.hasRole("attack")) return "attack";
        if (library.hasRole("defense")) return "defense";
        if (library.hasRole("production")) return "production";
        if (library.hasRole("power")) return "power";
        return null;
    }
}
