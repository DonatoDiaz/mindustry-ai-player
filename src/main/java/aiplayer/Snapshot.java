package aiplayer;

import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

public class Snapshot {
    public final Team team;
    public final CoreBuild core;
    public final int unitCount;
    public final int unitCap;
    public final int enemies;

    private Snapshot(Team team, CoreBuild core, int unitCount, int unitCap, int enemies) {
        this.team = team;
        this.core = core;
        this.unitCount = unitCount;
        this.unitCap = unitCap;
        this.enemies = enemies;
    }

    public static Snapshot capture(Team team) {
        CoreBuild core = team.core();
        int enemies = 0;
        if (core != null) {
            int[] count = {0};
            Units.nearbyEnemies(team, core.x, core.y, 700f, unit -> count[0]++);
            enemies = count[0];
        }
        return new Snapshot(team, core, team.data().units.size, Units.getCap(team), enemies);
    }
}
