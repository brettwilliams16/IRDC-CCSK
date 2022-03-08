package me.gmx.process.process;

import me.gmx.parser.CCSGrammar;
import me.gmx.process.ProcessTemplate;
import me.gmx.process.nodes.LabelNode;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

public abstract class ComplexProcess implements Process{

    Process left, right;
    static CCSGrammar operator;

    public Collection<Process> getChildren(){
        return Set.of(left, right);
    }

    @Override
    public Collection<LabelNode> getActionableLabels(){
        Set<LabelNode> s = Collections.emptySet();
        s.addAll(left.getActionableLabels());
        s.addAll(right.getActionableLabels());
        return s;
    }

    public boolean isInit(){
        return !(left == null || right == null);
    }

    public LinkedList<Process> init(LinkedList<Process> template){
        for (int i = 0; i < template.size(); i++) {

            if (template.get(i) == this) {
                if (left == null) {
                    System.out.println(String.format("Using %s to init left ", template.get(i - 1).origin()));
                    left = template.remove(i - 1);
                    i--;
                }
                if (right == null) {
                    System.out.println(String.format("Using %s to init right ", template.get(i + 1).origin()));
                    right = template.remove(i + 1);

                }
            }
        }

        //dont think is necessary
        return template;
    }

    @Override
    public String origin(){
        return left.origin() + this.operator + right.origin();
    }

}
