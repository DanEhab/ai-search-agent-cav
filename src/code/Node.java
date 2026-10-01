package code;

import java.util.LinkedList;
import java.util.List;

/**
 * One node of the search tree. It holds a state together with the trail that led to it:
 * the node we came from, the action we took, how many steps deep we are and what it all cost.
 * A node is not the same thing as a state: the same state can show up in many nodes,
 * reached by different paths with different costs. That is why nodes are never compared
 * with equals, only the states inside them are.
 */
public class Node {

    public final State state;
    public final Node parent;     // null for the root
    public final Action action;   // the action that led here from the parent, null for the root
    public final int depth;       // number of steps from the root
    public final int pathCost;    // cost of all the steps from the root to here

    // the root of a search tree: the starting state and nothing else
    public Node(State state) {
        this.state = state;
        this.parent = null;
        this.action = null;
        this.depth = 0;
        this.pathCost = 0;
    }

    // A child: the node we get by doing an action in the parent and landing in the given state.
    // stepCost is what that one step cost, and it gets added to the cost of the parent.
    public Node(Node parent, Action action, State state, int stepCost) {
        this.state = state;
        this.parent = parent;
        this.action = action;
        this.depth = parent.depth + 1;
        this.pathCost = parent.pathCost + stepCost;
    }

    // The actions that lead from the root to this node, in the order they were taken.
    // We walk back through the parents, so the last action is the first one we meet.
    public List<Action> plan() {
        LinkedList<Action> steps = new LinkedList<>();
        for (Node n = this; n.parent != null; n = n.parent) {
            steps.addFirst(n.action);
        }
        return steps;
    }

    // the plan written the way the answer needs it, like "right,climbup,collect"
    public String planText() {
        StringBuilder text = new StringBuilder();
        for (Action step : plan()) {
            if (text.length() > 0) {
                text.append(",");
            }
            text.append(step);
        }
        return text.toString();
    }

    @Override
    public String toString() {
        return "Node(depth=" + depth + ", pathCost=" + pathCost + ", action="
                + (action == null ? "none" : action) + ", " + state + ")";
    }
}
