package code;

import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * The general search procedure from the lectures. It works on any GenericSearchProblem,
 * and the strategy decides which node it looks at next. After a search, nodesExpanded()
 * tells how many nodes it had to expand.
 */
public class Search {

    private int nodesExpanded;

    // Searches the problem with the given strategy. Gives back the goal node it ended on
    // (follow its parents for the plan), or null if there is no solution.
    public Node run(GenericSearchProblem problem, Strategy strategy) {
        nodesExpanded = 0;
        switch (strategy) {
            case UC:
                return bestFirst(problem, Comparator.comparingInt(n -> n.pathCost));
            default:
                throw new IllegalArgumentException("unknown strategy " + strategy);
        }
    }

    // how many nodes the last search expanded (the goal node we stop at is not counted)
    public int nodesExpanded() {
        return nodesExpanded;
    }

    // The loop from Lecture 2: take the node that comes first in the given order, stop if it is a goal,
    // otherwise queue its children. We only queue a child if we have not seen its state before.
    // That is safe because reaching a state always costs the same, whatever path we took: the cost
    // only depends on the lives and the energy used, and both of those are part of the state.
    private Node bestFirst(GenericSearchProblem problem, Comparator<Node> order) {
        PriorityQueue<Node> queue = new PriorityQueue<>(order);
        Set<State> seen = new HashSet<>();

        Node start = problem.initialNode();
        queue.add(start);
        seen.add(start.state);

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (problem.isGoal(node.state)) {
                return node;
            }
            nodesExpanded++;
            for (Node child : problem.expand(node)) {
                if (seen.add(child.state)) {
                    queue.add(child);
                }
            }
        }
        return null;
    }
}
