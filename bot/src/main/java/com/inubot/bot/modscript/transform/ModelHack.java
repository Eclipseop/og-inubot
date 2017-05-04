package com.inubot.bot.modscript.transform;

import com.inubot.Inubot;
import com.inubot.bot.modscript.ModScript;
import com.inubot.bot.modscript.asm.ClassStructure;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.tree.*;
import com.inubot.api.methods.Client;

import java.lang.reflect.Modifier;
import java.util.*;

public class ModelHack implements Transform {

    @Override
    public void inject(Map<String, ClassStructure> classes) {
        ClassNode model = classes.get(ModScript.getClass("Model"));
        List<String> badKeys = new ArrayList<>();
        for (MethodNode mn : model.methods) {
            for (AbstractInsnNode ain : mn.instructions.toArray()) {
                //onCursorUids[onCursorCount++] = ...;
                if (ain.getOpcode() != IASTORE || !matchPrevs(ain, ILOAD, PUTSTATIC, IADD, ICONST_1, DUP, GETSTATIC, GETSTATIC))
                    continue;
                badKeys.add(mn.name + mn.desc);
            }
        }
        for (MethodNode mn : model.methods) {
            if (!badKeys.contains(mn.name + mn.desc) && !mn.desc.contains("[B") && !Modifier.isStatic(mn.access)) {
                for (AbstractInsnNode ain : mn.instructions.toArray()) {
                    if (ain.getOpcode() != IFNONNULL || !matchPrevs(ain, GETFIELD, ALOAD))
                        continue;
                    FieldInsnNode field = (FieldInsnNode) ain.getPrevious();
                    if (field.desc.equals("[B")) {
                        VarInsnNode aload = (VarInsnNode) ain.getPrevious().getPrevious();
                        InsnList setStack = new InsnList();
                        Label label = new Label();
                        LabelNode ln = new LabelNode(label);
                        mn.visitLabel(label);
                        setStack.add(new InsnNode(ICONST_0));
                        //if (Client.MODEL_RENDERING_ENABLED)
                        setStack.add(new FieldInsnNode(GETSTATIC, Client.class.getName().replace('.', '/'), "MODEL_RENDERING_ENABLED", "Z"));
                        setStack.add(new JumpInsnNode(IFNE, ln));
                        setStack.add(new InsnNode(RETURN));
                        setStack.add(ln);
                        mn.instructions.insertBefore(aload, setStack);
                    }
                }
            }
        }
        ClassNode rend = classes.get(ModScript.getClass("Renderable"));
        for (MethodNode mn : rend.methods) {
            if (!Modifier.isStatic(mn.access) && mn.desc.startsWith("(IIIIIIII") && mn.desc.endsWith("V")) {
                int modelVar = -1;
                for (AbstractInsnNode ain : mn.instructions.toArray()) {
                    if (ain.getOpcode() == ASTORE) {
                        VarInsnNode modelStore = (VarInsnNode) ain;
                        modelVar = modelStore.var;
                        break;
                    }
                }
                if (modelVar != -1) {
                    for (AbstractInsnNode ain : mn.instructions.toArray()) {
                        if (ain.getOpcode() == PUTFIELD) {
                            InsnList stack = new InsnList();
                            stack.add(new VarInsnNode(ALOAD, 0));
                            stack.add(new TypeInsnNode(NEW, WRAPPER_PACKAGE + "Model"));
                            stack.add(new InsnNode(DUP));
                            stack.add(new VarInsnNode(ALOAD, 0));
                            stack.add(new VarInsnNode(ALOAD, modelVar));
                            stack.add(new MethodInsnNode(INVOKESPECIAL, WRAPPER_PACKAGE + "Model", "<init>",
                                    "(L" + PACKAGE + "RSRenderable;L" + PACKAGE + "RSModel;)V", false));
                            stack.add(new MethodInsnNode(INVOKEVIRTUAL, rend.name,
                                    "setModel", "(L" + WRAPPER_PACKAGE + "Model;)V", false));
                            mn.instructions.insert(ain, stack);
                        }
                    }
                }
            }
        }
        rend.fields.add(new FieldNode(ACC_PRIVATE, "model", "L" + WRAPPER_PACKAGE + "Model;", null, null));
        MethodNode getModel = new MethodNode(ACC_PUBLIC, "getModel", "()L" + WRAPPER_PACKAGE + "Model;", null, null);
        {
            InsnList get = new InsnList();
            get.add(new VarInsnNode(ALOAD, 0));
            get.add(new FieldInsnNode(GETFIELD, rend.name, "model", "L" + WRAPPER_PACKAGE + "Model;"));
            get.add(new InsnNode(ARETURN));
            getModel.instructions = get;
        }
        rend.methods.add(getModel);

        MethodNode setModel = new MethodNode(ACC_PUBLIC, "setModel", "(L" + WRAPPER_PACKAGE + "Model;)V", null, null);
        {
            InsnList set = new InsnList();
            set.add(new VarInsnNode(ALOAD, 0));
            set.add(new VarInsnNode(ALOAD, 1));
            set.add(new FieldInsnNode(PUTFIELD, rend.name, "model", "L" + WRAPPER_PACKAGE + "Model;"));
            set.add(new InsnNode(RETURN));
            setModel.instructions = set;
        }
        rend.methods.add(setModel);
    }

    private boolean matchPrevs(AbstractInsnNode ain, int... ops) {
        AbstractInsnNode curr = ain;
        for (int i = 0; i < ops.length && (curr = curr.getPrevious()) != null; i++) {
            if (curr.getOpcode() != ops[i]) {
                return false;
            }
        }
        return true;
    }
}
