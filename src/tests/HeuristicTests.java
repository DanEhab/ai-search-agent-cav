package tests;

import code.CaveExplorer;
import code.CaveMap;
import code.GenericSearchProblem;
import code.Node;
import code.Search;
import code.State;
import code.Strategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the guess that A* uses (the heuristic): what a good guess does for the search,
 * and the rules every guess has to follow.
 */
public class HeuristicTests {

    private static final int NO_WAY = Integer.MAX_VALUE;

    private static final String PDF_EXAMPLE = "3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;";
    private static final String SIX_BY_SEVEN =
            "6,7;0,5;99,5;6,8,10,12,14,16,10;7,0,0,0,0,0,12;5,0,0,0,0,0,14;4,0,0,0,0,0,16;3,6,8,45,0,0,20;2,0,0,0,0,0,14;6,0;0,2;";
    private static final String BIG_CAVE =
            "7,8;0,1;120,5;0,3,0,0,8,4,0,2;4,9,5,0,2,0,7,0;0,1,8,8,6,9,6,0;0,2,7,3,4,4,0,2;7,0,1,3,2,0,3,9;7,0,2,6,0,1,4,0;5,0,9,4,4,3,8,5;3,2,5,6;1,0,5,3,0,4;";
    // a small cave where the shortest plan is not the cheapest
    private static final String SMALL_CAVE = "3,3;2,0;200,3;0,1,8;0,3,9;0,3,8;2,1;2,2;";

    // Works out what it really costs to win from a state: the cheapest way on from there (every step
    // plus what is left after it). NO_WAY means that nobody can win from that state any more.
    // A cave never leads back to an earlier state, so this always comes to an end.
    private static int costLeft(GenericSearchProblem problem, State state, Map<State, Integer> known) {
        Integer done = known.get(state);
        if (done != null) {
            return done;
        }
        int best = NO_WAY;
        if (problem.isGoal(state)) {
            best = 0;
        } else {
            for (Node child : problem.expand(new Node(state))) {
                int rest = costLeft(problem, child.state, known);
                if (rest != NO_WAY) {
                    best = Math.min(best, problem.stepCost(state, child.state) + rest);
                }
            }
        }
        known.put(state, best);
        return best;
    }

    // what it really costs to win from every state that we can reach
    private static Map<State, Integer> realCostLeft(GenericSearchProblem problem) {
        Map<State, Integer> known = new HashMap<>();
        costLeft(problem, problem.initialState(), known);
        return known;
    }

    // a cave problem whose guess is exactly what is really left
    private static class PerfectGuess extends CaveExplorer {
        private final Map<State, Integer> left;

        PerfectGuess(CaveMap cave, Map<State, Integer> left) {
            super(cave);
            this.left = left;
        }

        @Override
        public int heuristic(State state) {
            Integer real = left.get(state);
            return (real == null || real == NO_WAY) ? 1000000 : real;
        }
    }

    // ---------------------------------------------------------------- the helper itself

    @Test
    public void whatIsLeftAtTheStartIsTheCostOfTheCheapestPlan() {
        // the helper has to be right, because the other tests lean on it
        for (String text : new String[] {PDF_EXAMPLE, SIX_BY_SEVEN, SMALL_CAVE}) {
            CaveExplorer problem = new CaveExplorer(new CaveMap(text));
            Node cheapest = new Search().run(problem, Strategy.UC);

            assertEquals(cheapest.pathCost, realCostLeft(problem).get(problem.initialState()), text);
        }
    }

    // ---------------------------------------------------------------- what a good guess does

    @Test
    public void aPerfectGuessLeadsAlmostStraightToTheGoal() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            for (String text : new String[] {PDF_EXAMPLE, SIX_BY_SEVEN, BIG_CAVE}) {
                CaveMap cave = new CaveMap(text);
                Map<State, Integer> left = realCostLeft(new CaveExplorer(cave));

                Search uniform = new Search();
                Node cheapest = uniform.run(new CaveExplorer(cave), Strategy.UC);
                Search guided = new Search();
                Node found = guided.run(new PerfectGuess(cave, left), Strategy.AS);

                // the same cheapest plan, but A* hardly looks at anything else
                assertEquals(cheapest.pathCost, found.pathCost, text);
                assertTrue(guided.nodesExpanded() < uniform.nodesExpanded() / 10,
                        "A* expanded " + guided.nodesExpanded() + " nodes, uniform cost " + uniform.nodesExpanded());
            }
        });
    }

    @Test
    public void aStarAddsTheGuessAsItIsAndDoesNotScaleIt() {
        // In these two caves, a perfect guess that was counted twice would lead A* to a plan that costs
        // a bit more than the cheapest one. Added once, the perfect guess finds the cheapest plan.
        String[] caves = {"3,3;2,0;200,0;9,3,6;7,1,9;6,9,8;0,2;1,2;", "3,3;2,1;200,2;6,9,8;2,1,1;1,9,6;1,0;0,2;"};
        int[] cheapest = {2019, 1016};

        for (int i = 0; i < caves.length; i++) {
            CaveMap cave = new CaveMap(caves[i]);
            Map<State, Integer> left = realCostLeft(new CaveExplorer(cave));
            Node found = new Search().run(new PerfectGuess(cave, left), Strategy.AS);

            assertEquals(cheapest[i], found.pathCost, caves[i]);
        }
    }

    // ---------------------------------------------------------------- the rules for every guess

    @Test
    public void theGuessIsNeverNegativeAndZeroInAGoal() {
        for (String text : new String[] {PDF_EXAMPLE, SIX_BY_SEVEN, SMALL_CAVE}) {
            CaveExplorer problem = new CaveExplorer(new CaveMap(text));

            for (State state : realCostLeft(problem).keySet()) {
                assertTrue(problem.heuristic(state) >= 0, "negative guess in " + state);
                if (problem.isGoal(state)) {
                    assertEquals(0, problem.heuristic(state), "a goal must be guessed as 0: " + state);
                }
            }
        }
    }

    @Test
    public void theGuessNeverOverestimates() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            for (String text : new String[] {PDF_EXAMPLE, SIX_BY_SEVEN, SMALL_CAVE, BIG_CAVE}) {
                CaveExplorer problem = new CaveExplorer(new CaveMap(text));

                for (Map.Entry<State, Integer> entry : realCostLeft(problem).entrySet()) {
                    if (entry.getValue() == NO_WAY) {
                        continue;   // nobody can win from there, so any guess is fine
                    }
                    assertTrue(problem.heuristic(entry.getKey()) <= entry.getValue(),
                            "guess " + problem.heuristic(entry.getKey()) + " is more than the real " + entry.getValue()
                                    + " in " + entry.getKey());
                }
            }
        });
    }
}
