package aiplayer;

import arc.files.Fi;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustry.game.Team;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.BlockFlag;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConstructionPlanner {
    public final BuildQueue queue = new BuildQueue();
    public final SchematicLibrary library = new SchematicLibrary();

    private float cooldown;
    private String lastRole = "none";
    private final Map<String, Integer> builtCounts = new HashMap<>();
    private final Set<Long> aiBuilt = new HashSet<>();

    public void load(Fi folder) {
        library.load(folder);
    }

    public void reset() {
        cooldown = 0f;
        lastRole = "none";
        builtCounts.clear();
        aiBuilt.clear();
    }

    public int queueSize(Team team) {
        return queue.size(team);
    }

    public int schematicCount() {
        return library.schematics.size;
    }

    public String schematicPath() {
        return library.path;
    }

    public String lastRole() {
        return lastRole;
    }

    public String nextSchematicName(Team team, CoreBuild core) {
        Schematic schematic = library.next(lastRole.equals("none") ? null : lastRole, team, core);
        return schematic == null ? "none" : schematic.name();
    }

    public void update(Team team, CoreBuild core, boolean autoBuild, Snapshot snapshot, UnitManager units, String llmRole) {
        if (team == null) return;
        queue.prune(team);
        if (!autoBuild || core == null) return;
        if (cooldown > 0f) {
            cooldown -= Time.delta;
            return;
        }
        if (queue.size(team) > 0) return;

        String role = EconomyAnalyzer.chooseRole(team, core, snapshot, units, library, llmRole);
        lastRole = role == null ? "misc" : role;

        if (trySchematic(team, core, role)) {
            cooldown = 2f;
            return;
        }
        if (role != null && trySchematic(team, core, null)) {
            cooldown = 2f;
            return;
        }
        if (library.schematics.isEmpty() && tryWalls(team, core)) {
            cooldown = 5f;
        }
    }

    private boolean trySchematic(Team team, CoreBuild core, String role) {
        if ("power".equals(role) && AIPlayer.inst.config.debug) {
            Log.info("[AI] power role: candidates=@", library.candidates(role).size);
        }
        var candidates = library.candidates(role).copy();
        candidates.sort((a, b) -> {
            int score = Float.compare(AIPlayer.inst.learning.score(b.name()), AIPlayer.inst.learning.score(a.name()));
            if (score != 0) return score;
            return Integer.compare(priority(b), priority(a));
        });

        for (Schematic schematic : candidates) {
            String actualRole = role == null ? library.roleOf(schematic) : role;
            if ("attack".equals(actualRole) && !AIPlayer.inst.config.allowAttackSchematics) continue;
            if (schematic.name().equals("ai-attack")) continue;
            if (builtCounts.getOrDefault(schematic.name(), 0) >= maxBuilds(actualRole)) continue;
            if (!EconomyAnalyzer.available(schematic, core)) continue;

            ItemStack[] req = schematic.requirements().toArray();
            if (req.length > 0 && !core.items.has(req, Vars.state.rules.buildCostMultiplier)) continue;
            if (tryPlaceSchematic(schematic, team, core)) {
                builtCounts.merge(schematic.name(), 1, Integer::sum);
                AIPlayer.inst.learning.recordBuilt(schematic.name(), actualRole, core);
                Log.info("[AI] schematic (@): @", actualRole, schematic.name());
                return true;
            }
        }
        return false;
    }

    public boolean isAiBuilt(int x, int y) {
        return aiBuilt.contains(key(x, y));
    }

    private long key(int x, int y) {
        return ((long) x << 32) ^ (y & 0xffffffffL);
    }

    private int priority(Schematic schematic) {
        String name = schematic.name();
        if (name.contains("turret")) return 10;
        if (name.contains("power")) return 8;
        if (name.contains("production")) return 6;
        if (name.contains("defense")) return 4;
        return 1;
    }

    private int maxBuilds(String role) {
        if ("defense".equals(role)) return 2;
        if ("power".equals(role)) return 3;
        if ("production".equals(role)) return 2;
        if ("attack".equals(role)) return 1;
        return 1;
    }

    private boolean tryPlaceSchematic(Schematic schematic, Team team, CoreBuild payer) {
        for (CoreBuild anchor : team.data().cores) {
            int cx = anchor.tile.x;
            int cy = anchor.tile.y;
            int attempts = 0;

            for (int rotation = 0; rotation < 4; rotation++) {
                Schematic rotated = Schematics.rotate(schematic, rotation);
                for (int dx = -12; dx <= 12; dx += 4) {
                    for (int dy = -12; dy <= 12; dy += 4) {
                        if (Math.abs(dx) < 3 && Math.abs(dy) < 3) continue;
                        if (++attempts > 24) break;
                        if (tryPlace(rotated, team, payer, cx + dx, cy + dy)) return true;
                    }
                    if (attempts > 24) break;
                }
                if (attempts > 24) break;
            }
        }
        return false;
    }

    private boolean tryPlace(Schematic schematic, Team team, CoreBuild payer, int x, int y) {
        var plans = Vars.schematics.toPlans(schematic, x, y);
        if (plans.isEmpty()) return false;

        boolean hasDrill = false;
        boolean hasOre = false;
        int[] ore = {0};
        for (BuildPlan plan : plans) {
            if (plan.block == null || plan.breaking) return false;
            if (plan.block instanceof CoreBlock) return false;
            if (plan.block.flags.contains(BlockFlag.drill)) {
                hasDrill = true;
                plan.block.iterateTaken(plan.x, plan.y, (tx, ty) -> {
                    Tile oreTile = Vars.world.tile(tx, ty);
                    if (oreTile != null && oreTile.drop() != null) ore[0] = 1;
                });
            }
            if (!Build.validPlace(plan.block, team, plan.x, plan.y, plan.rotation)) return false;
        }
        hasOre = ore[0] == 1;
        if (hasDrill && !hasOre) return false;

        int queued = 0;
        for (BuildPlan plan : plans) {
            if (queue.enqueue(team, payer, plan.block, plan.x, plan.y, plan.rotation, plan.config)) {
                queued++;
                if (plan.block.flags.contains(BlockFlag.turret)) aiBuilt.add(key(plan.x, plan.y));
            }
        }
        return queued > 0;
    }

    private boolean tryWalls(Team team, CoreBuild core) {
        Block wall = bestWall(team, core);
        if (wall == null) {
            if (AIPlayer.inst.config.debug) Log.info("[AI] no affordable wall");
            return false;
        }

        int cx = core.tile.x;
        int cy = core.tile.y;
        int radius = 8;
        int existing = 0;
        int queued = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (Math.abs(dx) != radius && Math.abs(dy) != radius) continue;
                Tile tile = Vars.world.tile(cx + dx, cy + dy);
                if (tile == null) continue;
                if (tile.block() instanceof Wall) existing++;
            }
        }
        if (existing >= 8) return false;

        for (int dx = -radius; dx <= radius; dx++) {
            if (queued >= 12) break;
            for (int dy = -radius; dy <= radius; dy++) {
                if (Math.abs(dx) != radius && Math.abs(dy) != radius) continue;
                if (queue.enqueue(team, core, wall, cx + dx, cy + dy, 0, null)) queued++;
                if (queued >= 12) break;
            }
        }

        if (queued > 0) {
            Log.info("[AI] wall plans: @ (@)", queued, wall.name);
        } else if (AIPlayer.inst.config.debug) {
            Log.info("[AI] wall placement failed (@)", wall.name);
        }
        return queued > 0;
    }

    private Block bestWall(Team team, CoreBuild core) {
        Block best = null;
        int bestHealth = -1;
        for (Block block : Vars.content.blocks()) {
            if (!(block instanceof Wall) || block.size != 1 || !block.unlockedNow()) continue;
            if (Vars.state.rules.isBanned(block)) continue;
            if (block.requirements == null || !core.items.has(block.requirements, Vars.state.rules.buildCostMultiplier)) continue;
            if (block.health > bestHealth) {
                bestHealth = block.health;
                best = block;
            }
        }
        return best;
    }
}
