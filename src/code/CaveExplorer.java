package code;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * The cave explorer as a search problem. The generic problem knows how to expand a node;
 * here we only say what a cave looks like: where we start, which actions exist, when we have
 * won and what a step costs. The cave rules themselves live in CaveRules.
 */
public class CaveExplorer extends GenericSearchProblem {

    // What we answer when a state can never win. It is far more than any real cost, so A* leaves such
    // states alone for as long as it has anything better to do.
    private static final int TOO_MUCH = 1000000;

    private final CaveMap cave;
    private final CaveRules rules;

    // energyToKey[k][y][x] is the least energy it takes to walk from cave (x, y) to key k, and
    // energyToDoor works the same way for the doors. They are worked out once, here in the
    // constructor, so that asking for a guess later is quick.
    private final int[][][] energyToKey;
    private final int[][][] energyToDoor;

    public CaveExplorer(CaveMap cave) {
        this.cave = cave;
        this.rules = new CaveRules(cave);

        energyToKey = new int[cave.keys.length][][];
        for (int i = 0; i < cave.keys.length; i++) {
            energyToKey[i] = energyTo(cave.keys[i][0], cave.keys[i][1]);
        }
        energyToDoor = new int[cave.doors.length][][];
        for (int i = 0; i < cave.doors.length; i++) {
            energyToDoor[i] = energyTo(cave.doors[i][0], cave.doors[i][1]);
        }
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

    // THE GUESS FOR A*
    //
    // It is a lower bound on what is still left to pay, counted the same way as the real cost (1000 for
    // every life used, plus the energy used). So it can never be more than the truth.
    //
    // We get it by relaxing the problem: we only keep the rules that are easy to count and forget the rest.
    //   1. Every door that is still locked has to be reached. We work out the cost of reaching each one
    //      and take the biggest, because the walk that reaches the costliest door may pass the others on
    //      the way, so adding them up could be too much.
    //   2. If our hands are empty, a key has to be picked up before the door can be opened, so the walk
    //      goes through one of the keys that are still on the floor. We take the cheapest one. If we
    //      already hold a key, we can go straight to the door.
    //   3. Energy: a walk costs at least the cheapest way through the cave, adding up the difficulty of
    //      every cave we step into. Rope, lives and the energy we have left are ignored while looking for it.
    //   4. Lives: a walk also has to go up or down at least as many rows as its two ends are apart.
    //      Every step up needs a meter of rope, and every step down needs a meter of rope or a life
    //      (a jump). So whatever the rope cannot pay for has to be paid with lives, 1000 each.
    // If a walk cannot be done at all (more steps up than we have rope, more jumps than we have lives to
    // spare, or more energy than we have left), nobody can win from here and the answer is TOO_MUCH.
    //
    // Manhattan distance only counts steps. This counts energy and lives, which is what the cost is made of.
    @Override
    public int heuristic(State state) {
        int guess = 0;
        for (int door = 0; door < cave.doors.length; door++) {
            if (state.doorLocked(door)) {
                guess = Math.max(guess, costToOpen(state, door));
            }
        }
        return guess;
    }

    // The least it costs to get to this door and open it: with a key in hand, the walk to the door;
    // with empty hands, the walk to the cheapest key and from there to the door.
    private int costToOpen(State state, int door) {
        int doorX = cave.doors[door][0];
        int doorY = cave.doors[door][1];
        if (state.holdingKey) {
            return costOfTrip(state, energyToDoor[door][state.y][state.x],
                    rowsUp(state.y, doorY), rowsDown(state.y, doorY));
        }

        int cheapest = TOO_MUCH;
        for (int key = 0; key < cave.keys.length; key++) {
            if (!state.keyOnFloor(key)) {
                continue;
            }
            int keyX = cave.keys[key][0];
            int keyY = cave.keys[key][1];
            int energy = energyToKey[key][state.y][state.x] + energyToDoor[door][keyY][keyX];
            int up = rowsUp(state.y, keyY) + rowsUp(keyY, doorY);
            int down = rowsDown(state.y, keyY) + rowsDown(keyY, doorY);
            cheapest = Math.min(cheapest, costOfTrip(state, energy, up, down));
        }
        return cheapest;
    }

    // What a trip costs at the very least, if it needs this much energy and goes up and down at least this
    // many rows: the energy, plus 1000 for every step down that the rope cannot pay for. Or TOO_MUCH if
    // we could not make the trip at all.
    private int costOfTrip(State state, int energy, int up, int down) {
        int jumps = Math.max(0, up + down - state.rope);
        if (up > state.rope || jumps > state.lives - 1 || energy > state.energy) {
            return TOO_MUCH;
        }
        return jumps * CaveRules.LIFE_COST + energy;
    }

    // how many rows we have to climb up (row numbers get smaller going up) or down to get from row a to row b
    private static int rowsUp(int a, int b) {
        return Math.max(0, a - b);
    }

    private static int rowsDown(int a, int b) {
        return Math.max(0, b - a);
    }

    // The least energy to walk from every cave to the cave (targetX, targetY), looking only at the walls and
    // the difficulties. A cave that cannot be reached at all gets TOO_MUCH. It is Dijkstra's algorithm run
    // backwards from the target: walking from a neighbour into a cave costs that cave's difficulty.
    private int[][] energyTo(int targetX, int targetY) {
        int[][] least = new int[cave.rows][cave.cols];
        for (int[] row : least) {
            Arrays.fill(row, TOO_MUCH);
        }
        least[targetY][targetX] = 0;

        PriorityQueue<int[]> queue = new PriorityQueue<>((a, b) -> a[0] - b[0]);
        queue.add(new int[] {0, targetX, targetY});
        int[][] neighbours = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!queue.isEmpty()) {
            int[] now = queue.poll();
            int x = now[1];
            int y = now[2];
            if (now[0] > least[y][x]) {
                continue;   // an older entry, we have found a cheaper way since
            }
            for (int[] step : neighbours) {
                int nx = x + step[0];
                int ny = y + step[1];
                if (nx < 0 || nx >= cave.cols || ny < 0 || ny >= cave.rows || cave.grid[ny][nx] == 0) {
                    continue;
                }
                int through = least[y][x] + cave.grid[y][x];
                if (through < least[ny][nx]) {
                    least[ny][nx] = through;
                    queue.add(new int[] {through, nx, ny});
                }
            }
        }
        return least;
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