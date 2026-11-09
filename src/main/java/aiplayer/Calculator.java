package aiplayer;

import java.util.Locale;

import mindustry.Vars;
import mindustry.game.Team;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.world.meta.BlockFlag;

public final class Calculator {
    private Calculator() {
    }

    public static String summary(Team team) {
        if (team == null) return "[scarlet]AI: no active team[]";
        CoreBuild core = team.core();
        if (core == null) return "[scarlet]AI: no core[]";

        StringBuilder sb = new StringBuilder();
        sb.append("[accent]AI resource calculator[]\n");
        sb.append("[lightgray]Total stored:[] ").append(core.items.total()).append("\n");
        sb.append("[lightgray]Deficits / low stock:[]");

        int shown = 0;
        for (Item item : Vars.content.items()) {
            if (item == null) continue;
            int amount = core.items.get(item);
            float flow = core.items.getFlowRate(item);
            if (amount >= 200 && flow >= -0.01f) continue;
            if (shown++ >= 12) {
                sb.append("\n  ...");
                break;
            }
            sb.append("\n  ").append(item.name).append(": ").append(amount);
            sb.append(" [gray](").append(String.format(Locale.ROOT, "%+.1f/s", flow)).append(")[]");
        }
        if (shown == 0) sb.append("\n  [green]all key resources are stocked[]");

        UnitType miner = ContentIndex.bestMiner(team, core);
        UnitType combat = ContentIndex.bestCombat(team, core);
        if (Vars.indexer != null) {
            int generators = Vars.indexer.getFlagged(team, BlockFlag.generator).size;
            int batteries = Vars.indexer.getFlagged(team, BlockFlag.battery).size;
            sb.append("\n[accent]Power:[] generators=").append(generators);
            sb.append(" batteries=").append(batteries);
            if (generators == 0) sb.append(" [yellow](no energy production)[]");
        }

        StringBuilder missing = new StringBuilder();
        for (Item item : Vars.content.items()) {
            if (item == null) continue;
            int amount = core.items.get(item);
            float flow = core.items.getFlowRate(item);
            boolean ore = Vars.indexer != null && (Vars.indexer.hasOre(item) || Vars.indexer.hasWallOre(item));
            if (amount > 0 && flow <= 0.01f && !ore) {
                if (missing.length() > 0) missing.append(", ");
                missing.append(item.name).append('(').append(amount).append(')');
            }
        }
        if (missing.length() > 0) sb.append("\n[yellow]No sustainable production:[] ").append(missing);

        sb.append("\n[accent]Next units:[]");
        sb.append("\n  miner: ").append(miner == null ? "none affordable" : miner.name + " (" + ContentIndex.costString(miner, team) + ")");
        sb.append("\n  army: ").append(combat == null ? "none affordable" : combat.name + " (" + ContentIndex.costString(combat, team) + ")");
        return sb.toString();
    }
}
