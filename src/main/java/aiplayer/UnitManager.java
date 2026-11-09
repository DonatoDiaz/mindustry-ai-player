package aiplayer;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.units.UnitController;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

public class UnitManager {
    public final Seq<Unit> miners = new Seq<>();
    public final Seq<Unit> army = new Seq<>();
    public final Seq<Unit> builders = new Seq<>();

    public void prune() {
        miners.removeAll(u -> u == null || !u.isAdded() || u.dead());
        army.removeAll(u -> u == null || !u.isAdded() || u.dead());
        builders.removeAll(u -> u == null || !u.isAdded() || u.dead());
    }

    public void clear() {
        miners.clear();
        army.clear();
        builders.clear();
    }

    public Unit spawn(UnitType type, Team team, CoreBuild core, UnitController controller) {
        ItemStack[] req = ContentIndex.requirements(type, team);
        if (req.length == 0 || core == null || !core.items.has(req)) return null;

        Unit unit = type.spawn(team, core.x, core.y);
        if (unit == null) return null;

        if (controller != null) unit.controller(controller);
        unit.add();
        Units.notifyUnitSpawn(unit);
        core.items.remove(req);
        return unit;
    }
}
