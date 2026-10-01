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

    public static String solve(String initString, String strategy) {

        return "";

    }

}