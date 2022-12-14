/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package com.inubot.api.methods;

import com.inubot.api.oldschool.action.ActionOpcodes;
import com.inubot.api.util.Time;

public class LogoutTab {

    private static final int BASE_COMP_HASH = 4521996;

    //TODO pretty sure this broke on resizable update
    public static void switchWorlds(int world) {
        if (world > 300)
            world -= 300;
        Client.processAction(1, -1, 11927557, ActionOpcodes.COMPONENT_ACTION, "World switcher", "", 50, 50);
        Client.processAction(1, -1, BASE_COMP_HASH + world, ActionOpcodes.COMPONENT_ACTION, "Switch", "", 50, 50);
        Time.sleep(300);
        Client.processAction(0, 1, 14352384, ActionOpcodes.BUTTON_DIALOG, "Continue", "", 50, 50);
        //^ may not be needed if u chose not to ask again
    }

    public static void logout() {
        Client.processAction(1, -1, 11927558, ActionOpcodes.COMPONENT_ACTION, "", "", 50, 50);
    }
}
