package tests;

import code.Action;
import code.CaveExplorer;
import code.CaveMap;
import code.CaveRules;
import code.GenericSearchProblem;
import code.Node;
import code.State;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the cave explorer as a search problem: where it starts, when it has won,
 * and which nodes come out when we expand one.
 */
public class ProblemTests {

    // The PDF example (# is a wall, the number is the difficulty of the cave):
    //
    //          x=0     x=1     x=2     x=3
    //   y=0     #     3 key     #       #
    //   y=1   4 start  12       #       #
    //   y=2     #       7      23     8 door
    private static final String PDF_EXAMPLE = "3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;";

    private final CaveMap cave = new CaveMap(PDF_EXAMPLE);
    private final CaveExplorer problem = new CaveExplorer(cave);
    private final CaveRules rules = new CaveRules(cave);

    private static List<Action> actionsOf(List<Node> nodes) {
        List<Action> actions = new ArrayList<>();
        for (Node node : nodes) {
            actions.add(node.action);
        }
        return actions;
    }

    // Visits every state we can reach, using nothing but the generic problem (initialNode, expand
    // and isGoal), and counts what it finds: {different states, moves between them, winning states}.
    private static int[] explore(GenericSearchProblem problem) {
        Set<State> seen = new HashSet<>();
        Queue<Node> waiting = new ArrayDeque<>();
        Node start = problem.initialNode();
        seen.add(start.state);
        waiting.add(start);

        int moves = 0;
        int wins = 0;
        while (!waiting.isEmpty()) {
            Node node = waiting.remove();
            if (problem.isGoal(node.state)) {
                wins++;
            }
            for (Node child : problem.expand(node)) {
                moves++;
                if (seen.add(child.state)) {
                    waiting.add(child);
                }
            }
        }
        return new int[] {seen.size(), moves, wins};
    }

    // ---------------------------------------------------------------- the pieces of the problem

    @Test
    public void theStartNodeHoldsTheStartOfTheCave() {
        Node start = problem.initialNode();

        assertEquals(State.initial(cave), start.state);
        assertNull(start.parent);
        assertEquals(0, start.depth);
        assertEquals(0, start.pathCost);
    }

    @Test
    public void theOperatorsAreTheSevenActionsInTheOrderOfTheAssignment() {
        Action[] expected = {Action.LEFT, Action.RIGHT, Action.CLIMB_UP, Action.CLIMB_DOWN,
                Action.JUMP_DOWN, Action.COLLECT, Action.UNLOCK};

        assertArrayEquals(expected, problem.operators());
    }

    @Test
    public void weRecognizeAWinningState() {
        assertFalse(problem.isGoal(State.initial(cave)));
        assertTrue(problem.isGoal(new State(3, 2, 35, 0, 2, false, 0, 0)));
        // one door still locked is not a win
        assertFalse(problem.isGoal(new State(3, 2, 35, 0, 2, true, 0, 1)));
    }

    @Test
    public void aStepCostsWhatTheRulesSay() {
        State before = new State(1, 1, 73, 0, 3, true, 0, 1);

        assertEquals(1007, problem.stepCost(before, problem.result(before, Action.JUMP_DOWN)));
        assertEquals(0, problem.stepCost(before, before));
    }

    @Test
    public void resultGivesNullForAnActionThatIsNotAllowed() {
        State start = State.initial(cave);

        assertNull(problem.result(start, Action.LEFT));
        assertEquals(rules.apply(start, Action.RIGHT), problem.result(start, Action.RIGHT));
    }

    // ---------------------------------------------------------------- expanding nodes

    @Test
    public void fromTheStartThereIsOnlyOneWayToGo() {
        Node start = problem.initialNode();
        List<Node> children = problem.expand(start);

        assertEquals(1, children.size());
        Node child = children.get(0);
        assertEquals(Action.RIGHT, child.action);
        assertEquals(new State(1, 1, 88, 2, 3, false, 1, 1), child.state);
        assertSame(start, child.parent);
        assertEquals(1, child.depth);
        assertEquals(12, child.pathCost);
    }

    @Test
    public void fromThereWeCanGoFourWays() {
        Node afterRight = problem.expand(problem.initialNode()).get(0);
        List<Node> children = problem.expand(afterRight);

        // right is a wall, and there is nothing to collect or unlock in this cave
        List<Action> expected = List.of(Action.LEFT, Action.CLIMB_UP, Action.CLIMB_DOWN, Action.JUMP_DOWN);
        assertEquals(expected, actionsOf(children));
    }

    @Test
    public void everyChildKnowsItsParentItsDepthAndItsCost() {
        Node afterRight = problem.expand(problem.initialNode()).get(0);
        List<Node> children = problem.expand(afterRight);

        // the parent cost 12 so far. Then: left costs 4, climbup 3, climbdown 7, and a jump 1000 + 7.
        int[] expectedCosts = {16, 15, 19, 1019};
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            assertSame(afterRight, child.parent);
            assertEquals(2, child.depth);
            assertEquals(expectedCosts[i], child.pathCost, "child " + child.action);
            assertEquals(rules.apply(afterRight.state, child.action), child.state);
        }
    }

    @Test
    public void atTheKeyWeCanCollectIt() {
        Node node = problem.initialNode();
        node = problem.expand(node).get(0);            // right
        node = problem.expand(node).get(1);            // climbup, the second child
        assertEquals(Action.CLIMB_UP, node.action);

        List<Node> children = problem.expand(node);

        // we can go back down by climbing or jumping, or pick up the key. Collecting is free.
        assertEquals(List.of(Action.CLIMB_DOWN, Action.JUMP_DOWN, Action.COLLECT), actionsOf(children));
        assertEquals(node.pathCost, children.get(2).pathCost);
        assertTrue(children.get(2).state.holdingKey);
    }

    @Test
    public void aStateWithNoWayOutHasNoChildren() {
        // no energy, no rope and only one life: nothing is allowed any more
        Node stuck = new Node(new State(1, 1, 0, 0, 1, false, 1, 1));

        assertTrue(problem.expand(stuck).isEmpty());
    }

    @Test
    public void expandingTwiceGivesTheSameChildrenInTheSameOrder() {
        Node afterRight = problem.expand(problem.initialNode()).get(0);
        List<Node> first = problem.expand(afterRight);
        List<Node> second = problem.expand(afterRight);

        assertEquals(actionsOf(first), actionsOf(second));
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).state, second.get(i).state);
            assertNotSame(first.get(i), second.get(i));   // they are new nodes every time
        }
    }

    @Test
    public void expandUsesWhateverPiecesTheProblemProvides() {
        Node afterRight = problem.expand(problem.initialNode()).get(0);

        // a problem that only has two operators: going right and climbing up
        CaveExplorer fewOperators = new CaveExplorer(cave) {
            @Override
            public Action[] operators() {
                return new Action[] {Action.RIGHT, Action.CLIMB_UP};
            }
        };
        assertEquals(List.of(Action.CLIMB_UP), actionsOf(fewOperators.expand(afterRight)));

        // a problem where nothing is ever allowed
        CaveExplorer nothingAllowed = new CaveExplorer(cave) {
            @Override
            public State result(State state, Action action) {
                return null;
            }
        };
        assertTrue(nothingAllowed.expand(nothingAllowed.initialNode()).isEmpty());

        // a problem where every step costs 5
        CaveExplorer everythingCostsFive = new CaveExplorer(cave) {
            @Override
            public int stepCost(State before, State after) {
                return 5;
            }
        };
        Node first = everythingCostsFive.expand(everythingCostsFive.initialNode()).get(0);
        Node second = everythingCostsFive.expand(first).get(0);
        assertEquals(5, first.pathCost);
        assertEquals(10, second.pathCost);
    }

    @Test
    public void theNodeWeExpandIsNotChanged() {
        Node start = problem.initialNode();
        problem.expand(start);

        assertEquals(State.initial(cave), start.state);
        assertEquals(0, start.depth);
        assertEquals(0, start.pathCost);
    }

    // ---------------------------------------------------------------- exploring a whole cave

    // The numbers below were counted separately, with a small script that has its own copy of the
    // rules. If expanding nodes is right, we have to find exactly the same numbers.

    @Test
    public void thePdfCaveHasTheStatesMovesAndWinsWeCountedSeparately() {
        assertArrayEquals(new int[] {368, 462, 14}, explore(problem));
    }

    @Test
    public void aCaveWithExactlyOneWinningState() {
        CaveMap oneWin = new CaveMap("6,7;0,5;99,5;6,8,10,12,14,16,10;7,0,0,0,0,0,12;5,0,0,0,0,0,14;4,0,0,0,0,0,16;3,6,8,45,0,0,20;2,0,0,0,0,0,14;6,0;0,2;");

        assertArrayEquals(new int[] {3694, 6192, 1}, explore(new CaveExplorer(oneWin)));
    }

    @Test
    public void aCaveWithTooLittleRopeHasNoWinningState() {
        // the cave of test_plan_uc6, which expects "No Solution"
        CaveMap noWay = new CaveMap("7,8;0,1;120,2;0,3,0,0,8,4,0,2;4,9,5,0,2,0,7,0;0,1,8,8,6,9,6,0;0,2,7,3,4,4,0,2;7,0,1,3,2,0,3,9;7,0,2,6,0,1,4,0;5,0,9,4,4,3,8,5;3,2,5,6;1,0,5,3,0,4;");

        assertArrayEquals(new int[] {14350, 33103, 0}, explore(new CaveExplorer(noWay)));
    }

    @Test
    public void theBiggestPublicCave() {
        // the same cave with 5 meters of rope instead of 2 can be won
        CaveMap big = new CaveMap("7,8;0,1;120,5;0,3,0,0,8,4,0,2;4,9,5,0,2,0,7,0;0,1,8,8,6,9,6,0;0,2,7,3,4,4,0,2;7,0,1,3,2,0,3,9;7,0,2,6,0,1,4,0;5,0,9,4,4,3,8,5;3,2,5,6;1,0,5,3,0,4;");

        assertArrayEquals(new int[] {63192, 164609, 220}, explore(new CaveExplorer(big)));
    }
}
