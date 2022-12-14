/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package com.inubot.bundledscripts.complete.hunter;

import com.inubot.api.methods.*;
import com.inubot.api.methods.Inventory;
import com.inubot.api.methods.traversal.Movement;
import com.inubot.api.oldschool.*;
import com.inubot.api.oldschool.action.ActionOpcodes;
import com.inubot.api.util.*;
import com.inubot.client.natives.oldschool.RSEntityMarker;
import com.inubot.client.natives.oldschool.RSObjectDefinition;
import com.inubot.script.Manifest;
import com.inubot.script.Script;

import java.awt.*;

/**
 * @author unsigned
 * @since 17-05-2015
 */
@Manifest(
        name = "RedChins PRO",
        developer = "septron",
        desc = "Catches carnivourous chinchompas for decent cash money!"
)
public class RedChinsPRO extends Script implements Paintable {

    private static final int EXP_EACH = 265;
    private static final int PRICE_EST = 1300;

    private Tile tile;
    private int startExp;
    private StopWatch runtime;
    private boolean fuckOff = false;

    public boolean setup() {
        if (!Game.isLoggedIn()) {
            System.err.println("Start logged in!"); //need 2 get base tile. cud do something like setup(String... args) and hardcode start tile
            return false;
        }
        startExp = Skills.getExperience(Skill.HUNTER);
        runtime = new StopWatch(0);
        tile = Players.getLocal().getLocation();
        return true;
    }

    private boolean isMakingGarden(Tile t) {
        GameObject[] objects = GameObjects.getLoadedAt(t.getRegionX(), t.getRegionY(), 0);
        for (GameObject object : objects) {
            RSObjectDefinition def = object.getDefinition();
            if (def != null) {
                String name = def.getName();
                if (name != null && (name.equals("Flowers") || name.equals("Fire")))
                    return true;
            }
        }
        return false;
    }

    @Override
    public int loop() {
//        if (fuckOff) {
//            for (char c : Random.nextElement(MEMES).toCharArray())
//                Inubot.getInstance().getCanvas().sendKey(c, 20);
//            Inubot.getInstance().getCanvas().pressEnter();
//            fuckOff = false;
//        }

        Npc npc = Npcs.getNearest(cock -> {
            if (cock.getTarget() != null && cock.getTarget().getRaw() == Players.getLocal().getRaw()) {
                for (String action : cock.getDefinition().getActions()) {
                    if (action.equals("Dismiss")) {
                        return true;
                    }
                }
            }
            return false;
        });
        if (npc != null)
            npc.processAction("Dismiss");

        Tile next = getNextLocation();
        if (next != null) {
            GroundItem item = GroundItems.getNearest(groundItem -> groundItem.getLocation().equals(next)
                    && "Box trap".equals(groundItem.getName()));
            GameObject obj = GameObjects.getNearest(t -> t.getLocation().equals(next));
            if (obj != null && (obj.containsAction("Check") || !obj.containsAction("Investigate"))) {
                obj.processAction(obj.containsAction("Check") ? "Check" : "Dismantle");
                if (Time.await(() -> GameObjects.getNearest(t -> t.getLocation().equals(next)) != null
                        && Players.getLocal().getAnimation() != -1, 1500)) {
                    Time.await(() -> GameObjects.getNearest(t -> t.getLocation().equals(next)) != null
                            && Players.getLocal().getAnimation() == -1, 1500);
                }
                return 420;
            }
            if (item == null) {
                if (!Players.getLocal().getLocation().equals(next)) {
                    Movement.walkTo(next);
                    return 600;
                }
                Item trap = Inventory.getFirst("Box trap");
                if (trap != null && Players.getLocal().getLocation().equals(next)) {
                    trap.processAction(ActionOpcodes.ITEM_ACTION_0, "Lay");
                    if (Time.await(() -> GameObjects.getNearest(t -> t.getLocation().equals(next)) != null
                            && Players.getLocal().getAnimation() != -1, 1500)) {
                        Time.await(() -> GameObjects.getNearest(t -> t.getLocation().equals(next)) != null
                                && Players.getLocal().getAnimation() == -1, 1500);
                    }
                }
            } else {
                item.processAction(ActionOpcodes.GROUND_ITEM_ACTION_3, "Lay");
                if (Time.await(() -> GameObjects.getNearest(t -> t.getLocation().equals(next)) != null
                        && Players.getLocal().getAnimation() != -1, 1500)) {
                    Time.await(() -> GameObjects.getNearest(t -> t.getLocation().equals(next)) != null
                            && Players.getLocal().getAnimation() == -1, 1500);
                }
            }
        }
        return 800;
    }

    private int getMaxTraps() {
        int inv = Inventory.getCount("Box trap");
        int max = Skills.getLevel(Skill.HUNTER) / 20 + 1;
        return Math.min(inv, max);
    }

    private Tile[] getTrapTactics() {
        switch (getMaxTraps()) {
            case 1:
                return new Tile[]{tile};
            case 2:
                return new Tile[]{tile.derive(-1, 0), tile.derive(1, 0)};
            case 3:
                return new Tile[]{tile.derive(-1, 0), tile.derive(0, -1), tile.derive(1, 0)};
            case 4:
                return new Tile[]{tile.derive(-1, 0), tile.derive(0, -1), tile.derive(1, 0), tile.derive(0, 1)};
            case 5:
                return new Tile[]{tile.derive(-1, 1), tile.derive(-1, -1), tile, tile.derive(1, -1), tile.derive(1, 1)};
            default: {
                return new Tile[0];
            }
        }
    }


    private Tile[] getTrapTacticsAlt() {
        switch (getMaxTraps()) {
            case 1:
                return new Tile[]{
                        tile
                };
            case 2:
                return new Tile[]{
                        tile.derive(-1, 0), tile.derive(1, 0)};
            case 3:
                return new Tile[]{
                        tile.derive(-1, 0), tile.derive(0, -1), tile.derive(1, 0)
                };
            case 4:
                return new Tile[]{
                        new Tile(2502, 2881), new Tile(2504, 2881),
                        new Tile(2502, 2880), new Tile(2504, 2880)
                };
            case 5:
                return new Tile[]{
                        new Tile(2502, 2881), new Tile(2504, 2881),
                        new Tile(2502, 2880), new Tile(2504, 2880),
                        new Tile(2503, 2880)
                };
            default: {
                return new Tile[0];
            }
        }
    }

    public Tile getNextLocation() {
        Tile[] formation = getTrapTactics();
        for (Tile tile : formation) { //no trap is available
            if (isMakingGarden(tile)) {
                continue;
            }
            if (GameObjects.getNearest(o -> o.getLocation().equals(tile)
                    && o.getRaw() instanceof RSEntityMarker
                    && o.getName() != null && !o.getName().equals("Grass")) == null)
                return tile;
        }
        for (Tile tile : formation) {
            GameObject obj = GameObjects.getNearest(o -> o.getLocation().equals(tile));
            if (obj != null && (obj.containsAction("Check") || !obj.containsAction("Investigate"))) {
                return tile;
            }
        }
        return null;
    }

    @Override
    public void render(Graphics2D g) {
        g.drawString("Runtime: " + runtime.toElapsedString(), 30, 30);
        int gain = Skills.getExperience(Skill.HUNTER) - startExp;
        g.drawString("Exp: " + gain, 30, 50);
        int caught = gain / EXP_EACH;
        g.drawString("Profit: " + PRICE_EST * caught, 30, 70);
        g.drawString("Caught: " + caught, 30, 90);
    }
}
