package code;

import java.util.Arrays;

/**
 * The rules of the cave. Give it a state and an action and it tells you whether the action
 * is allowed, and if it is, what the new state looks like.
 */
public class CaveRules {

    // A cave never starts with more than 500 energy, so one life is worth more than all the energy
    // there is. That way a plan that loses fewer lives always costs less, whatever the energy.
    public static final int LIFE_COST = 1000;

    private final CaveMap cave;

    // keyAt[y][x] is the number of the key lying in that cave, or -1 if there is none.
    // doorAt works the same way for doors.
    private final int[][] keyAt;
    private final int[][] doorAt;

    public CaveRules(CaveMap cave) {
        this.cave = cave;
        this.keyAt = indexGrid(cave, cave.keys);
        this.doorAt = indexGrid(cave, cave.doors);
    }

    private static int[][] indexGrid(CaveMap cave, int[][] spots) {
        int[][] result = new int[cave.rows][cave.cols];
        for (int[] row : result) {
            Arrays.fill(row, -1);
        }
        for (int i = 0; i < spots.length; i++) {
            result[spots[i][1]][spots[i][0]] = i;
        }
        return result;
    }

    // Does the action in this state. Gives back the new state, or null if the action is not allowed.
    // The state we were given is never changed.
    public State apply(State s, Action action) {
        switch (action) {
            case LEFT:
                return walk(s, -1);
            case RIGHT:
                return walk(s, 1);
            case CLIMB_UP:
                return climb(s, -1);
            case CLIMB_DOWN:
                return climb(s, 1);
            case JUMP_DOWN:
                return jump(s);
            case COLLECT:
                return collect(s);
            case UNLOCK:
                return unlock(s);
            default:
                throw new IllegalArgumentException("unknown action " + action);
        }
    }

    // Can the explorer step into cave (x, y)? It has to be inside the grid, not a wall,
    // and the explorer needs at least as much energy as the cave's difficulty.
    private boolean canEnter(State s, int x, int y) {
        if (x < 0 || x >= cave.cols || y < 0 || y >= cave.rows) {
            return false;
        }
        int difficulty = cave.grid[y][x];
        return difficulty != 0 && s.energy >= difficulty;
    }

    // one step sideways: dx is -1 for left and 1 for right
    private State walk(State s, int dx) {
        int x = s.x + dx;
        if (!canEnter(s, x, s.y)) {
            return null;
        }
        return new State(x, s.y, s.energy - cave.grid[s.y][x], s.rope, s.lives,
                s.holdingKey, s.keysOnFloor, s.doorsLocked);
    }

    // one step up (dy is -1) or down (dy is 1), which uses a meter of rope
    private State climb(State s, int dy) {
        int y = s.y + dy;
        if (s.rope < 1 || !canEnter(s, s.x, y)) {
            return null;
        }
        return new State(s.x, y, s.energy - cave.grid[y][s.x], s.rope - 1, s.lives,
                s.holdingKey, s.keysOnFloor, s.doorsLocked);
    }

    // one step down that costs a life instead of rope. The last life can never be spent.
    private State jump(State s) {
        int y = s.y + 1;
        if (s.lives < 2 || !canEnter(s, s.x, y)) {
            return null;
        }
        return new State(s.x, y, s.energy - cave.grid[y][s.x], s.rope, s.lives - 1,
                s.holdingKey, s.keysOnFloor, s.doorsLocked);
    }

    // pick up the key lying in this cave, if our hands are empty
    private State collect(State s) {
        int key = keyAt[s.y][s.x];
        if (s.holdingKey || key == -1 || !s.keyOnFloor(key)) {
            return null;
        }
        return new State(s.x, s.y, s.energy, s.rope, s.lives,
                true, s.keysOnFloor & ~(1 << key), s.doorsLocked);
    }

    // open the locked door in this cave with the key we are holding (the key is used up)
    private State unlock(State s) {
        int door = doorAt[s.y][s.x];
        if (!s.holdingKey || door == -1 || !s.doorLocked(door)) {
            return null;
        }
        return new State(s.x, s.y, s.energy, s.rope, s.lives,
                false, s.keysOnFloor, s.doorsLocked & ~(1 << door));
    }

    // We win when no door is locked any more. Having a life left comes for free, because
    // a jump is never allowed on the last one.
    public boolean isGoal(State s) {
        return s.doorsLocked == 0;
    }

    // What one step cost: every life it took counts LIFE_COST, plus the energy it used.
    // Rope is not part of the cost.
    public int stepCost(State before, State after) {
        return (before.lives - after.lives) * LIFE_COST + (before.energy - after.energy);
    }

    public int livesUsed(State s) {
        return State.START_LIVES - s.lives;
    }

    public int energyUsed(State s) {
        return cave.energy - s.energy;
    }

    // Runs a plan written as text, like "right,climbup,collect", from the start of the cave.
    // Gives back the state we end up in, or null if one of the steps is not allowed.
    public State replay(String plan) {
        State s = State.initial(cave);
        if (plan.trim().isEmpty()) {
            return s;
        }
        for (String word : plan.split(",")) {
            s = apply(s, Action.fromText(word.trim()));
            if (s == null) {
                return null;
            }
        }
        return s;
    }
}
