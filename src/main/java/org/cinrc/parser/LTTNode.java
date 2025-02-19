package org.cinrc.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.cinrc.IRDC;
import org.cinrc.process.ProcessContainer;
import org.cinrc.process.nodes.Label;
import org.cinrc.process.nodes.LabelKey;
import org.cinrc.process.nodes.NodeIDGenerator;
import org.cinrc.process.nodes.TauLabelNode;
import org.cinrc.process.process.Process;
import org.cinrc.process.process.ProcessImpl;
import org.cinrc.util.RCCSFlag;
import org.cinrc.util.SetUtil;

public class LTTNode {

  public HashMap<Label, LTTNode> children;
  public Process internalProcess;
  public HashMap<Label, LTTNode> parents;
  private int currentDepth;

  private int maxDepth;

  public LTTNode(Process internalProcess) {
    this.internalProcess = internalProcess;
    children = new HashMap<>();
    currentDepth = 0;
    parents = new HashMap<>();
  }

  public HashMap<Label, LTTNode> getOutgoingEdges() {
    return (HashMap<Label, LTTNode>) children.clone();
  }

  public void setParent(Label label, LTTNode n) {
    this.parents.put(label, n);
    this.currentDepth = n.getCurrentDepth() + 1;
  }

  public void addChild(Label l, Process pr) {
    LTTNode node = new LTTNode(pr);
    addChild(l, node);
  }

  public void addChild(Label l, LTTNode node){
    node.setParent(l, this);
    node.setCurrentDepth(currentDepth + 1);
    node.echoDepth(0);
    children.put(l, node);
  }

  public int getCurrentDepth() {
    return currentDepth;
  }

  public void setCurrentDepth(int currentDepth) {
    this.currentDepth = currentDepth;
  }


  /**
   * Ensures that the given node's edges are a subset of this node's edges.
   * This operation does not guarantee the reverse. Note that in this context, because
   * Label#isEquivalent does not care about complements, a is equivalent to 'a
   * and thus a.P can simulate 'a.P
   *
   * @param node LTTNode
   * @return True if this node this can simulate all edges of given node
   */
  public boolean canSimulate(LTTNode node) {
    Collection<Label> keySet = children.keySet();
    if (node.internalProcess instanceof ProcessImpl && internalProcess instanceof ProcessImpl
        && !IRDC.config.contains(RCCSFlag.PROCESS_NAMES_EQUIVALENT)) {
      if (!node.internalProcess.hasSameProcess(this.internalProcess)) {
        return false;
      }
    }

    for (Map.Entry<Label, LTTNode> entry : node.children.entrySet()) { //for every edge of compared
      int match = 0;
      Label compareLabel = entry.getKey();
      for (Label ourLabel : keySet) { //iterate through our edges
        if (ourLabel.isEquivalent(compareLabel)) { //can our edge do what compared edge can?
          if (children.get(ourLabel).canSimulate(node.children.get(compareLabel))) {
            match = 1;
          }
        }
      }
      if (match == 0) {
        IRDC.log("%s no match %s",internalProcess.represent(), entry.getValue().internalProcess.represent() );
        return false; //If at the end and turns out cant simulate return false
      }
    }
    return true;
  }

  /**
   * Function is called on each leaf node. Leaf node will start with depth 0, and
   * recursively call parent nodes with depth+1, until reaching the top. At each step
   * up, the parent node will recalculate its max depth based on given depth.
   * Sort of like a callback function to calculate depth with a complexity of O(n), as opposed
   * to O(n^2)   ((I think))
   *
   * @param depth Depth of current node
   */
  protected void echoDepth(int depth) {
    if (depth > maxDepth) {
      maxDepth = depth;
    }
    if (!parents.isEmpty()) {
      for (LTTNode n : parents.values()) {
        n.echoDepth(++depth);
      }
    }
  }

  public LTTNode getAncestor() throws Exception {
    if (parents.keySet().size() > 1)
      throw new Exception("Process has more than one parent!");
    else if (parents.keySet().size() == 0)
      return this;
    for (LTTNode node : parents.values())
      return node.getAncestor();

    return this;
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  /**
   * Enumerate all actionable labels on the parent process, and
   * extend the tree by adding new nodes as children to this node from each label,
   * and recursively call this function on all child nodes if recurse is true. This method does not enumerate
   * backwards. That is to say, given a process [k1]a.[k2]b.P, it will not regenerate a.b.P.
   * See LTTNode#regenerate for that.
   *
   * @param recurse Whether or not to enumerate children as well
   */
  public void enumerate(boolean recurse) {
    ProcessContainer pc = new ProcessContainer(internalProcess.clone());
    //For every actionable label in current node,
    for (Label l : pc.getActionableLabels()) {
      if (!(l instanceof LabelKey)) {
        Process tmp = pc.getProcess().clone();

        pc.act(l); //Act on that label and make a new node with that child process (clone)
        Process z = pc.getProcess().clone();
        addChild(l, z);

        if (!z.getKey().from.isEquivalent(l)){

          throw new CCSParserException(tmp.represent() + " -"+ l.represent() +"-> " + z.represent() + ", but found key "
              + z.getKey());
        }
        pc.reverseLastAction(); //Then reverse and next label.

      }/* else { //This should be all we need to implement reversibility
                Label originalLabel = ((LabelKey) l).from;
                pc.act(l); //Reverse along actionable key
                for (Map.Entry<Label, LTTNode> n : parents.entrySet())
                //TODO: Check pc.p already exists in parents, else add it.
                {
                  if ()
                }
                if (originalLabel instanceof TauLabelNode) {
                    ((TauLabelNode) originalLabel).consumeLeft = false;
                    ((TauLabelNode) originalLabel).consumeRight = false;
                }
                pc.act(originalLabel); //Revert back
      }*/
    }
    if (recurse) {
      for (LTTNode child : children.values()) {
        child.enumerate(true);
      }
    }
  }

  /**
   * TODO: Enumerate, but backwards
   * @return
   */
  public LTTNode regenerate(){
    if (internalProcess.hasKey()){
      Process p = internalProcess.clone();
      Process p2 = p.act(internalProcess.getKey());
      LTTNode node = new LTTNode(p2);
      node.addChild(internalProcess.getKey().from, this);

    }
    for (Map.Entry<Label, LTTNode> entry : parents.entrySet()){
      entry.getValue().regenerate();
    }

    return this;
  }

  public boolean isLeafNode() {
    return children.isEmpty();
  }

  /**
   * Creates set of all fully reduced processes that consider this process
   * an 'ancestor' (but not necessarily the top level ancestor).
   * e.g: A list of processes that can be obtained by any sequence of actions.
   *
   * @return HashSet of LTTNodes that represent leaf nodes
   */
  public Collection<LTTNode> getLeafChildren() {
    HashSet<LTTNode> nodeSet = new HashSet<>();
    for (LTTNode node : children.values()) {
      if (node.isLeafNode()) {
        nodeSet.add(node);
      } else {
        nodeSet.addAll(node.getLeafChildren());
      }
    }
    return nodeSet;
  }

  /**
   * Recurse through parent processes to determine which sequence of actions
   * was taken to reach this process from the ancestor parent.
   *
   * @return ArrayList of labels that it's ancestor must have taken in order to reach this.
   */
  public ArrayList<Label> calculatePath() {
    ArrayList<Label> al = new ArrayList<>();
    for (Map.Entry<Label, LTTNode> n : parents.entrySet()) {
      al.addAll(n.getValue().calculatePath());
      al.add(n.getKey());
      break; //TODO: What do we do if parents size is bigger than one?
    }
    return al;
  }

  public void manageHistory(){
    //TODO: CHeck all nodes. If any two are equal, create one single node and create union of parents
    // a|b
    // a -> b -> [a]|[b]
    // b -> a -> [a]|[b]
    //   a|b             a|b
    //  /   \            /   \
    //[a]|b a|[b]  <--  [a]|b a|[b]
    //  \   /            /       \
    // [a]|[b]          [a]|[b]   [a]|[b]
    /*
    node a is equal node b
    create node c.
    set node c new Parents(a.parents, b.parents)
    manage children and grandparents
     */
  }

  public String toString() {
    return print(new StringBuilder(500), "", "");
    //return internalProcess.represent();
  }


  //https://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram-in-java
  private String print(final StringBuilder buffer, final String prefix,
                       final String childrenPrefix) {
    buffer.append(prefix);
    buffer.append(
        String.format("%s (Depth: %d), %s",
            internalProcess.represent(),
            this.currentDepth,
            calculatePath()));
    buffer.append('\n');
    for (Iterator<Map.Entry<Label, LTTNode>> it = children.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<Label, LTTNode> entry = it.next();
      LTTNode next = entry.getValue();
      Label l = entry.getKey();
      if (it.hasNext()) {
        next.print(buffer, childrenPrefix
            + "├─" + l + "─ ", childrenPrefix + "│   ");
      } else {
        next.print(buffer, childrenPrefix
            + "└─" + l + "─ ", childrenPrefix + "    ");
      }
    }
    return buffer.toString();
  }

  /**
   * //TODO: Basically, check if the given node is EQUIVALENT, not BISIMILAR
   * @param node
   * @return
   */
  public boolean equals(LTTNode node, boolean hpb){
    return internalProcess.equals(node.internalProcess, hpb);
  }

  @Override
  public boolean equals(Object o){
    if (!(o instanceof LTTNode))
      return false;
    return equals((LTTNode)o, false);
  }

}
