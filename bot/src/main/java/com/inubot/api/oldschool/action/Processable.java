/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package com.inubot.api.oldschool.action;

public interface Processable {
    boolean processAction(int opcode, String action);
    boolean processAction(String action);
    String[] getActions();
}
