package aiplayer;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustry.game.Team;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.world.meta.BlockFlag;

public class SchematicLibrary {
    public final Seq<Schematic> schematics = new Seq<>();
    public final ObjectMap<String, Seq<Schematic>> roles = new ObjectMap<>();
    public String path = "";

    public void load(Fi folder) {
        clear();
        if (folder == null) return;

        DefaultSchematics.ensure(folder);
        path = folder.toString();

        for (Fi file : folder.list()) {
            if (file.isDirectory()) {
                String role = normalizeRole(file.name());
                for (Fi nested : file.list()) {
                    if (nested.extEquals("msch")) read(nested, role);
                }
            } else if (file.extEquals("msch")) {
                read(file, null);
            }
        }

        if (!hasRole("production") && Vars.schematics != null) {
            for (Schematic schematic : Vars.schematics.all()) {
                if ("production".equals(classify(schematic))) register(schematic, "production");
            }
        }

        for (Seq<Schematic> list : roles.values()) {
            list.sort((a, b) -> Integer.compare(a.requirements().total, b.requirements().total));
        }
        schematics.sort((a, b) -> Integer.compare(a.requirements().total, b.requirements().total));
        Log.info("[AI] schematics loaded: @ (@)", schematics.size, path);
    }

    private void clear() {
        schematics.clear();
        roles.clear();
        path = "";
    }

    private void read(Fi file, String forcedRole) {
        try {
            register(Schematics.read(file), forcedRole);
        } catch (Exception e) {
            Log.err("[AI] failed to read schematic @", file.name(), e);
        }
    }

    private void register(Schematic schematic, String forcedRole) {
        if (!usable(schematic)) return;
        if (schematics.contains(schematic, true)) return;
        schematics.add(schematic);

        String role = forcedRole != null ? forcedRole : classify(schematic);
        if (role == null) role = "misc";
        Seq<Schematic> list = roles.get(role);
        if (list == null) {
            list = new Seq<>();
            roles.put(role, list);
        }
        list.add(schematic);
    }

    private boolean usable(Schematic schematic) {
        if (schematic == null || schematic.hasCore()) return false;
        if (schematic.width <= 0 || schematic.height <= 0 || schematic.width > 40 || schematic.height > 40) return false;
        if (schematic.tiles.isEmpty()) return false;
        for (Schematic.Stile tile : schematic.tiles) {
            Block block = tile.block;
            if (block == null || !block.unlockedNow()) return false;
            if (Vars.state.rules.isBanned(block)) return false;
        }
        return true;
    }

    private String classify(Schematic schematic) {
        String tag = schematic.tags.get("ai-role");
        if (tag != null && !tag.isEmpty()) return normalizeRole(tag);
        for (Schematic.Stile tile : schematic.tiles) {
            Block block = tile.block;
            if (block == null) continue;
            if (block.flags.contains(BlockFlag.turret)) return "attack";
            if (block.flags.contains(BlockFlag.drill) || block.flags.contains(BlockFlag.factory)) return "production";
            if (block.flags.contains(BlockFlag.generator) || block.flags.contains(BlockFlag.battery)) return "power";
            if (block.flags.contains(BlockFlag.repair) || block.name.contains("wall")) return "defense";
        }
        return "misc";
    }

    private String normalizeRole(String role) {
        String value = role.toLowerCase();
        if (value.equals("economy") || value.equals("production")) return "production";
        if (value.equals("defence") || value.equals("defense")) return "defense";
        if (value.equals("power") || value.equals("energy")) return "power";
        if (value.equals("attack") || value.equals("military")) return "attack";
        return value;
    }

    public String roleOf(Schematic schematic) {
        String tag = schematic.tags.get("ai-role");
        return tag == null || tag.isEmpty() ? classify(schematic) : normalizeRole(tag);
    }

    public boolean hasRole(String role) {
        Seq<Schematic> list = roles.get(role);
        return list != null && !list.isEmpty();
    }

    public Seq<Schematic> candidates(String role) {
        if (role == null) return schematics;
        Seq<Schematic> list = roles.get(role);
        return list == null ? new Seq<>() : list;
    }

    public Schematic next(String role, Team team, CoreBuild core) {
        for (Schematic schematic : candidates(role)) {
            if (EconomyAnalyzer.available(schematic, core)) return schematic;
        }
        return null;
    }

    public Schematic nextAffordable(Team team, CoreBuild core) {
        return next(null, team, core);
    }

    public Schematic usableSchematic(Schematic schematic) {
        return usable(schematic) ? schematic : null;
    }
}
