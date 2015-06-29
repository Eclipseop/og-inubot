/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package com.inubot.bot.modscript.transform;

import com.inubot.bot.modscript.asm.ClassStructure;
import jdk.internal.org.objectweb.asm.tree.*;
import com.inubot.client.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * @author unsigned
 * @since 27-04-2015
 */
public class MessageCallback implements Transform {

    private static final Logger logger = LoggerFactory.getLogger(MessageCallback.class);

    @Override
    public void inject(Map<String, ClassStructure> classes) {
        for (ClassNode cn : classes.values()) {
            for (MethodNode mn : cn.methods) {
                if (!Modifier.isStatic(mn.access) || !mn.desc.endsWith("V")
                        || !mn.desc.startsWith("(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;"))
                    continue;
                InsnList stack = new InsnList();
                stack.add(new VarInsnNode(ILOAD, 0));
                stack.add(new VarInsnNode(ALOAD, 1));
                stack.add(new VarInsnNode(ALOAD, 2));
                stack.add(new VarInsnNode(ALOAD, 3));
                stack.add(new MethodInsnNode(INVOKESTATIC, Callback.class.getName().replace('.', '/'),
                        "messageReceived", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false));
                mn.instructions.insertBefore(mn.instructions.getFirst(), stack);
                logger.debug("Injected message @ " + cn.name + "." + mn.name + mn.desc);
            }
        }
    }
}
