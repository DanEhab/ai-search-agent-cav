package code;

/**
 * The cave explorer as a search problem. The generic problem knows how to expand a node;
 * here we only say what a cave looks like: where we start, which actions exist, when we have
 * won and what a step costs. The cave rules themselves live in CaveRules.
 */
public class CaveExplorer extends GenericSearchProblem {

    private final CaveMap cave;
    private final CaveRules rules;

    public CaveExplorer(CaveMap cave) {
        this.cave = cave;
        this.rules = new CaveRules(cave);
    }

    @Override
    public State initialState() {
        return State.initial(cave);
    }

    @Override
    public Action[] operators() {
        return Action.values();
    }

    @Override
    public State result(State state, Action action) {
        return rules.apply(state, action);
    }

    @Override
    public boolean isGoal(State state) {
        return rules.isGoal(state);
    }

    @Override
    public int stepCost(State before, State after) {
        return rules.stepCost(before, after);
    }

    // We have no real guess yet. Saying 0 means "no idea how far the goal is", and with that A* does
    // exactly what uniform cost search does.
    @Override
    public int heuristic(State state) {
        return 0;
    }

    // Reads the cave, searches it with the strategy ("UC", "ID" or "AS") and gives back the answer in
    // the format the assignment wants: plan;lives;energy;nodes. If the cave cannot be won it is "No Solution".
    public static String solve(String initString, String strategy) {
        Strategy chosen = Strategy.fromText(strategy);
        CaveExplorer problem = new CaveExplorer(new CaveMap(initString));
        Search search = new Search();

        Node goal = search.run(problem, chosen);
        if (goal == null) {
            return "No Solution";
        }
        return goal.planText() + ";" + problem.rules.livesUsed(goal.state) + ";"
                + problem.rules.energyUsed(goal.state) + ";" + search.nodesExpanded();
    }

}