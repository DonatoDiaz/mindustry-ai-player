package aiplayer;

import arc.Core;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ai.types.DefenderAI;
import mindustry.ai.types.MinerAI;
import mindustry.content.UnitTypes;
import mindustry.entities.Units;
import mindustry.entities.units.UnitController;
import mindustry.game.Team;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

public class AICharacter {
    private static final int AI_PLAYER_ID = 123456;

    private Player player;
    private Unit unit;
    private UnitController controller;
    private float respawnTimer;

    public void update(Team team, CoreBuild core) {
        if (!AIPlayer.inst.config.spawnAiPlayer) {
            remove();
            return;
        }
        if (team == null || core == null) {
            remove();
            return;
        }

        if (player != null && !player.isAdded()) {
            try {
                player.add();
                Log.info("[AI] AI player re-added");
            } catch (Throwable t) {
                player = null;
            }
        }
        if (player == null) createPlayer(team);

        Unit current = player == null ? null : player.unit();
        if (current != null && (!current.isAdded() || current.dead())) {
            player.clearUnit();
            current = null;
        }

        if (current == null) {
            respawnTimer -= Time.delta;
            if (respawnTimer <= 0f) spawnUnit(team, core);
            return;
        }

        unit = current;
        if (controller != null && current.controller() != controller) {
            current.controller(controller);
        }
    }

    private void createPlayer(Team team) {
        try {
            Player p = Player.create();
            p.id(AI_PLAYER_ID);
            p.name = AIPlayer.inst.config.aiPlayerName;
            p.locale = "en";
            p.team(team);
            p.color(team.color.cpy());
            p.add();
            player = p;
            respawnTimer = 0f;
            Log.info("[AI] AI player created: @", p.name);
        } catch (Throwable t) {
            Log.err("[AI] failed to create AI player", t);
            player = null;
        }
    }

    private void spawnUnit(Team team, CoreBuild core) {
        UnitType type = ContentIndex.bestCombat(team, core);
        boolean paid = type != null;

        if (type == null) {
            if (core.block instanceof CoreBlock block && block.unitType != null) {
                type = block.unitType;
            } else {
                type = UnitTypes.alpha;
            }
        }

        if (type == null) {
            respawnTimer = 5f;
            return;
        }

        ItemStack[] req = paid ? ContentIndex.requirements(type, team) : new ItemStack[0];
        if (paid && (req.length == 0 || !core.items.has(req))) {
            respawnTimer = 2f;
            return;
        }

        Unit spawned = type.spawn(team, core.x, core.y);
        if (spawned == null) {
            respawnTimer = 2f;
            return;
        }

        controller = type.hasWeapons() ? new DefenderAI() : new MinerAI();
        if (player != null) {
            player.set(core.x, core.y);
            player.unit(spawned);
        }
        spawned.controller(controller);
        spawned.add();
        Units.notifyUnitSpawn(spawned);
        if (paid) core.items.remove(req);

        unit = spawned;
        respawnTimer = 5f;
        Log.info("[AI] AI player unit: @ (@)", type.name, paid ? "paid" : "core");
    }

    public void remove() {
        if (player != null) {
            Unit current = player.unit();
            player.clearUnit();
            if (current != null && current.isAdded()) current.remove();
            if (player.isAdded()) player.remove();
            player = null;
        } else if (unit != null && unit.isAdded()) {
            unit.remove();
        }
        unit = null;
        controller = null;
        respawnTimer = 0f;
    }

    public void rename(String name) {
        if (player != null && name != null && !name.trim().isEmpty()) {
            player.name = name.trim();
        }
    }

    public boolean alive() {
        return player != null && player.isAdded() && player.unit() != null && player.unit().isAdded() && !player.unit().dead();
    }

    public Player player() {
        return player;
    }
}
