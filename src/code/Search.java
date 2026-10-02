package code;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.ToIntFunction;

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
                return bestFirst(problem, n -> n.pathCost);
            case ID:
                return iterativeDeepening(problem);
            case AS:
                return bestFirst(problem, n -> n.pathCost + problem.heuristic(n.state));
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
    // A guess that only looks at the state does not change that.
    // The priority of a node (the smaller, the sooner it is looked at) is worked out once, when the
    // node is queued. Asking for the guess again on every comparison inside the queue made A* slow.
    // A node whose priority is HOPELESS or more is not queued at all: that is how the guess of A* says that
    // no goal can be reached from there. Uniform cost has no guess, so it never meets this.
    private Node bestFirst(GenericSearchProblem problem, ToIntFunction<Node> priority) {
        PriorityQueue<Queued> queue = new PriorityQueue<>(Comparator.comparingInt(q -> q.priority));
        Set<State> seen = new HashSet<>();

        Node start = problem.initialNode();
        queue.add(new Queued(start, priority.applyAsInt(start)));
        seen.add(start.state);

        while (!queue.isEmpty()) {
            Node node = queue.poll().node;
            if (problem.isGoal(node.state)) {
                return node;
            }
            nodesExpanded++;
            for (Node child : problem.expand(node)) {
                if (seen.add(child.state)) {
                    int rank = priority.applyAsInt(child);
                    if (rank < GenericSearchProblem.HOPELESS) {
                        queue.add(new Queued(child, rank));
                    }
                }
            }
        }
        return null;
    }

    // Iterative deepening: search down to depth 0, then to depth 1, then 2 and so on, until a round
    // finds a goal. That goal needs the fewest actions, but it is not always the cheapest one.
    // If a whole round finishes without ever being stopped by its limit, nothing is left to try.
    // "known" remembers every state we have met, over all the rounds, with the fewest actions it takes to get
    // there. That number never changes once we know it, so every new round can make use of it.
    private Node iterativeDeepening(GenericSearchProblem problem) {
        Node start = problem.initialNode();
        Map<State, Known> known = new HashMap<>();
        known.put(start.state, new Known(0, 0));
        for (int limit = 0; ; limit++) {
            hitLimit = false;
            known.get(start.state).lastRound = limit;

            Node goal = depthLimited(problem, start, limit, known);
            if (goal != null) {
                return goal;
            }
            if (!hitLimit) {
                return null;
            }
        }
    }

    // A depth-first search that never goes deeper than the limit. Only the shortest way to a state is worth
    // following: arriving by a longer way is pointless, because the shortest way has more steps to spare
    // for everything below the state. So we skip a state if we know a shorter way to it, and, since a
    // state needs only one visit per round, if we were there already in this round.
    // A state we have never met before can only be reached at the limit (the earlier rounds would have
    // found it otherwise), so the depth we first see it at is the shortest one.
    // Without all this, going left and right over and over would make the search tree gigantic.
    private Node depthLimited(GenericSearchProblem problem, Node node, int limit, Map<State, Known> known) {
        if (problem.isGoal(node.state)) {
            return node;
        }
        if (node.depth == limit) {
            hitLimit = true;
            return null;
        }
        nodesExpanded++;
        for (Node child : problem.expand(node)) {
            Known before = known.get(child.state);
            if (before == null) {
                known.put(child.state, new Known(child.depth, limit));
            } else if (before.depth < child.depth || before.lastRound == limit) {
                continue;
            } else {
                before.lastRound = limit;
            }
            Node goal = depthLimited(problem, child, limit, known);
            if (goal != null) {
                return goal;
            }
        }
        return null;
    }

    // What iterative deepening knows about a state: the fewest actions that lead to it, and the last
    // round in which it was visited.
    private static class Known {
        final int depth;
        int lastRound;

        Known(int depth, int lastRound) {
            this.depth = depth;
            this.lastRound = lastRound;
        }
    }

    // a node waiting in the queue, together with the priority it was queued with
    private static class Queued {
        final Node node;
        final int priority;

        Queued(Node node, int priority) {
            this.node = node;
            this.priority = priority;
        }
    }
}
