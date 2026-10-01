package code;

/**
 * The seven things the explorer can do, in the same order as the assignment lists them.
 * The text is the name used in the plan we hand back.
 */
public enum Action {
    LEFT("left"),
    RIGHT("right"),
    CLIMB_UP("climbup"),
    CLIMB_DOWN("climbdown"),
    JUMP_DOWN("jumpdown"),
    COLLECT("collect"),
    UNLOCK("unlock");

    private final String text;

    Action(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
