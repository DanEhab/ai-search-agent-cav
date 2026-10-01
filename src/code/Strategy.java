package code;

import java.util.Arrays;

/**
 * The ways we can search. The names are the ones the assignment uses.
 */
public enum Strategy {
    UC;   // uniform cost: always carry on from the cheapest node we have

    // the strategy with this name ("UC" gives UC), or an error that lists the ones we have
    public static Strategy fromText(String text) {
        for (Strategy strategy : values()) {
            if (strategy.name().equals(text)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("unknown strategy \"" + text + "\", the strategies are "
                + Arrays.toString(values()));
    }
}
