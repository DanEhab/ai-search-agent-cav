package tests;

import code.CaveExplorer;
import code.CaveMap;
import code.CaveRules;
import code.Node;
import code.Search;
import code.State;
import code.Strategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the search loop and the three strategies: uniform cost, iterative deepening and A*.
 */
public class SearchTests {

    private static final String PDF_EXAMPLE = "3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;";

    // every cave the public tests use, and what the cheapest plan for it costs (1000 for every
    // life used plus the energy used), or -1 if the cave cannot be won. The costs were worked out
    // separately, and the real checker accepts the plans.
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
    private static final int[] CHEAPEST = {53, 55, 99, 95, 95, 95, 2075, -1, 2075};

    // A cave problem that remembers every node it is asked to expand, in order.
    private static class RecordingProblem extends CaveExplorer {
        final List<Node> expanded = new ArrayList<>();

        RecordingProblem(CaveMap cave) {
            super(cave);
        }

        @Override
        public List<Node> expand(Node node) {
            expanded.add(node);
            return super.expand(node);
        }
    }

    // the fewest actions any plan needs for each of those caves (-1 means the cave cannot be won)
    private static final int[] FEWEST_ACTIONS = {9, 8, 11, 14, 13, 13, 20, -1, 20};

    private static Node iterativeDeepening(String caveText) {
        return new Search().run(new CaveExplorer(new CaveMap(caveText)), Strategy.ID);
    }
    private static Node uniformCost(String caveText) {
        return new Search().run(new CaveExplorer(new CaveMap(caveText)), Strategy.UC);
    }

    // ---------------------------------------------------------------- the PDF example

    @Test
    public void thePdfExampleNeedsOneLifeAnd65Energy() {
        CaveRules rules = new CaveRules(new CaveMap(PDF_EXAMPLE));
        Search search = new Search();
        Node goal = search.run(new CaveExplorer(new CaveMap(PDF_EXAMPLE)), Strategy.UC);

        assertNotNull(goal);
        assertTrue(rules.isGoal(goal.state));
        assertEquals(1065, goal.pathCost);
        assertEquals(1, rules.livesUsed(goal.state));
        assertEquals(65, rules.energyUsed(goal.state));
        // the plan that comes out really leads to that state
        assertEquals(goal.state, rules.replay(goal.planText()));
        // the cave only has 368 different states, and we should not need to look at all of them
        assertTrue(search.nodesExpanded() > 0 && search.nodesExpanded() < 368);
    }

    @Test
    public void aCaveThatCannotBeWonGivesNoNode() {
        // the cave of test_plan_uc6: with 2 meters of rope the door can never be reached
        assertNull(uniformCost(PUBLIC_CAVES[7]));
    }

    // ---------------------------------------------------------------- the public caves

    @Test
    public void everyPublicCaveGetsItsCheapestPlan() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            for (int i = 0; i < PUBLIC_CAVES.length; i++) {
                CaveMap cave = new CaveMap(PUBLIC_CAVES[i]);
                CaveRules rules = new CaveRules(cave);
                Search search = new Search();
                Node goal = search.run(new CaveExplorer(cave), Strategy.UC);
                String which = "public cave number " + i;

                if (CHEAPEST[i] == -1) {
                    assertNull(goal, which);
                    continue;
                }
                assertNotNull(goal, which);
                assertTrue(rules.isGoal(goal.state), which);
                assertEquals(CHEAPEST[i], goal.pathCost, which);
                assertEquals(goal.state, rules.replay(goal.planText()), which);

                // the real checker has to accept the answer too
                String answer = goal.planText() + ";" + rules.livesUsed(goal.state) + ";"
                        + rules.energyUsed(goal.state) + ";" + search.nodesExpanded();
                Checker.ValidationResult result = Checker.validateSolution(PUBLIC_CAVES[i], answer);
                assertTrue(result.isValid, which + ": the checker refuses " + answer + " (" + result.errorMessage + ")");
            }
        });
    }

    @Test
    public void theCostTestsOfThePublicTestsGetTheirNumbers() {
        // test_plan_uc_cost1 expects 0 lives and 99 energy, test_plan_uc_cost2 expects 2 lives and 75 energy
        CaveRules rules1 = new CaveRules(new CaveMap(PUBLIC_CAVES[2]));
        Node goal1 = uniformCost(PUBLIC_CAVES[2]);
        assertEquals(0, rules1.livesUsed(goal1.state));
        assertEquals(99, rules1.energyUsed(goal1.state));

        CaveRules rules2 = new CaveRules(new CaveMap(PUBLIC_CAVES[6]));
        Node goal2 = uniformCost(PUBLIC_CAVES[6]);
        assertEquals(2, rules2.livesUsed(goal2.state));
        assertEquals(75, rules2.energyUsed(goal2.state));
    }

    // ---------------------------------------------------------------- lives come before energy

    @Test
    public void aLifeIsWorthMoreThanAnyEnergy() {
        // In both caves there is a plan that saves a little energy but costs lives
        // (9 energy and 2 lives against 11 energy and no life in the first cave,
        // 14 energy and 1 life against 17 energy and no life in the second).
        // Uniform cost has to take the plan that keeps the lives.
        String first = "3,3;2,1;200,1;3,6,0;3,7,6;1,1,3;0,2;0,1;";
        String second = "3,3;2,2;200,3;6,5,7;1,2,2;2,8,8;0,0;0,2;";

        assertEquals(11, uniformCost(first).pathCost);
        assertEquals(17, uniformCost(second).pathCost);
    }

    @Test
    public void moreRopeMeansNoLifeIsNeeded() {
        // the PDF example needs a jump with 2 meters of rope, and nothing but climbing with 3
        CaveMap twoMeters = new CaveMap(PDF_EXAMPLE);
        CaveMap threeMeters = new CaveMap("3,4;0,1;100,3;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;");
        CaveRules rules = new CaveRules(twoMeters);

        Node withTwo = new Search().run(new CaveExplorer(twoMeters), Strategy.UC);
        Node withThree = new Search().run(new CaveExplorer(threeMeters), Strategy.UC);

        assertEquals(1, rules.livesUsed(withTwo.state));
        assertEquals(0, rules.livesUsed(withThree.state));
        assertEquals(65, withThree.pathCost);
    }

    @Test
    public void theGoalCountsOnlyWhenItComesOutOfTheQueue() {
        // With 3 meters of rope the PDF example can be won without a jump, for 65 energy.
        // Here unlocking is made very expensive (2000) for anyone who has not used a life, so the
        // plan with a jump (1000 + 65, then a free unlock) is the cheapest one after all.
        // A search that stops as soon as it makes a goal node, instead of waiting until
        // the goal comes out of the queue, would return the 2065 plan that it finds first.
        CaveMap cave = new CaveMap("3,4;0,1;100,3;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;");
        CaveExplorer expensiveUnlock = new CaveExplorer(cave) {
            @Override
            public int stepCost(State before, State after) {
                boolean unlocking = before.holdingKey && !after.holdingKey;
                if (unlocking && before.lives == State.START_LIVES) {
                    return 2000;
                }
                return super.stepCost(before, after);
            }
        };

        Node goal = new Search().run(expensiveUnlock, Strategy.UC);

        assertEquals(1065, goal.pathCost);
        assertEquals(2, goal.state.lives);
    }

    // ---------------------------------------------------------------- how the search behaves

    @Test
    public void nodesAreExpandedInOrderOfCost() {
        RecordingProblem problem = new RecordingProblem(new CaveMap(PDF_EXAMPLE));
        new Search().run(problem, Strategy.UC);

        for (int i = 1; i < problem.expanded.size(); i++) {
            assertTrue(problem.expanded.get(i - 1).pathCost <= problem.expanded.get(i).pathCost,
                    "node " + i + " is cheaper than the one before it");
        }
    }

    @Test
    public void noStateIsExpandedTwiceAndTheCounterCountsThem() {
        RecordingProblem problem = new RecordingProblem(new CaveMap(PDF_EXAMPLE));
        Search search = new Search();
        search.run(problem, Strategy.UC);

        Set<State> states = new HashSet<>();
        for (Node node : problem.expanded) {
            assertTrue(states.add(node.state), "expanded twice: " + node.state);
        }
        assertEquals(problem.expanded.size(), search.nodesExpanded());
    }

    @Test
    public void theCounterStartsOverForEverySearch() {
        Search search = new Search();
        CaveExplorer problem = new CaveExplorer(new CaveMap(PDF_EXAMPLE));

        search.run(problem, Strategy.UC);
        int firstTime = search.nodesExpanded();
        search.run(problem, Strategy.UC);

        assertTrue(firstTime > 0);
        assertEquals(firstTime, search.nodesExpanded());
    }

    @Test
    public void aCaveThatCannotBeWonIsSearchedCompletely() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            // with no way to win, the search has to look at every state there is before it gives up
            Search search = new Search();
            search.run(new CaveExplorer(new CaveMap(PUBLIC_CAVES[7])), Strategy.UC);

            assertEquals(14350, search.nodesExpanded());
        });
    }

    // ---------------------------------------------------------------- iterative deepening

    @Test
    public void iterativeDeepeningFindsAPdfPlanWithTheFewestActions() {
        CaveRules rules = new CaveRules(new CaveMap(PDF_EXAMPLE));
        Node goal = iterativeDeepening(PDF_EXAMPLE);

        assertNotNull(goal);
        assertTrue(rules.isGoal(goal.state));
        // no plan for this cave has fewer than 8 actions
        assertEquals(8, goal.depth);
        assertEquals(goal.state, rules.replay(goal.planText()));
    }

    @Test
    public void iterativeDeepeningGivesNoNodeWhenThereIsNoWay() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            // the cave of test_plan_uc6, and the PDF example with no rope at all (the key is up a wall)
            assertNull(iterativeDeepening(PUBLIC_CAVES[7]));
            assertNull(iterativeDeepening("3,4;0,1;100,0;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;"));
        });
    }

    @Test
    public void everyPublicCaveGetsAPlanWithTheFewestActions() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            for (int i = 0; i < PUBLIC_CAVES.length; i++) {
                CaveMap cave = new CaveMap(PUBLIC_CAVES[i]);
                CaveRules rules = new CaveRules(cave);
                Search search = new Search();
                Node goal = search.run(new CaveExplorer(cave), Strategy.ID);
                String which = "public cave number " + i;

                if (FEWEST_ACTIONS[i] == -1) {
                    assertNull(goal, which);
                    continue;
                }
                assertNotNull(goal, which);
                assertTrue(rules.isGoal(goal.state), which);
                assertEquals(FEWEST_ACTIONS[i], goal.depth, which);
                assertEquals(goal.state, rules.replay(goal.planText()), which);
                // fewest actions is not the same as cheapest, but it can never be cheaper than the cheapest
                assertTrue(goal.pathCost >= CHEAPEST[i], which);

                String answer = goal.planText() + ";" + rules.livesUsed(goal.state) + ";"
                        + rules.energyUsed(goal.state) + ";" + search.nodesExpanded();
                Checker.ValidationResult result = Checker.validateSolution(PUBLIC_CAVES[i], answer);
                assertTrue(result.isValid, which + ": the checker refuses " + answer + " (" + result.errorMessage + ")");
            }
        });
    }

    @Test
    public void iterativeDeepeningDoesNotPromiseTheCheapestPlan() {
        // In this cave the shortest plan has 5 actions and costs 26 (climb down twice, collect, climb up, unlock).
        // A longer plan, walking round through the cheaper caves, costs only 24.
        String cave = "3,3;2,0;200,3;0,1,8;0,3,9;0,3,8;2,1;2,2;";
        Node fewestActions = iterativeDeepening(cave);
        Node cheapest = uniformCost(cave);

        assertEquals(5, fewestActions.depth);
        assertEquals(24, cheapest.pathCost);
        assertTrue(fewestActions.pathCost > cheapest.pathCost);
        assertTrue(cheapest.depth > fewestActions.depth);
    }

    @Test
    public void everyRoundStartsOverAndNoStateIsExpandedAgainAtTheSameDepth() {
        RecordingProblem problem = new RecordingProblem(new CaveMap(PDF_EXAMPLE));
        Node goal = new Search().run(problem, Strategy.ID);

        // A new round begins whenever the start node (depth 0) is expanded again. Within one round a state
        // may only be expanded again if we reach it with fewer steps than before.
        Map<State, Integer> shallowest = new HashMap<>();
        int rounds = 0;
        for (Node node : problem.expanded) {
            if (node.depth == 0) {
                rounds++;
                shallowest.clear();
            }
            Integer earlier = shallowest.get(node.state);
            assertTrue(earlier == null || node.depth < earlier, "expanded again too deep: " + node.state);
            shallowest.put(node.state, node.depth);
        }

        // the rounds have limits 1, 2, 3 ... up to the depth of the goal (a round with limit 0 expands nothing)
        assertEquals(goal.depth, rounds);
    }

    @Test
    public void theCounterAddsUpAllTheRounds() {
        RecordingProblem problem = new RecordingProblem(new CaveMap(PDF_EXAMPLE));
        Search search = new Search();
        search.run(problem, Strategy.ID);

        assertEquals(problem.expanded.size(), search.nodesExpanded());

        // every round expands the start node again, so we expanded more nodes than there are different states
        Set<State> different = new HashSet<>();
        for (Node node : problem.expanded) {
            different.add(node.state);
        }
        assertTrue(different.size() < search.nodesExpanded());
    }

    // ---------------------------------------------------------------- A*

    @Test
    public void aStarFindsTheCheapestPlanOfEveryPublicCave() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            for (int i = 0; i < PUBLIC_CAVES.length; i++) {
                CaveMap cave = new CaveMap(PUBLIC_CAVES[i]);
                CaveRules rules = new CaveRules(cave);
                Search search = new Search();
                Node goal = search.run(new CaveExplorer(cave), Strategy.AS);
                String which = "public cave number " + i;

                if (CHEAPEST[i] == -1) {
                    assertNull(goal, which);
                    continue;
                }
                assertNotNull(goal, which);
                assertTrue(rules.isGoal(goal.state), which);
                assertEquals(CHEAPEST[i], goal.pathCost, which);
                assertEquals(goal.state, rules.replay(goal.planText()), which);

                String answer = goal.planText() + ";" + rules.livesUsed(goal.state) + ";"
                        + rules.energyUsed(goal.state) + ";" + search.nodesExpanded();
                Checker.ValidationResult result = Checker.validateSolution(PUBLIC_CAVES[i], answer);
                assertTrue(result.isValid, which + ": the checker refuses " + answer + " (" + result.errorMessage + ")");
            }
        });
    }

    @Test
    public void aStarGivesNoNodeWhenThereIsNoWay() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            Search search = new Search();

            assertNull(search.run(new CaveExplorer(new CaveMap(PUBLIC_CAVES[7])), Strategy.AS));
            assertTrue(search.nodesExpanded() > 0);
            assertNull(new Search().run(new CaveExplorer(new CaveMap("3,4;0,1;100,0;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;")), Strategy.AS));
        });
    }

    @Test
    public void aStarWithAGuessOfZeroDoesWhatUniformCostDoes() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            for (String text : PUBLIC_CAVES) {
                CaveMap cave = new CaveMap(text);
                CaveExplorer noGuess = new CaveExplorer(cave) {
                    @Override
                    public int heuristic(State state) {
                        return 0;
                    }
                };
                Search uniform = new Search();
                Search aStar = new Search();
                Node fromUniform = uniform.run(new CaveExplorer(cave), Strategy.UC);
                Node fromAStar = aStar.run(noGuess, Strategy.AS);

                // the same order of nodes, so the same number of them
                assertEquals(uniform.nodesExpanded(), aStar.nodesExpanded(), text);
                if (fromUniform == null) {
                    assertNull(fromAStar, text);
                } else {
                    assertEquals(fromUniform.pathCost, fromAStar.pathCost, text);
                }
            }
        });
    }

    @Test
    public void aGuessThatIsTooBigCanMakeAStarMissTheCheapestPlan() {
        // The PDF example with 3 meters of rope: the cheapest plan costs 65 and needs no jump.
        // This guess says that anyone who has not lost a life yet is 5000 away from the goal. That is
        // far more than the truth, so A* runs away from those states and pays lives to get out of them.
        // It is why a guess must never be bigger than what is really left.
        CaveMap cave = new CaveMap("3,4;0,1;100,3;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;");
        CaveExplorer tooBig = new CaveExplorer(cave) {
            @Override
            public int heuristic(State state) {
                return state.lives == State.START_LIVES ? 5000 : 0;
            }
        };

        Node cheapest = new Search().run(new CaveExplorer(cave), Strategy.UC);
        Node misled = new Search().run(tooBig, Strategy.AS);

        assertEquals(65, cheapest.pathCost);
        assertTrue(misled.pathCost > cheapest.pathCost);
        assertTrue(misled.state.lives < State.START_LIVES);
    }}
