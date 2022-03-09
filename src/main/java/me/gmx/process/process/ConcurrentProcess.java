package me.gmx.process.process;

import me.gmx.parser.CCSGrammar;
import me.gmx.process.nodes.LabelNode;

import java.util.Collection;
import java.util.Set;

public class ConcurrentProcess extends ComplexProcess{


    public static String representString = "|";
    /**
     * @param left - left side me.gmx.process
     * @param right - right side me.gmx.process
     */
    public ConcurrentProcess(Process left, Process right) {
        this.left = left;
        this.right = right;
        operator = CCSGrammar.OP_CONCURRENT;
    }


    @Override
    public boolean canAct(LabelNode label) {
        return left.canAct(label) || right.canAct(label);
    }

    @Override
    public Process act(LabelNode label) {
        if (left.canAct(label))
            left = left.act(label);
        if (right.canAct(label))
            right = right.act(label);

        return this;
    }

    @Override
    public String represent() {
        return String.format("[Concurrent(%s, %s)]",left.represent(),right.represent());
    }

    @Override
    public Collection<Process> getChildren() {
        return Set.of(left, right);
    }


}
