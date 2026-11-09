package aiplayer;

import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.world.Build;

public class DeconstructionPlanner {
    private final Seq<Point2> targets = new Seq<>();
    private float cooldown;

    public int queued() {
        return targets.size;
    }

    public void reset() {
        targets.clear();
        cooldown = 0f;
    }

    public boolean queue(Team team, int x, int y) {
        if (team == null || !Build.validBreak(team, x, y)) return false;
        for (Point2 target : targets) {
            if (target.x == x && target.y == y) return false;
        }
        targets.add(new Point2(x, y));
        Log.info("[AI] demolish queued: @,@", x, y);
        return true;
    }

    public void update(Team team, UnitManager units) {
        if (team == null || units == null || targets.isEmpty()) return;
        if (cooldown > 0f) {
            cooldown -= Time.delta;
            return;
        }

        for (int i = 0; i < targets.size; i++) {
            Point2 target = targets.get(i);
            if (!Build.validBreak(team, target.x, target.y)) {
                targets.remove(i--);
                continue;
            }

            Unit builder = null;
            for (Unit unit : units.builders) {
                if (unit != null && unit.isAdded() && !unit.dead() && unit.canBuild()) {
                    builder = unit;
                    break;
                }
            }
            if (builder == null) return;

            builder.addBuild(new BuildPlan(target.x, target.y));
            targets.remove(i);
            cooldown = 1.5f;
            Log.info("[AI] demolish started: @,@", target.x, target.y);
            return;
        }
    }
}
