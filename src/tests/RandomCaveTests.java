package tests;

import code.Action;
import code.CaveExplorer;
import code.CaveMap;
import code.CaveRules;
import code.Node;
import code.Search;
import code.State;
import code.Strategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests with caves that nobody picked by hand: random caves in the sizes the assignment allows.
 * The strategies are checked against two plain searches written here, and against the real checker.
 * The private tests will use caves we have never seen, so this is the closest we can get to them.
 */
public class RandomCaveTests {

    private static final int NO_WAY = -1;
    private static final String[] STRATEGIES = {"UC", "ID", "AS"};

    // 100 random caves with sides of 3 to 6, half of them can be won and half cannot. They are the same
    // ones for every test (the seed is fixed).
    private static final List<String> CAVES = randomCaves(2026, 100, 6, 200, 12, 30000);

    // ---------------------------------------------------------------- making caves

    private static List<String> randomCaves(long seed, int count, int biggestSide, int mostEnergy, int mostRope,
                                            int mostStates) {
        Random random = new Random(seed);
        List<String> caves = new ArrayList<>();
        while (caves.size() < count) {
            // caves that can be won are rare among the small ones, so we ask for them every other time
            boolean wantOneWeCanWin = caves.size() % 2 == 0;
            String text = randomCave(random, biggestSide, mostEnergy, mostRope);
            CaveMap cave = new CaveMap(text);
            if (hasAtMost(cave, mostStates) && (fewestActions(cave) != NO_WAY) == wantOneWeCanWin) {
                caves.add(text);
            }
        }
        return caves;
    }

    // Is the whole cave small enough? Counts every state we can get into and gives up once there are more than
    // the limit. Many caves have millions of states, and the plain searches below would take far too long on them.
    private static boolean hasAtMost(CaveMap cave, int limit) {
        CaveRules rules = new CaveRules(cave);
        State start = State.initial(cave);
        Set<State> seen = new HashSet<>();
        Deque<State> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            State state = queue.poll();
            for (Action action : Action.values()) {
                State next = rules.apply(state, action);
                if (next != null && seen.add(next)) {
                    if (seen.size() > limit) {
                        return false;
                    }
                    queue.add(next);
                }
            }
        }
        return true;
    }

    // A random cave. The sides are between 3 and biggestSide, and everything else stays in the ranges of the
    // assignment: 1 to 5 doors, as many keys or more (up to 8) and difficulties up to 45. Some caves have walls
    // (a difficulty of 0), some have none. The energy and the rope are never more than the caller allows.
    private static String randomCave(Random random, int biggestSide, int mostEnergy, int mostRope) {
        while (true) {
            int rows = 3 + random.nextInt(biggestSide - 2);
            int cols = 3 + random.nextInt(biggestSide - 2);
            int hardest = new int[] {3, 9, 45}[random.nextInt(3)];
            int wallChance = 10 * random.nextInt(4);

            int[][] grid = new int[rows][cols];
            List<int[]> free = new ArrayList<>();
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    if (random.nextInt(100) >= wallChance) {
                        grid[y][x] = 1 + random.nextInt(hardest);
                        free.add(new int[] {x, y});
                    }
                }
            }
            int doors = 1 + random.nextInt(5);
            int keys = doors + random.nextInt(Math.min(3, 8 - doors) + 1);
            if (free.size() < 1 + doors + keys) {
                continue;   // too many walls to fit everything, make another one
            }
            Collections.shuffle(free, random);

            int energy = mostEnergy / 4 + random.nextInt(mostEnergy - mostEnergy / 4 + 1);
            int rope = random.nextInt(mostRope + 1);
            return caveText(grid, free, doors, keys, energy, rope);
        }
    }

    // A cave whose first door sits in the bottom right corner with a wall on each side of it, so nobody can
    // ever get to that door. The rest of the cave is open, with up to 3 doors in all.
    private static String sealedCave(Random random, int side, int energy, int rope) {
        int[][] grid = new int[side][side];
        List<int[]> free = new ArrayList<>();
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                boolean corner = x == side - 1 && y == side - 1;
                boolean wall = (x == side - 2 && y == side - 1) || (x == side - 1 && y == side - 2);
                if (!wall) {
                    grid[y][x] = 1 + random.nextInt(4);
                }
                if (!wall && !corner) {
                    free.add(new int[] {x, y});
                }
            }
        }
        Collections.shuffle(free, random);
        free.add(1, new int[] {side - 1, side - 1});   // the first door goes right after the start
        int doors = 1 + random.nextInt(3);
        int keys = doors + random.nextInt(3);
        return caveText(grid, free, doors, keys, energy, rope);
    }

    // The biggest cave the assignment allows: 10 by 10, 5 doors, 8 keys, 500 energy and 25 meters of rope.
    // It has no walls and only cheap caves, which makes the most states to look through.
    private static String biggestCave(Random random) {
        int hardest = 1 + random.nextInt(3);
        int[][] grid = new int[10][10];
        List<int[]> free = new ArrayList<>();
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                grid[y][x] = 1 + random.nextInt(hardest);
                free.add(new int[] {x, y});
            }
        }
        Collections.shuffle(free, random);
        return caveText(grid, free, 5, 8, 500, 25);
    }

    // Writes a cave down in the form of the assignment. The first cave in the list is where we start,
    // the next ones are the doors and then come the keys.
    private static String caveText(int[][] grid, List<int[]> free, int doors, int keys, int energy, int rope) {
        StringBuilder text = new StringBuilder();
        text.append(grid.length).append(",").append(grid[0].length).append(";");
        text.append(free.get(0)[0]).append(",").append(free.get(0)[1]).append(";");
        text.append(energy).append(",").append(rope).append(";");
        for (int[] row : grid) {
            for (int x = 0; x < row.length; x++) {
                text.append(row[x]).append(x < row.length - 1 ? "," : ";");
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

    // ---------------------------------------------------------------- the right answers, the slow and plain way

    // The fewest actions any plan needs, found by plain breadth-first search over the states, or NO_WAY
    // if the cave cannot be won. It uses nothing but the rules, none of the search code we are testing.
    private static int fewestActions(CaveMap cave) {
        CaveRules rules = new CaveRules(cave);
        State start = State.initial(cave);
        Map<State, Integer> depth = new HashMap<>();
        Deque<State> queue = new ArrayDeque<>();
        depth.put(start, 0);
        queue.add(start);
        while (!queue.isEmpty()) {
            State state = queue.poll();
            if (rules.isGoal(state)) {
                return depth.get(state);
            }
            for (Action action : Action.values()) {
                State next = rules.apply(state, action);
                if (next != null && !depth.containsKey(next)) {
                    depth.put(next, depth.get(state) + 1);
                    queue.add(next);
                }
            }
        }
        return NO_WAY;
    }

    // The least any plan can cost (1000 for every life, plus the energy), found by a plain Dijkstra over the
    // states, or NO_WAY if the cave cannot be won. Like the search above it only uses the rules.
    private static int cheapestCost(CaveMap cave) {
        CaveRules rules = new CaveRules(cave);
        State start = State.initial(cave);
        Map<State, Integer> best = new HashMap<>();
        PriorityQueue<Map.Entry<State, Integer>> queue = new PriorityQueue<>(Map.Entry.comparingByValue());
        best.put(start, 0);
        queue.add(new AbstractMap.SimpleEntry<>(start, 0));
        while (!queue.isEmpty()) {
            Map.Entry<State, Integer> top = queue.poll();
            State state = top.getKey();
            int cost = top.getValue();
            if (cost > best.get(state)) {
                continue;   // an old entry, we found a cheaper way to this state since
            }
            if (rules.isGoal(state)) {
                return cost;
            }
            for (Action action : Action.values()) {
                State next = rules.apply(state, action);
                if (next == null) {
                    continue;
                }
                int through = cost + rules.stepCost(state, next);
                Integer known = best.get(next);
                if (known == null || through < known) {
                    best.put(next, through);
                    queue.add(new AbstractMap.SimpleEntry<>(next, through));
                }
            }
        }
        return NO_WAY;
    }

    // ---------------------------------------------------------------- random caves

    @Test
    public void halfOfTheRandomCavesCanBeWonAndHalfCannot() {
        // the other tests only mean something if there are caves of both kinds
        int solvable = 0;
        for (String text : CAVES) {
            if (fewestActions(new CaveMap(text)) != NO_WAY) {
                solvable++;
            }
        }
        assertEquals(CAVES.size() / 2, solvable);
    }

    @Test
    public void uniformCostAndAStarFindTheCheapestPlanOfEveryRandomCave() {
        assertTimeoutPreemptively(Duration.ofSeconds(120), () -> {
            for (String text : CAVES) {
                CaveMap cave = new CaveMap(text);
                int cheapest = cheapestCost(cave);
                Node uniform = new Search().run(new CaveExplorer(cave), Strategy.UC);
                Node aStar = new Search().run(new CaveExplorer(cave), Strategy.AS);

                if (cheapest == NO_WAY) {
                    assertNull(uniform, text);
                    assertNull(aStar, text);
                } else {
                    assertNotNull(uniform, text);
                    assertNotNull(aStar, text);
                    assertEquals(cheapest, uniform.pathCost, "uniform cost in " + text);
                    assertEquals(cheapest, aStar.pathCost, "A* in " + text);
                }
            }
        });
    }

    @Test
    public void iterativeDeepeningFindsThePlanWithTheFewestActionsInEveryRandomCave() {
        assertTimeoutPreemptively(Duration.ofSeconds(120), () -> {
            for (String text : CAVES) {
                CaveMap cave = new CaveMap(text);
                int fewest = fewestActions(cave);
                Node found = new Search().run(new CaveExplorer(cave), Strategy.ID);

                if (fewest == NO_WAY) {
                    assertNull(found, text);
                } else {
                    assertNotNull(found, text);
                    assertEquals(fewest, found.depth, text);
                    assertTrue(new CaveRules(cave).isGoal(found.state), text);
                }
            }
        });
    }

    @Test
    public void everyStrategyGivesAnAnswerTheRealCheckerAccepts() {
        assertTimeoutPreemptively(Duration.ofSeconds(120), () -> {
            for (String text : CAVES) {
                boolean solvable = cheapestCost(new CaveMap(text)) != NO_WAY;
                for (String strategy : STRATEGIES) {
                    String answer = CaveExplorer.solve(text, strategy);
                    String which = strategy + " on " + text;

                    if (!solvable) {
                        assertEquals("No Solution", answer, which);
                        continue;
                    }
                    Checker.ValidationResult result = Checker.validateSolution(text, answer);
                    assertTrue(result.isValid, which + ": the checker refuses " + answer + " (" + result.errorMessage + ")");
                }
            }
        });
    }

    @Test
    public void theLivesAndEnergyInAnAnswerAddUpToTheCheapestCost() {
        assertTimeoutPreemptively(Duration.ofSeconds(120), () -> {
            for (String text : CAVES) {
                int cheapest = cheapestCost(new CaveMap(text));
                if (cheapest == NO_WAY) {
                    continue;
                }
                for (String strategy : new String[] {"UC", "AS"}) {
                    String[] parts = CaveExplorer.solve(text, strategy).split(";");

                    int paid = 1000 * Integer.parseInt(parts[1]) + Integer.parseInt(parts[2]);
                    assertEquals(cheapest, paid, strategy + " on " + text);
                }
            }
        });
    }

    // ---------------------------------------------------------------- caves that cannot be won

    @Test
    public void aDoorNobodyCanReachMeansNoSolutionForEveryStrategy() {
        // small caves, because uniform cost and iterative deepening have to look at every state of them
        assertTimeoutPreemptively(Duration.ofSeconds(120), () -> {
            Random random = new Random(77);
            int checked = 0;
            while (checked < 20) {
                String text = sealedCave(random, 4, 20 + random.nextInt(81), random.nextInt(8));
                if (!hasAtMost(new CaveMap(text), 30000)) {
                    continue;
                }
                for (String strategy : STRATEGIES) {
                    assertEquals("No Solution", CaveExplorer.solve(text, strategy), strategy + " on " + text);
                }
                checked++;
            }
        });
    }

    @Test
    public void aStarSeesRightAwayThatASealedDoorCannotBeOpened() {
        // In the biggest cave allowed, uniform cost would have to look at tens of millions of states (and
        // would run out of memory) before it could say that nobody can win. A* knows from the very start,
        // because from the start no key can bring us to that door, so it never queues anything.
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            Random random = new Random(7);
            for (int i = 0; i < 5; i++) {
                String text = sealedCave(random, 10, 500, 25);
                int[] expanded = {0};
                CaveExplorer problem = new CaveExplorer(new CaveMap(text)) {
                    @Override
                    public List<Node> expand(Node node) {
                        // stop at once if A* starts looking around, instead of waiting for it to run out of memory
                        assertTrue(++expanded[0] <= 10, "A* is expanding too many nodes in " + text);
                        return super.expand(node);
                    }
                };

                assertNull(new Search().run(problem, Strategy.AS), text);
                assertEquals(1, expanded[0], text);
                assertEquals("No Solution", CaveExplorer.solve(text, "AS"), text);
            }
        });
    }

    @Test
    public void noEnergyMeansNoSolution() {
        // the start cave costs nothing, but every other cave costs at least 1 energy to enter
        for (String text : new String[] {"3,3;0,0;0,25;1,1,1;1,1,1;1,1,1;2,2;1,1;",
                                         "4,4;1,2;0,0;3,2,1,5;4,4,4,4;1,9,1,9;45,45,45,45;3,3,0,0;1,1,2,2;"}) {
            for (String strategy : STRATEGIES) {
                assertEquals("No Solution", CaveExplorer.solve(text, strategy), strategy + " on " + text);
            }
        }
    }

    // ---------------------------------------------------------------- the biggest caves

    @Test
    public void theBiggestCaveAllowedIsSolvedByAStarInTime() {
        // 10 by 10, 5 doors, 8 keys, 500 energy and 25 meters of rope: the most the assignment can ask for
        assertTimeoutPreemptively(Duration.ofSeconds(120), () -> {
            Random random = new Random(11);
            for (int i = 0; i < 3; i++) {
                String text = biggestCave(random);
                String answer = CaveExplorer.solve(text, "AS");

                Checker.ValidationResult result = Checker.validateSolution(text, answer);
                assertTrue(result.isValid, "the checker refuses " + answer + " (" + result.errorMessage + ") for " + text);
            }
        });
    }

    @Test
    public void inTheBiggestCaveUniformCostFindsTheSamePlanCostAsAStar() {
        assertTimeoutPreemptively(Duration.ofSeconds(120), () -> {
            String text = biggestCave(new Random(11));
            String[] uniform = CaveExplorer.solve(text, "UC").split(";");
            String[] aStar = CaveExplorer.solve(text, "AS").split(";");

            assertEquals(uniform[1], aStar[1], text);
            assertEquals(uniform[2], aStar[2], text);
            // and A* got there by looking at far fewer nodes
            assertTrue(Integer.parseInt(aStar[3]) * 5 < Integer.parseInt(uniform[3]), text);
        });
    }
}
