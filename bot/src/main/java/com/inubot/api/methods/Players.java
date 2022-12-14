/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package com.inubot.api.methods;

import com.inubot.Inubot;
import com.inubot.api.oldschool.Player;
import com.inubot.client.natives.oldschool.RSPlayer;

import java.util.ArrayList;
import java.util.List;

public class Players {

    public static final int MAX_PLAYERS = 2048;
    public static final int LOCAL_PLAYER_INDEX = 2047;

    /**
     * @return The local {@link com.inubot.api.oldschool.Player}
     */
    public static Player getLocal() {
        RSPlayer player = Inubot.getInstance().getClient().getPlayer();
        return player != null ? new Player(player, LOCAL_PLAYER_INDEX) : null;
    }

    public static RSPlayer[] internal() {
        return Inubot.getInstance().getClient().getPlayers();
    }

    /**
     * @return An array of loaded {@link com.inubot.api.oldschool.Player}'s
     */
    public static Player[] getLoaded() {
        List<Player> players = new ArrayList<>();
        RSPlayer[] raws = internal();
        if (raws == null || raws.length == 0) {
            return new Player[0];
        }
        for (int i = 0; i < raws.length; i++) {
            RSPlayer player = raws[i];
            if (player == null) {
                continue;
            }
            players.add(new Player(player, i));
        }
        return players.toArray(new Player[players.size()]);
    }
}
