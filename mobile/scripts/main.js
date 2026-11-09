const MinerAI = Packages.mindustry.ai.types.MinerAI;
const DefenderAI = Packages.mindustry.ai.types.DefenderAI;
const ItemStack = Packages.mindustry.type.ItemStack;
const Units = Packages.mindustry.entities.Units;

let enabled = Core.settings.getBool("ai-player-mobile.enabled", true);
let maxMiners = Core.settings.getInt("ai-player-mobile.miners", 3);
let maxArmy = Core.settings.getInt("ai-player-mobile.army", 4);
let miners = [];
let army = [];
let timer = 0;
let expanded = false;
let hud = null;

function save() {
    Core.settings.put("ai-player-mobile.enabled", enabled);
    Core.settings.put("ai-player-mobile.miners", maxMiners);
    Core.settings.put("ai-player-mobile.army", maxArmy);
}

function requirements(type, team) {
    let req = type.getTotalRequirements();
    if (req == null || req.length == 0) return null;
    return ItemStack.mult(req, Vars.state.rules.unitCost(team));
}

function totalCost(type) {
    let req = type.getTotalRequirements();
    if (req == null) return 0;
    let total = 0;
    for (let i = 0; i < req.length; i++) {
        if (req[i] != null && req[i].item != null) total += req[i].amount;
    }
    return total;
}

function canAfford(type, team, core) {
    let req = requirements(type, team);
    return req != null && req.length > 0 && core.items.has(req);
}

function hasMineableOre(type) {
    if (Vars.indexer == null) return true;
    let items = Vars.content.items();
    for (let i = 0; i < items.size; i++) {
        let item = items.get(i);
        if (item == null) continue;
        if (!Vars.indexer.hasOre(item) && !Vars.indexer.hasWallOre(item)) continue;
        if (type.mineTier < item.hardness) continue;
        if (type.mineItems != null && !type.mineItems.contains(item)) continue;
        return true;
    }
    return false;
}

function bestMiner(team, core) {
    let best = null, score = -1;
    let list = Vars.content.units();
    for (let i = 0; i < list.size; i++) {
        let type = list.get(i);
        if (type == null || !type.unlockedNow() || type.mineSpeed <= 0) continue;
        if (!type.mineFloor && !type.mineWalls) continue;
        if (Vars.state.rules.isBanned(type) || !canAfford(type, team, core)) continue;
        if (!hasMineableOre(type)) continue;
        let value = type.mineTier * 100 + type.mineSpeed * 10 - totalCost(type) * 0.05;
        if (value > score) {
            score = value;
            best = type;
        }
    }
    return best;
}

function bestCombat(team, core) {
    let best = null, score = -1;
    let list = Vars.content.units();
    for (let i = 0; i < list.size; i++) {
        let type = list.get(i);
        if (type == null || !type.unlockedNow() || !type.hasWeapons()) continue;
        if (Vars.state.rules.isBanned(type) || !canAfford(type, team, core)) continue;
        let value = type.dpsEstimate / Math.max(1, totalCost(type));
        if (value > score) {
            score = value;
            best = type;
        }
    }
    return best;
}

function spawn(type, team, core, controller) {
    let req = requirements(type, team);
    if (req == null || req.length == 0 || !core.items.has(req)) return null;
    let unit = type.spawn(team, core.x, core.y);
    if (unit == null) return null;
    unit.controller(controller);
    unit.add();
    Units.notifyUnitSpawn(unit);
    core.items.remove(req);
    return unit;
}

function prune(list) {
    for (let i = list.length - 1; i >= 0; i--) {
        let unit = list[i];
        if (unit == null || !unit.isAdded() || unit.dead()) list.splice(i, 1);
    }
}

function updateAI() {
    if (!Vars.state.isPlaying() || Vars.player == null) return;
    if (Vars.net.client()) return;

    if (!enabled) return;

    let team = Vars.player.team();
    if (team == null) return;
    let core = team.core();
    if (core == null) return;

    prune(miners);
    prune(army);

    timer += Time.delta;
    if (timer < 1) return;
    timer = 0;

    let cap = Units.getCap(team);
    let count = team.data().units.size;

    if (miners.length < maxMiners && count < cap) {
        let type = bestMiner(team, core);
        if (type != null) {
            let unit = spawn(type, team, core, new MinerAI());
            if (unit != null) {
                miners.push(unit);
                Log.info("[AI Mobile] miner: " + type.name);
            }
        }
    }

    if (army.length < maxArmy && count < cap) {
        let type = bestCombat(team, core);
        if (type != null) {
            let unit = spawn(type, team, core, new DefenderAI());
            if (unit != null) {
                army.push(unit);
                Log.info("[AI Mobile] army: " + type.name);
            }
        }
    }
}

function status() {
    let state = enabled ? "[green]ON[]" : "[red]OFF[]";
    return "[accent]AI Mobile[] " + state
        + "\nminers " + miners.length + "/" + maxMiners
        + "  army " + army.length + "/" + maxArmy
        + "\n[gray]no local LLM[]";
}

function positionHud() {
    if (hud == null) return;
    let x = Core.graphics.getWidth() - hud.getWidth() - 10;
    let y = Core.graphics.getHeight() / 2 - hud.getHeight() / 2;
    hud.setPosition(Math.max(10, x), y);
}

function rebuild() {
    if (hud == null) return;
    hud.clearChildren();

    if (!expanded) {
        hud.button("AI", () => {
            expanded = true;
            rebuild();
        }).size(46);
    } else {
        hud.label(() => status()).left().row();
        hud.button("AI on/off", () => {
            enabled = !enabled;
            save();
        }).size(150, 38).row();
        hud.button("Hide", () => {
            expanded = false;
            rebuild();
        }).size(150, 38);
    }

    hud.pack();
    positionHud();
}

function buildHud() {
    if (Vars.headless || Vars.ui == null) return;
    hud = new Table(Styles.black6);
    hud.margin(6);
    hud.visibility = () => Vars.state.isPlaying();
    rebuild();
    Vars.ui.hudGroup.addChild(hud);
    positionHud();
}

Events.on(ClientLoadEvent, () => {
    buildHud();
});

Events.on(WorldLoadEvent, () => {
    Log.info("[AI Mobile] world loaded");
    miners = [];
    army = [];
    timer = 0;
});

Events.run(Trigger.update, () => {
    updateAI();
});

Events.on(ResizeEvent, () => {
    positionHud();
});
