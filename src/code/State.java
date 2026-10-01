package code;

/**
 * A snapshot of everything that can change while the explorer moves around the cave.
 * Two snapshots with the same numbers are the same state, which is how the search can
 * notice that it has already been in a situation before. A state never changes after it
 * is built: moving the explorer means making a new state.
 */
public class State {

    public static final int START_LIVES = 3;

    public final int x;
    public final int y;
    public final int energy;   // energy left
    public final int rope;     // meters of rope left
    public final int lives;    // lives left
    public final boolean holdingKey;

    // One bit per key and per door, in the same order as CaveMap.keys and CaveMap.doors.
    // Bit i of keysOnFloor is 1 while key i is still lying in its cave,
    // and bit i of doorsLocked is 1 while door i is still locked.
    public final int keysOnFloor;
    public final int doorsLocked;

    public State(int x, int y, int energy, int rope, int lives, boolean holdingKey,
                 int keysOnFloor, int doorsLocked) {
        this.x = x;
        this.y = y;
        this.energy = energy;
        this.rope = rope;
        this.lives = lives;
        this.holdingKey = holdingKey;
        this.keysOnFloor = keysOnFloor;
        this.doorsLocked = doorsLocked;
    }

    // the situation at the very beginning: nothing picked up, nothing unlocked yet
    public static State initial(CaveMap cave) {
        int allKeys = (1 << cave.keys.length) - 1;
        int allDoors = (1 << cave.doors.length) - 1;
        return new State(cave.startX, cave.startY, cave.energy, cave.rope, START_LIVES, false, allKeys, allDoors);
    }

    public boolean keyOnFloor(int i) {
        return (keysOnFloor & (1 << i)) != 0;
    }

    public boolean doorLocked(int i) {
        return (doorsLocked & (1 << i)) != 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof State)) {
            return false;
        }
        State s = (State) other;
        return x == s.x && y == s.y && energy == s.energy && rope == s.rope && lives == s.lives
                && holdingKey == s.holdingKey && keysOnFloor == s.keysOnFloor && doorsLocked == s.doorsLocked;
    }

    @Override
    public int hashCode() {
        // Energy goes first because it has the most different values. I tried x and y first, but then
        // up to a third of the states in the public caves got a hash code that another state already had.
        int h = energy;
        h = 31 * h + rope;
        h = 31 * h + lives;
        h = 31 * h + x;
        h = 31 * h + y;
        h = 31 * h + (holdingKey ? 1 : 0);
        h = 31 * h + keysOnFloor;
        h = 31 * h + doorsLocked;
        return h;
    }

    @Override
    public String toString() {
        return "State(x=" + x + ", y=" + y + ", energy=" + energy + ", rope=" + rope + ", lives=" + lives
                + ", holdingKey=" + holdingKey + ", keysOnFloor=" + keysOnFloor
                + ", doorsLocked=" + doorsLocked + ")";
    }
}
