package aiplayer;

import arc.math.geom.Vec2;
import mindustry.Vars;
import mindustry.ai.types.CommandAI;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

public class SquadController {
    private final Vec2 rally = new Vec2();
    private String state = "rally";

    public String state() {
        return state;
    }

    public void reset() {
        state = "rally";
    }

    public void update(Team team, CoreBuild core, UnitManager units, Snapshot snapshot, boolean autoAttack) {
        if (team == null || core == null || units == null) return;

        Teamc target = null;
        String mode = "rally";

        if (snapshot != null && snapshot.enemies > 0) {
            Unit enemy = Units.closestEnemy(team, core.x, core.y, 900f, unit -> unit != null && !unit.dead());
            if (enemy != null) {
                target = enemy;
                mode = "defend";
            }
        }

        if (target == null && autoAttack && units.army.size >= AIPlayer.inst.config.attackThreshold) {
            CoreBuild enemyCore = Vars.state.teams.closestEnemyCore(core.x, core.y, team);
            if (enemyCore != null) {
                target = enemyCore;
                mode = "attack";
            }
        }

        state = mode;

        if (target != null) {
            for (Unit unit : units.army) {
                if (!valid(unit)) continue;
                CommandAI ai = unit.command();
                if (ai == null) continue;
                if (ai.attackTarget != target) {
                    ai.clearCommands();
                    ai.commandTarget(target);
                }
            }
        } else {
            rally.set(core.x, core.y + 80f);
            for (Unit unit : units.army) {
                if (!valid(unit)) continue;
                CommandAI ai = unit.command();
                if (ai != null && !ai.hasCommand()) ai.commandPosition(rally);
            }
        }
    }

    private boolean valid(Unit unit) {
        return unit != null && unit.isAdded() && !unit.dead() && unit.isCommandable() && unit.controller() instanceof CommandAI;
    }
}
