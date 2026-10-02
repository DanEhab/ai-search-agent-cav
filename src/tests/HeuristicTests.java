package tests;

import code.CaveExplorer;
import code.CaveMap;
import code.CaveRules;
import code.GenericSearchProblem;
import code.Node;
import code.Search;
import code.State;
import code.Strategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

    // every cave the public tests use
    private static final String[] PUBLIC_CAVES = {
        "4,5;0,3;180,10;6,12,8,15,10;5,0,0,0,9;7,11,4,6,8;3,5,7,9,6;4,0;1,2;",
        "5,5;2,4;250,12;8,14,6,22,9;7,0,0,0,10;5,11,18,0,12;9,0,6,0,8;4,5,7,6,3;0,0;2,2;",
        "5,6;0,4;300,15;6,10,14,8,28,12;5,0,0,7,0,9;4,8,24,18,6,11;7,0,12,0,0,13;3,5,9,15,35,7;5,0;3,2;",
        "6,6;0,5;350,15;6,10,14,9,20,12;8,0,0,0,0,11;5,0,0,0,0,10;7,0,0,0,0,12;4,5,8,0,0,7;3,0,0,0,0,6;0,1,5,0;0,3,2,0;",
        "6,7;0,5;350,20;6,8,10,12,14,16,10;7,0,0,0,0,0,12;5,0,0,0,0,0,14;4,0,0,0,0,0,16;3,6,8,45,0,0,20;2,0,0,0,0,0,14;6,0;0,2;",
        "6,7;0,5;99,5;6,8,10,12,14,16,10;7,0,0,0,0,0,12;5,0,0,0,0,0,14;4,0,0,0,0,0,16;3,6,8,45,0,0,20;2,0,0,0,0,0,14;6,0;0,2;",
        "7,8;0,1;120,5;0,3,0,0,8,4,0,2;4,9,5,0,2,0,7,0;0,1,8,8,6,9,6,0;0,2,7,3,4,4,0,2;7,0,1,3,2,0,3,9;7,0,2,6,0,1,4,0;5,0,9,4,4,3,8,5;3,2,5,6;1,0,5,3,0,4;",
        "7,8;0,1;120,2;0,3,0,0,8,4,0,2;4,9,5,0,2,0,7,0;0,1,8,8,6,9,6,0;0,2,7,3,4,4,0,2;7,0,1,3,2,0,3,9;7,0,2,6,0,1,4,0;5,0,9,4,4,3,8,5;3,2,5,6;1,0,5,3,0,4;",
        "7,8;0,1;80,5;0,3,0,0,8,4,0,2;4,9,5,0,2,0,7,0;0,1,8,8,6,9,6,0;0,2,7,3,4,4,0,2;7,0,1,3,2,0,3,9;7,0,2,6,0,1,4,0;5,0,9,4,4,3,8,5;3,2,5,6;1,0,5,3,0,4;"
    };

    // A random cave in the style of the assignment, small enough that we can check every state of it.
    // Gives back null if there were too many walls to fit the doors and keys, so the caller can try again.
    private static String randomCave(Random random) {
        int rows = 3 + random.nextInt(3);
        int cols = 3 + random.nextInt(3);
        int[][] grid = new int[rows][cols];
        List<int[]> free = new ArrayList<>();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (random.nextInt(5) != 0) {
                    grid[y][x] = 1 + random.nextInt(20);
                    free.add(new int[] {x, y});
                }
            }
        }
        int doors = 1 + random.nextInt(3);
        int keys = doors + random.nextInt(3);
        if (free.size() < 1 + doors + keys) {
            return null;
        }
        Collections.shuffle(free, random);

        StringBuilder text = new StringBuilder();
        text.append(rows).append(",").append(cols).append(";");
        text.append(free.get(0)[0]).append(",").append(free.get(0)[1]).append(";");
        text.append(20 + random.nextInt(121)).append(",").append(random.nextInt(9)).append(";");
        for (int[] row : grid) {
            for (int x = 0; x < cols; x++) {
                text.append(row[x]).append(x < cols - 1 ? "," : ";");
            }
        }
        for (int i = 1; i <= doors; i++) {
            text.append(free.get(i)[0]).append(",").append(free.get(i)[1]).append(i < doors ? "," : ";");
        }
        for (int i = 1 + doors; i < 1 + doors + keys; i++) {
            text.append(free.get(i)[0]).append(",").append(free.get(i)[1]).append(i < doors + keys ? "," : ";");
        }
        return text.toString();
    }

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
            return (real == null || real == NO_WAY) ? HOPELESS : real;
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

    // ---------------------------------------------------------------- what the real guess says

    @Test
    public void atTheStartOfThePdfCaveTheGuessIsExactlyTheRealCost() {
        // 1 life and 65 energy, which is 1065. With 3 meters of rope no jump is needed, so just 65.
        assertEquals(1065, new CaveExplorer(new CaveMap(PDF_EXAMPLE)).heuristic(State.initial(new CaveMap(PDF_EXAMPLE))));
        CaveMap moreRope = new CaveMap("3,4;0,1;100,3;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;");
        assertEquals(65, new CaveExplorer(moreRope).heuristic(State.initial(moreRope)));
    }

    @Test
    public void theGuessCountsTheLivesThatTheRopeCannotPayFor() {
        // the trip to the key and then the door goes up 1 row and down 2, so it needs 3 meters of rope
        CaveExplorer twoMeters = new CaveExplorer(new CaveMap(PDF_EXAMPLE));
        assertTrue(twoMeters.heuristic(State.initial(new CaveMap(PDF_EXAMPLE))) >= CaveRules.LIFE_COST);
        CaveMap threeMeters = new CaveMap("3,4;0,1;100,3;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;");
        assertTrue(new CaveExplorer(threeMeters).heuristic(State.initial(threeMeters)) < CaveRules.LIFE_COST);
    }

    @Test
    public void theGuessAddsUpTheEnergyOfTheCheapestWalk() {
        CaveExplorer problem = new CaveExplorer(new CaveMap(PDF_EXAMPLE));

        // holding the key in cave (1,1): the walk to the door goes through (1,2), (2,2) and (3,2): 7 + 23 + 8
        assertEquals(38, problem.heuristic(new State(1, 1, 100, 3, 3, true, 0, 1)));
        // key still on the floor, 2 meters of rope: climb up for 3, then 12 + 7 + 23 + 8 to the door,
        // and one of the 3 steps up or down has to be a jump
        assertEquals(1053, problem.heuristic(new State(1, 1, 88, 2, 3, false, 1, 1)));
    }

    @Test
    public void theGuessIsTheCostOfTheHardestDoor() {
        // The biggest cave has two doors. The guess is the larger of what it costs to open each of them,
        // not the sum and not the smaller one.
        for (String text : new String[] {BIG_CAVE, PUBLIC_CAVES[8]}) {
            CaveMap cave = new CaveMap(text);
            assertEquals(56, new CaveExplorer(cave).heuristic(State.initial(cave)), text);
        }
    }

    @Test
    public void aStateThatCanNeverWinGetsAHugeGuess() {
        CaveExplorer problem = new CaveExplorer(new CaveMap(PDF_EXAMPLE));
        int hopeless = GenericSearchProblem.HOPELESS;

        // not enough energy for the 3 it takes to step up into the key's cave
        assertTrue(problem.heuristic(new State(1, 1, 2, 2, 3, false, 1, 1)) >= hopeless);
        // the key is a row up and there is no rope left to climb with (plenty of lives, so only the rope matters)
        assertTrue(problem.heuristic(new State(1, 1, 88, 0, 9, false, 1, 1)) >= hopeless);
        // the key has been used already and our hands are empty, so the door can never be opened
        assertTrue(problem.heuristic(new State(1, 1, 88, 2, 3, false, 0, 1)) >= hopeless);
        // the door is a row down, there is no rope, and with one life left we cannot jump
        assertTrue(problem.heuristic(new State(1, 1, 88, 0, 1, true, 0, 1)) >= hopeless);
        // a key is in hand but the door is out of reach for the energy we have
        assertTrue(problem.heuristic(new State(1, 1, 20, 3, 3, true, 0, 1)) >= hopeless);
    }

    @Test
    public void aStarWithTheGuessNeedsFarFewerNodesThanUniformCost() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            for (String text : PUBLIC_CAVES) {
                CaveMap cave = new CaveMap(text);
                Search uniform = new Search();
                Search guided = new Search();
                Node cheapest = uniform.run(new CaveExplorer(cave), Strategy.UC);
                Node found = guided.run(new CaveExplorer(cave), Strategy.AS);

                if (cheapest == null) {
                    assertNull(found, text);   // nobody can win, so there is nothing to save
                    continue;
                }
                assertEquals(cheapest.pathCost, found.pathCost, text);
                assertTrue(guided.nodesExpanded() < uniform.nodesExpanded() / 5,
                        "A* expanded " + guided.nodesExpanded() + " nodes, uniform cost " + uniform.nodesExpanded());
            }
        });
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

    @Test
    public void theGuessNeverOverestimatesInRandomCaves() {
        assertTimeoutPreemptively(Duration.ofSeconds(120), () -> {
            Random random = new Random(2026);
            int checked = 0;
            int solvable = 0;
            while (checked < 40) {
                String text = randomCave(random);
                if (text == null) {
                    continue;
                }
                CaveExplorer problem = new CaveExplorer(new CaveMap(text));

                // the guess may never be negative, may never be more than the real cost left, and is 0 at a goal
                for (Map.Entry<State, Integer> entry : realCostLeft(problem).entrySet()) {
                    int guess = problem.heuristic(entry.getKey());
                    assertTrue(guess >= 0, "negative guess in " + text);
                    if (problem.isGoal(entry.getKey())) {
                        assertEquals(0, guess, "a goal must be guessed as 0 in " + text);
                    }
                    if (entry.getValue() != NO_WAY) {
                        assertTrue(guess <= entry.getValue(), "guess " + guess + " is more than the real "
                                + entry.getValue() + " in " + entry.getKey() + " of " + text);
                    }
                }

                // and A* still finds the cheapest plan that uniform cost finds
                Node cheapest = new Search().run(new CaveExplorer(new CaveMap(text)), Strategy.UC);
                Node found = new Search().run(new CaveExplorer(new CaveMap(text)), Strategy.AS);
                if (cheapest == null) {
                    assertNull(found, text);
                } else {
                    assertEquals(cheapest.pathCost, found.pathCost, text);
                    solvable++;
                }
                checked++;
            }
            // make sure the check did not only look at caves nobody can win
            assertTrue(solvable >= 5, "only " + solvable + " of the 40 random caves can be won");
        });
    }
}
