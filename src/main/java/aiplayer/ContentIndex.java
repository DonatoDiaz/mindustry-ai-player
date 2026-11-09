package aiplayer;

import mindustry.Vars;
import mindustry.game.Team;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

public final class ContentIndex {
    private ContentIndex() {
    }

    public static UnitType bestMiner(Team team, CoreBuild core) {
        UnitType best = null;
        float bestScore = -1f;
        for (UnitType type : Vars.content.units()) {
            if (type == null || !type.unlockedNow() || type.mineSpeed <= 0f) continue;
            if (!type.mineFloor && !type.mineWalls) continue;
            if (Vars.state.rules.isBanned(type) || !canAfford(type, team, core)) continue;
            if (!hasMineableOre(type)) continue;
            float score = type.mineTier * 100f + type.mineSpeed * 10f - totalCost(type) * 0.05f;
            if (score > bestScore) {
                bestScore = score;
                best = type;
            }
        }
        return best;
    }

    private static boolean hasMineableOre(UnitType type) {
        if (Vars.indexer == null) return true;
        for (Item item : Vars.content.items()) {
            if (item == null) continue;
            if (!Vars.indexer.hasOre(item) && !Vars.indexer.hasWallOre(item)) continue;
            if (type.mineTier < item.hardness) continue;
            if (type.mineItems != null && !type.mineItems.contains(item)) continue;
            return true;
        }
        return false;
    }

    public static UnitType bestBuilder(Team team, CoreBuild core) {
        UnitType best = null;
        float bestScore = -1f;
        for (UnitType type : Vars.content.units()) {
            if (type == null || !type.unlockedNow() || type.buildSpeed <= 0f || type.buildRange <= 0f) continue;
            if (Vars.state.rules.isBanned(type) || !canAfford(type, team, core)) continue;
            float score = type.buildSpeed / Math.max(1f, totalCost(type));
            if (score > bestScore) {
                bestScore = score;
                best = type;
            }
        }
        return best;
    }

    public static UnitType bestCombat(Team team, CoreBuild core) {
        UnitType best = null;
        float bestScore = -1f;
        for (UnitType type : Vars.content.units()) {
            if (type == null || !type.unlockedNow() || !type.hasWeapons()) continue;
            if (Vars.state.rules.isBanned(type) || !canAfford(type, team, core)) continue;
            float score = type.dpsEstimate / Math.max(1f, totalCost(type));
            if (score > bestScore) {
                bestScore = score;
                best = type;
            }
        }
        return best;
    }

    public static boolean canAfford(UnitType type, Team team, CoreBuild core) {
        ItemStack[] req = requirements(type, team);
        if (req.length == 0 || core == null || !core.items.has(req)) return false;
        for (ItemStack stack : req) {
            if (stack != null && stack.item != null && !EconomyAnalyzer.availableItem(stack.item, core)) return false;
        }
        return true;
    }

    public static ItemStack[] requirements(UnitType type, Team team) {
        ItemStack[] req = type.getTotalRequirements();
        if (req == null || req.length == 0) return new ItemStack[0];
        return ItemStack.mult(req, Vars.state.rules.unitCost(team));
    }

    public static float totalCost(UnitType type) {
        ItemStack[] req = type.getTotalRequirements();
        if (req == null) return 0f;
        float total = 0f;
        for (ItemStack stack : req) {
            if (stack != null && stack.item != null) total += stack.amount;
        }
        return total;
    }

    public static String costString(UnitType type, Team team) {
        ItemStack[] req = requirements(type, team);
        if (req.length == 0) return "free";
        StringBuilder sb = new StringBuilder();
        for (ItemStack stack : req) {
            if (stack == null || stack.item == null || stack.amount <= 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(stack.item.name).append(" x").append(stack.amount);
        }
        return sb.toString();
    }
}
