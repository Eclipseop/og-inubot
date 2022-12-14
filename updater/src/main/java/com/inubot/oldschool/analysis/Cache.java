package com.inubot.oldschool.analysis;

import com.inubot.visitor.GraphVisitor;
import com.inubot.visitor.VisitorInfo;
import org.objectweb.asm.tree.ClassNode;

@VisitorInfo(hooks = {"table", "queue"})
public class Cache extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.getFieldTypeCount() == 4 && cn.fieldCount(desc("DoublyLinkedNode")) == 1 &&
                cn.fieldCount(desc("NodeTable")) == 1;
    }

    @Override
    public void visit() {
        add("table", cn.getField(null, desc("NodeTable")));
        add("queue", cn.getField(null, desc("Queue")));
    }
}
