/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package com.inubot.live.analysis;

import com.inubot.modscript.hook.FieldHook;
import com.inubot.visitor.GraphVisitor;
import com.inubot.visitor.VisitorInfo;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.*;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Dogerina
 * @since 05-07-2015
 */
@VisitorInfo(hooks = {"floorLevel", "x", "y"})
public class SceneOffset extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.ownerless() && cn.constructors().contains("(I)V")
                && cn.getFieldTypeCount() == 1 && cn.fieldCount("I") == 3;
    }

    @Override
    public void visit() {
        visitLocalIfM(new BlockVisitor() {

            private int added = 0;

            @Override
            public boolean validate() {
                return added < 3;
            }

            @Override
            public void visit(Block block) {
                block.tree().accept(new NodeVisitor() {
                    public void visitVariable(VariableNode vn) {
                        if (!vn.hasParent()) {
                            return;
                        }
                        AbstractNode ok = vn.opcode() == ALOAD ? vn.parent() : vn.parent().parent();
                        if (ok == null || !(ok instanceof FieldMemberNode)) {
                            return;
                        }
                        FieldMemberNode fmn = (FieldMemberNode) ok;
                        switch (vn.var()) {
                            case 1:
                                addHook(new FieldHook("floorLevel", fmn.fin()));
                                added++;
                                break;
                            case 2:
                                addHook(new FieldHook("x", fmn.fin()));
                                added++;
                                break;
                            case 3:
                                addHook(new FieldHook("y", fmn.fin()));
                                added++;
                                break;
                        }
                    }
                });
            }
        }, m -> m.desc.equals("(III)V") && m.name.equals("<init>"));
    }
}
