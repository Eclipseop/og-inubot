/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package com.inubot.api.methods;

import com.inubot.Inubot;
import com.inubot.api.oldschool.NodeTable;
import com.inubot.api.util.CacheLoader;
import com.inubot.client.natives.oldschool.RSItemDefinition;
import com.inubot.client.natives.oldschool.RSItemTable;
import com.inubot.client.natives.oldschool.RSNode;
import com.inubot.api.util.Identifiable;
import com.inubot.client.natives.oldschool.RSNodeTable;

import java.util.ArrayList;
import java.util.List;

/**
 * This class should be used rather than the respective provider class (e.g. Bank, Equipment) in situations where you
 * do not need to interact with the item (counting number of items, getting cached item values etc).
 */
public class ItemTables {

    public static final int VARROCK_GENERAL_STORE = 4;
    public static final int VARROCK_RUNE_STORE = 5;
    public static final int VARROCK_STAFF_STORE = 51;
    public static final int PRICE_CHECKER = 90;
    public static final int INVENTORY = 93;
    public static final int EQUIPMENT = 94;
    public static final int BANK = 95;
    public static final int EXCHANGE_COLLECTION = 518;

    public static RSNodeTable getRaw() {
        return Inubot.getInstance().getClient().getItemTables();
    }

    public static NodeTable getStorage() {
        RSNodeTable raw = getRaw();
        return raw != null ? new NodeTable(raw) : null;
    }

    /**
     * Note: ItemTable values for non-default widgets such as shop, bank are cached from when the widget was last opened.
     * Example: If I open bank in g/e and then walk away it will return the values from the bank.
     * Another note: If I opened the bank in g/e, and then walk into another region the table values will reset to
     * [-1, -1, -1, -1, -1, -1, -1. -1, -1, -1, -1, -1, -1, -1. -1, -1, -1, -1, -1, -1, -1. -1, -1, -1, -1, -1, -1, -1]
     *
     * @param tableKey the node key
     * @return The RSItemTable whose key matches the given tableKey
     */
    private static RSItemTable lookup(int tableKey) {
        NodeTable store = getStorage();
        if (store == null) {
            return null;
        }
        RSNode node = store.lookup(tableKey);
        return node != null && node instanceof RSItemTable ? (RSItemTable) node : null;
    }

    public static Entry[] getEntriesIn(int tableKey) {
        RSItemTable table = lookup(tableKey);
        if (table == null) {
            return new Entry[0];
        }
        int len = table.getIds().length;
        if (len != table.getStackSizes().length) {
            return new Entry[0];
        }
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            int id = table.getIds()[i];
            int amt = table.getStackSizes()[i];
            if (id > 0 && amt > 0) {
                entries.add(new Entry(i, id, amt));
            }
        }
        return entries.toArray(new Entry[entries.size()]);
    }

    public static int[] getIdsIn(int tableKey) {
        RSItemTable table = lookup(tableKey);
        if (table == null) {
            return new int[0];
        }
        int len = table.getIds().length;
        if (len != table.getStackSizes().length) {
            return new int[0];
        }
        List<Integer> values = new ArrayList<>();
        for (int value : table.getIds()) {
            if (value > 0) {
                values.add(value);
            }
        }
        int[] valuesNew = new int[values.size()];
        for (int i = 0; i < valuesNew.length; i++) {
            valuesNew[i] = values.get(i);
        }
        return valuesNew;
    }

    public static int getIdAt(int tableKey, int index) {
        int[] ids = getIdsIn(tableKey);
        if (ids.length > index) {
            return ids[index];
        }
        return -1;
    }

    public static int getQuantityAt(int tableKey, int index) {
        int[] qtys = getQuantitiesIn(tableKey);
        if (qtys.length > index) {
            return qtys[index];
        }
        return -1;
    }

    public static Entry getEntryAt(int tableKey, int index) {
        RSItemTable table = lookup(tableKey);
        if (table == null) {
            return null;
        }
        int len = table.getIds().length;
        if (len != table.getStackSizes().length) {
            return null;
        }
        return len > index ? new Entry(index, table.getIds()[index], table.getStackSizes()[index]) : null;
    }

    public static int[] getQuantitiesIn(int tableKey) {
        RSItemTable table = lookup(tableKey);
        if (table == null) {
            return new int[0];
        }
        int len = table.getStackSizes().length;
        if (len != table.getIds().length) {
            return new int[0];
        }
        List<Integer> values = new ArrayList<>();
        for (int value : table.getStackSizes()) {
            if (value > 0) {
                values.add(value);
            }
        }
        int[] valuesNew = new int[values.size()];
        for (int i = 0; i < valuesNew.length; i++) {
            valuesNew[i] = values.get(i);
        }
        return valuesNew;
    }

    public static Entry[] getInventory() {
        return getEntriesIn(INVENTORY);
    }

    public static Entry[] getEquipment() {
        return getEntriesIn(EQUIPMENT);
    }

    public static Entry[] getPriceChecker() {
        return getEntriesIn(PRICE_CHECKER);
    }

    public static Entry[] getBank() { //the items are cached until you enter a new region
        return getEntriesIn(BANK);
    }

    public static Entry[] getExchangeCollection() {
        return getEntriesIn(EXCHANGE_COLLECTION);
    }

    public static Entry[] getVarrockGeneralStore() {
        return getEntriesIn(VARROCK_GENERAL_STORE);
    }

    public static Entry[] getVarrockRuneStore() {
        return getEntriesIn(VARROCK_RUNE_STORE);
    }

    public static Entry[] getVarrockStaffStore() {
        return getEntriesIn(VARROCK_STAFF_STORE);
    }

    public static class Entry implements Identifiable {

        private final int index;
        private final int id;
        private final int quantity;
        private final RSItemDefinition definition;

        public Entry(int index, int id, int quantity) {
            this.index = index;
            this.id = id;
            this.quantity = quantity;
            this.definition = CacheLoader.findItemDefinition(id);
        }

        public int getId() {
            return id;
        }

        public int getQuantity() {
            return quantity;
        }

        public RSItemDefinition getDefinition() {
            return definition;
        }

        public String getName() {
            return definition == null ? null : definition.getName();
        }

        public String[] getActions() {
            return definition == null ? new String[0] : definition.getActions();
        }

        public String[] getGroundActions() {
            return definition == null ? new String[0] : definition.getGroundActions();
        }

        public int getIndex() {
            return index;
        }
    }
}
