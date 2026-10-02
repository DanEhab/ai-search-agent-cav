package code;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * The general search procedure from the lectures. It works on any GenericSearchProblem,
 * and the strategy decides which node it looks at next. After a search, nodesExpanded()
 * tells how many nodes it had to expand.
 */
public class Search {

    private int nodesExpanded;

    // set when a depth-limited search had to stop at its limit, so there may be more below
    private boolean hitLimit;

    // Searches the problem with the given strategy. Gives back the goal node it ended on
    // (follow its parents for the plan), or null if there is no solution.
    public Node run(GenericSearchProblem problem, Strategy strategy) {
        nodesExpanded = 0;
        switch (strategy) {
            case UC:
                return bestFirst(problem, Comparator.comparingInt(n -> n.pathCost));
            case ID:
                return iterativeDeepening(problem);
            default:
                throw new IllegalArgumentException("unknown strategy " + strategy);
        }
    }

    // how many nodes the last search expanded (the goal node we stop at is not counted).
    // For iterative deepening this adds up all the rounds.
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

    // Iterative deepening: search down to depth 0, then to depth 1, then 2 and so on, until a round
    // finds a goal. That goal needs the fewest actions, but it is not always the cheapest one.
    // If a whole round finishes without ever being stopped by its limit, nothing is left to try.
    private Node iterativeDeepening(GenericSearchProblem problem) {
        for (int limit = 0; ; limit++) {
            hitLimit = false;
            Node start = problem.initialNode();
            Map<State, Integer> shallowest = new HashMap<>();
            shallowest.put(start.state, 0);

            Node goal = depthLimited(problem, start, limit, shallowest);
            if (goal != null) {
                return goal;
            }
            if (!hitLimit) {
                return null;
            }
        }
    }

    // A depth-first search that never goes deeper than the limit. "shallowest" remembers the smallest
    // depth at which each state was reached in this round. Reaching a state again at the same depth or
    // deeper is pointless, because we already tried everything below it with at least as many steps
    // to spare. Only a shallower visit is worth another go. Without this, going left and right over and
    // over would make the search tree gigantic.
    private Node depthLimited(GenericSearchProblem problem, Node node, int limit, Map<State, Integer> shallowest) {
        if (problem.isGoal(node.state)) {
            return node;
        }
        if (node.depth == limit) {
            hitLimit = true;
            return null;
        }
        nodesExpanded++;
        for (Node child : problem.expand(node)) {
            Integer before = shallowest.get(child.state);
            if (before != null && before <= child.depth) {
                continue;
            }
            shallowest.put(child.state, child.depth);
            Node goal = depthLimited(problem, child, limit, shallowest);
            if (goal != null) {
                return goal;
            }
        }
        return null;
    }}
