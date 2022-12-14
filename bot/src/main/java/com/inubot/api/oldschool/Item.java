/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package com.inubot.api.oldschool;

import com.inubot.api.methods.Client;
import com.inubot.api.oldschool.action.ActionOpcodes;
import com.inubot.api.oldschool.action.Processable;
import com.inubot.api.oldschool.action.tree.*;
import com.inubot.api.util.CacheLoader;
import com.inubot.api.util.Identifiable;
import com.inubot.client.natives.oldschool.RSItemDefinition;

import java.awt.*;

public class Item implements Identifiable, Processable {

    //The client sets the bounds of an item as 32x32 [Constant]
    public static final Dimension DEFAULT_SIZE = new Dimension(32, 32);
    private int id;
    private int quantity;
    private int index;
    private InterfaceComponent owner;

    public Item(InterfaceComponent owner, int index) {
        this.id = owner.getType() == 2 ? owner.getItemIds()[index] - 1 : owner.getItemId();
        this.quantity = owner.getType() == 2 ? owner.getItemQuantities()[index] : owner.getItemQuantity();
        this.owner = owner;
        this.index = index;
    }

    public InterfaceComponent getOwner() {
        return owner;
    }

    public int getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getName() {
        RSItemDefinition def = getDefinition();
        if(def == null) {
            return "";
        }
        return def.getName() != null ? def.getName() : "";
    }

    public boolean isInTable() {
        return owner.getType() == 2;
    }

    public RSItemDefinition getDefinition() {
        return CacheLoader.findItemDefinition(id);
    }

    public boolean processFirst() {
        if (isInTable()) {
            RSItemDefinition def = getDefinition();
            if (def == null) {
                return false;
            }
            for (String action : def.getActions()) {
                if (action != null && !action.equals("null")) {
                    if (processAction(action)) {
                        return true;
                    }
                }
            }
        } else {
            for (String action : owner.getActions()) {
                if (action != null && !action.equals("null")) {
                    if (processAction(action)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean processAction(int opcode, String action) {
        String itemName = getName();
        if (itemName == null) {
            return false;
        }
        if (isInTable()) {
            Client.processAction(new TableItemAction(opcode, getId(), getIndex(), getOwner().getRaw().getId()), action, action);
            return true;
        }
        int index = Action.indexOf(owner.getActions(), action) + 1;
        if (index > 4) {
            Client.processAction(new InterfaceComponentAction(true, index, this.index, owner.getId()), action, action);
            return true;
        } else {
            Client.processAction(new InterfaceComponentAction(opcode, index, this.index, owner.getId()), action, action);
            return true;
        }
    }

    public boolean processAction(String action) {
        if (isInTable()) {
            RSItemDefinition def = getDefinition();
            if (def != null) {
                processAction(ActionOpcodes.ITEM_ACTION_0 + Action.indexOf(def.getActions(), action), action);
                return true;
            }
        } else {
            int index = Action.indexOf(owner.getActions(), action) + 1;
            Client.processAction(new InterfaceComponentAction(index > 4, index, this.index, owner.getId()), action, action);
            return true;
        }
        return false;
    }

    @Override
    public String[] getActions() {
        return isInTable() ? owner.getTableActions() : owner.getActions();
    }

    public void processAction(String action, String option) {
        if (isInTable()) {
            RSItemDefinition def = getDefinition();
            if (def == null)
                return;
            processAction(ActionOpcodes.ITEM_ACTION_0 + Action.indexOf(def.getActions(), action), action);
        } else {
            int index = Action.indexOf(owner.getActions(), action) + 1;
            Client.processAction(new InterfaceComponentAction(index > 4, index, this.index - 1, owner.getId()), action, option);
        }
    }

    public void use(Item target) {
        Client.processAction(new UseItemAction(getId(), getIndex(), getOwner().getId()), "Use", "Use");
        Client.processAction(new ItemOnItemAction(target.getId(), target.index, target.owner.getId()), "Use", "Use " + getName() + " -> " + target.getName());
    }

    public void use(GameObject target) {
        Client.processAction(new UseItemAction(getId(), getIndex(), getOwner().getId()), "Use", "Use");
        Client.processAction(new ItemOnEntityAction(ActionOpcodes.ITEM_ON_OBJECT, target.getRaw().getId(), target.getRegionX(), target.getRegionY()), "Use", "Use " + getName() + " -> " + target.getName());
    }

    public void use(GroundItem target) {
        Client.processAction(new UseItemAction(getId(), getIndex(), getOwner().getId()), "Use", "Use");
        Client.processAction(new ItemOnEntityAction(ActionOpcodes.ITEM_ON_GROUND_ITEM, target.getId(), target.getRaw().getRegionX(), target.getRaw().getRegionY()), "Use", "Use " + getName() + " -> " + target.getName());
    }

    public void drop() {
        processAction(ActionOpcodes.ITEM_ACTION_4, "Drop");
    }

    public boolean containsAction(String act) {
        RSItemDefinition def = getDefinition();
        if (def == null)
            return false;
        String[] actions = getDefinition().getActions();
        if (actions == null)
            return false;
        for (String action : actions) {
            if (action == null) continue;
            if (action.equals(act)) {
                return true;
            }
        }
        return false;
    }
}