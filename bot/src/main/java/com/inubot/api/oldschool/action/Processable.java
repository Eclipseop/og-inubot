/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package com.inubot.api.oldschool.action;

/**
 * @author unsigned
 * @since 20-04-2015
 */
public interface Processable {
    void processAction(int opcode, String action);
    void processAction(String action);
}