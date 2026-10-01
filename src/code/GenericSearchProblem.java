package code;

import java.util.ArrayList;
import java.util.List;

/**
 * What every search problem is made of, the way the lectures define it: the state we start in,
 * the operators we can use, a goal test and a cost for every step. A search strategy only needs
 * these pieces, so it can work on any problem that fills them in.
 *
 * The state space (all the states we can reach) is never written down. We find it as we go,
 * by expanding nodes.
 */
public abstract class GenericSearchProblem {

    // the state we start in
    public abstract State initialState();

    // all the actions that exist in this problem (not every one is allowed in every state)
    public abstract Action[] operators();

    // The state we get by doing an action in a state, or null if the action is not allowed there.
    public abstract State result(State state, Action action);

    // is this a state we are looking for?
    public abstract boolean isGoal(State state);

    // what the single step from one state to the next one costs
    public abstract int stepCost(State before, State after);

    // the node a search starts from
    public Node initialNode() {
        return new Node(initialState());
    }

    // All the nodes we can reach from this one in a single step, one for every operator that is allowed.
    public List<Node> expand(Node node) {
        List<Node> children = new ArrayList<>();
        for (Action action : operators()) {
            State next = result(node.state, action);
            if (next != null) {
                children.add(new Node(node, action, next, stepCost(node.state, next)));
            }
        }
        return children;
    }
}