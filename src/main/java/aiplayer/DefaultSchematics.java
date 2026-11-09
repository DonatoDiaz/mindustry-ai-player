package aiplayer;

import arc.files.Fi;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustry.world.Block;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.storage.StorageBlock;
import mindustry.world.blocks.power.PowerNode;
import mindustry.world.meta.BlockFlag;

public final class DefaultSchematics {
    private DefaultSchematics() {
    }

    public static void ensure(Fi folder) {
        if (folder == null) return;
        folder.mkdirs();
        if (!folder.child("ai-power.msch").exists()) createPower(folder);
        if (!folder.child("ai-defense.msch").exists()) createDefense(folder);
        if (!folder.child("ai-turret.msch").exists()) createTurret(folder);
    }

    private static void createPower(Fi folder) {
        Block solar = block("solar-panel");
        if (!valid(solar)) solar = find(b -> b.flags.contains(BlockFlag.generator) && b.size == 1);
        Block battery = block("battery");
        if (!valid(battery)) battery = find(b -> b.flags.contains(BlockFlag.battery) && b.size == 1);
        Block node = block("power-node");
        if (!valid(node)) node = find(b -> b instanceof PowerNode);
        if (!valid(solar) || !valid(battery) || !valid(node)) return;

        Seq<Schematic.Stile> tiles = new Seq<>();
        int x = 0;
        tiles.add(new Schematic.Stile(solar, x, 0, null, (byte) 0));
        x += solar.size;
        tiles.add(new Schematic.Stile(battery, x, 0, null, (byte) 0));
        x += battery.size;
        tiles.add(new Schematic.Stile(node, x, 0, null, (byte) 0));
        x += node.size;
        write("ai-power", "power", tiles, x, Math.max(solar.size, Math.max(battery.size, node.size)), folder);
    }

    private static void createDefense(Fi folder) {
        Block wall = find(b -> b instanceof Wall && b.size == 1);
        if (!valid(wall)) return;

        Seq<Schematic.Stile> tiles = new Seq<>();
        for (int i = 0; i < 6; i++) {
            tiles.add(new Schematic.Stile(wall, i, 0, null, (byte) 0));
        }
        write("ai-defense", "defense", tiles, 6, 1, folder);
    }

    private static void createAttack(Fi folder) {
        Block turret = block("duo");
        if (!valid(turret)) turret = find(b -> b.flags.contains(BlockFlag.turret) && b.size <= 3);
        if (!valid(turret)) return;

        Block wall = find(b -> b instanceof Wall && b.size == 1);
        Seq<Schematic.Stile> tiles = new Seq<>();
        int size = turret.size;
        tiles.add(new Schematic.Stile(turret, 1, 1, null, (byte) 0));
        if (valid(wall)) {
            for (int x = 0; x < size + 2; x++) {
                for (int y = 0; y < size + 2; y++) {
                    if (x == 0 || y == 0 || x == size + 1 || y == size + 1) {
                        tiles.add(new Schematic.Stile(wall, x, y, null, (byte) 0));
                    }
                }
            }
        }
        write("ai-attack", "attack", tiles, size + 2, size + 2, folder);
    }

    private static void createTurret(Fi folder) {
        Block generator = block("solar-panel");
        if (!valid(generator)) generator = find(b -> b.flags.contains(BlockFlag.generator) && b.size <= 2);
        Block turret = find(b -> b.flags.contains(BlockFlag.turret) && b.consumesPower && !b.hasLiquids && b.size <= 3);
        Block battery = block("battery");
        if (!valid(battery)) battery = find(b -> b.flags.contains(BlockFlag.battery) && b.size == 1);
        Block node = block("power-node");
        if (!valid(node)) node = find(b -> b instanceof PowerNode);
        if (!valid(generator) || !valid(turret) || !valid(node)) return;

        Seq<Schematic.Stile> tiles = new Seq<>();
        int x = 0;
        tiles.add(new Schematic.Stile(generator, x, 0, null, (byte) 0));
        x += generator.size;
        if (valid(battery)) {
            tiles.add(new Schematic.Stile(battery, x, 0, null, (byte) 0));
            x += battery.size;
        }
        tiles.add(new Schematic.Stile(node, x, 0, null, (byte) 0));
        x += node.size;
        tiles.add(new Schematic.Stile(turret, x, 0, null, (byte) 0));
        x += turret.size;
        write("ai-turret", "defense", tiles, x, Math.max(generator.size, Math.max(node.size, turret.size)), folder);
    }

    private static void createProduction(Fi folder) {
        Block drill = find(b -> b.flags.contains(BlockFlag.drill) && b.size >= 2 && b.size <= 3);
        Block conveyor = find(b -> b instanceof Conveyor);
        Block storage = block("container");
        if (!valid(storage)) storage = find(b -> b instanceof StorageBlock && !(b instanceof CoreBlock) && b.size >= 2);
        if (!valid(drill) || !valid(conveyor) || !valid(storage)) return;

        Seq<Schematic.Stile> tiles = new Seq<>();
        tiles.add(new Schematic.Stile(drill, 0, 0, null, (byte) 0));
        int x = drill.size;
        tiles.add(new Schematic.Stile(conveyor, x, 0, null, (byte) 0));
        x += conveyor.size;
        tiles.add(new Schematic.Stile(storage, x, 0, null, (byte) 0));
        x += storage.size;
        write("ai-production", "production", tiles, x, Math.max(drill.size, storage.size), folder);
    }

    private static void write(String name, String role, Seq<Schematic.Stile> tiles, int width, int height, Fi folder) {
        try {
            StringMap tags = new StringMap();
            tags.put("ai-role", role);
            Schematic schematic = new Schematic(tiles, tags, width, height);
            Schematics.write(schematic, folder.child(name + ".msch"));
            Log.info("[AI] generated schematic: @", name);
        } catch (Exception e) {
            Log.err("[AI] failed to generate schematic @", name, e);
        }
    }

    private static Block block(String name) {
        Block block = Vars.content.block(name);
        return valid(block) ? block : null;
    }

    private static Block find(arc.func.Boolf<Block> predicate) {
        for (Block block : Vars.content.blocks()) {
            if (valid(block) && predicate.get(block)) return block;
        }
        return null;
    }

    private static boolean valid(Block block) {
        return block != null
            && block.unlockedNow()
            && !Vars.state.rules.isBanned(block)
            && block.requirements != null;
    }
}
