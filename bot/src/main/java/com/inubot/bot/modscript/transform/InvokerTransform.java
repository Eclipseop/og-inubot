/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package com.inubot.bot.modscript.transform;

import com.inubot.bot.modscript.ModScript;
import com.inubot.bot.modscript.asm.ClassStructure;
import com.inubot.bot.modscript.hooks.InvokeHook;
import jdk.internal.org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author unsigned
 * @since 20-04-2015
 */
public class InvokerTransform implements Transform {

    @Override
    public void inject(Map<String, ClassStructure> classes) {
        addUnpackInvoker(classes.get(ModScript.getClass("ReferenceTable")));
        addGetInvoker("ObjectDefinition", classes.get("client"));
        addGetInvoker("NpcDefinition", classes.get("client"));
        addGetInvoker("ItemDefinition", classes.get("client"));
        addSpriteInvoker("Sprite", "loadItemSprite", classes.get("client"));
        addDrawStringInvoker("Font", "drawString", classes.get(ModScript.getClass("Font")));

        //keeeeeeeeeeek
        outer:
        for (ClassNode cn : classes.values()) {
            if (cn.superName.equals(ModScript.getClass("DoublyLinkedNode"))) {
                int sia = 0, spi = 0;
                for (FieldNode fn : cn.fields) {
                    if (!Modifier.isStatic(fn.access))
                        continue outer;
                    if (fn.desc.equals("[I"))
                        sia++;
                    if (Modifier.isPublic(fn.access) && fn.desc.equals("I"))
                        spi++;
                }
                if (sia != 1 || spi < 4)
                    continue;
                outer0:
                for (MethodNode mn : cn.methods) {
                    if (Modifier.isStatic(mn.access) && mn.desc.startsWith("(IIIII") && mn.desc.endsWith("V")) {
                        List<String> calls = new ArrayList<>();
                        int iconst_1s = 0;
                        for (AbstractInsnNode ain : mn.instructions.toArray()) {
                            if (ain.getOpcode() == ICONST_1) {
                                iconst_1s++;
                            } else if (ain.getOpcode() == INVOKESTATIC) {
                                MethodInsnNode min = (MethodInsnNode) ain;
                                calls.add(min.owner + "." + min.name + min.desc);
                            }
                        }
                        if (calls.size() != 4 || iconst_1s != 2)
                            continue;
                        for (String call : calls) {
                            int count = 0;
                            for (String call0 : calls) {
                                if (call0.equals(call))
                                    count++;
                            }
                            if (count != 2)
                                continue outer0;
                        }
                        ClassNode client = classes.get("client");
                        MethodNode invoker = new MethodNode(ACC_PUBLIC, "drawRectangle", "(IIIII)V", null, null);
                        invoker.instructions.add(new VarInsnNode(ILOAD, 1));
                        invoker.instructions.add(new VarInsnNode(ILOAD, 2));
                        invoker.instructions.add(new VarInsnNode(ILOAD, 3));
                        invoker.instructions.add(new VarInsnNode(ILOAD, 4));
                        invoker.instructions.add(new VarInsnNode(ILOAD, 5));
                        invoker.instructions.add(new MethodInsnNode(INVOKESTATIC, cn.name, mn.name, mn.desc, false));
                        invoker.instructions.add(new InsnNode(RETURN));
                        client.methods.add(invoker);
                        break outer;
                    }
                }
            }
        }
    }

    private void addDrawStringInvoker(String defined, String method, ClassNode target) {
        MethodNode mn = new MethodNode(ACC_PUBLIC, "drawString", "(Ljava/lang/String;II)V", null, null);
        InvokeHook ih = ModScript.getInvoke(defined + "#" + method);
        mn.instructions.add(new VarInsnNode(ALOAD, 0));
        mn.instructions.add(new VarInsnNode(ALOAD, 1));
        mn.instructions.add(new VarInsnNode(ILOAD, 2));
        mn.instructions.add(new VarInsnNode(ILOAD, 3));
        if (ih.predicate != Integer.MAX_VALUE) {
            mn.instructions.add(new LdcInsnNode(ih.predicate));
        }
        mn.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, ih.clazz, ih.method, ih.desc, false));
        mn.instructions.add(new InsnNode(RETURN));
        target.methods.add(mn);
    }

    private void addUnpackInvoker(ClassNode node) {
        MethodNode mn = new MethodNode(ACC_PUBLIC, "unpack", "(II[I)[B", null, null);
        InvokeHook ih = ModScript.getInvokeHook("ReferenceTable#unpack");
        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(ALOAD, 0));
        instructions.add(new VarInsnNode(ILOAD, 1));
        instructions.add(new VarInsnNode(ILOAD, 2));
        instructions.add(new VarInsnNode(ALOAD, 3));
        if (ih.predicate != Integer.MAX_VALUE) {
            instructions.add(new LdcInsnNode(ih.predicate));
        }
        instructions.add(new MethodInsnNode(INVOKEVIRTUAL, ih.clazz, ih.method, ih.desc, false));
        instructions.add(new InsnNode(ARETURN));
        mn.instructions = instructions;
        node.methods.add(mn);
    }

    private void addGetInvoker(String defined, ClassNode client) {
        MethodNode mn = new MethodNode(ACC_PUBLIC, "load" + defined, "(I)L" + PACKAGE + "RS" + defined + ";", null, null);
        InvokeHook ih = ModScript.getInvoke("Client#load" + defined);
        mn.instructions.add(new VarInsnNode(ILOAD, 1));
        if (ih.predicate != Integer.MAX_VALUE)
            mn.instructions.add(new LdcInsnNode(ih.predicate));
        mn.instructions.add(new MethodInsnNode(INVOKESTATIC, ih.clazz, ih.method, ih.desc, false));
        mn.instructions.add(new InsnNode(ARETURN));
        client.methods.add(mn);
    }

    private void addSpriteInvoker(String defined, String name, ClassNode client) {
        MethodNode mn = new MethodNode(ACC_PUBLIC, name, "(IIIIIZ)L" + PACKAGE + "RS" + defined + ";", null, null);
        InvokeHook ih = ModScript.getInvoke("Client#" + name);
        mn.instructions.add(new VarInsnNode(ILOAD, 1));
        mn.instructions.add(new VarInsnNode(ILOAD, 2));
        mn.instructions.add(new VarInsnNode(ILOAD, 3));
        mn.instructions.add(new VarInsnNode(ILOAD, 4));
        mn.instructions.add(new VarInsnNode(ILOAD, 5));
        mn.instructions.add(new VarInsnNode(ILOAD, 6));
        if (ih.predicate != Integer.MAX_VALUE)
            mn.instructions.add(new LdcInsnNode(ih.predicate));
        mn.instructions.add(new MethodInsnNode(INVOKESTATIC, ih.clazz, ih.method, ih.desc, false));
        mn.instructions.add(new InsnNode(ARETURN));
        client.methods.add(mn);
    }
}
