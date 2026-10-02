package tests;

import code.Action;
import code.CaveMap;
import code.CaveRules;
import code.Node;
import code.State;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the search tree node: the trail of parents, depth, cost and the plan we read off it.
 */
public class NodeTests {

    private static final String PDF_EXAMPLE = "3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;";
    private static final String PDF_PLAN = "right,climbup,collect,climbdown,jumpdown,right,right,unlock";

    // the cave with 7 rows and its cheapest plan: 2 lives and 75 energy
    private static final String BIG_CAVE =
            "7,8;0,1;120,5;0,3,0,0,8,4,0,2;4,9,5,0,2,0,7,0;0,1,8,8,6,9,6,0;0,2,7,3,4,4,0,2;7,0,1,3,2,0,3,9;7,0,2,6,0,1,4,0;5,0,9,4,4,3,8,5;3,2,5,6;1,0,5,3,0,4;";
    private static final String BIG_PLAN = "right,climbup,collect,climbdown,climbdown,right,right,unlock,climbdown,"
            + "right,right,collect,left,climbdown,left,jumpdown,jumpdown,right,right,unlock";

    // most of these tests do not care which state is inside the nodes, so any state will do
    private static State someState() {
        return new State(0, 1, 100, 2, 3, false, 1, 1);
    }

    private static Node child(Node parent, Action action, int stepCost) {
        return new Node(parent, action, someState(), stepCost);
    }

    // Walks a whole plan through the rules and gives back the node at the end of it.
    private static Node nodeAfter(String caveText, String plan) {
        CaveMap cave = new CaveMap(caveText);
        CaveRules rules = new CaveRules(cave);
        Node node = new Node(State.initial(cave));

        for (String word : plan.split(",")) {
            Action action = Action.fromText(word);
            State next = rules.apply(node.state, action);
            node = new Node(node, action, next, rules.stepCost(node.state, next));
        }
        return node;
    }

    @Test
    public void theRootHasNoParentAndNoAction() {
        State start = someState();
        Node root = new Node(start);

        assertSame(start, root.state);
        assertNull(root.parent);
        assertNull(root.action);
        assertEquals(0, root.depth);
        assertEquals(0, root.pathCost);
    }

    @Test
    public void aChildPointsBackToItsParent() {
        Node root = new Node(someState());
        State next = new State(1, 1, 88, 2, 3, false, 1, 1);
        Node child = new Node(root, Action.RIGHT, next, 12);

        assertSame(root, child.parent);
        assertSame(next, child.state);
        assertEquals(Action.RIGHT, child.action);
    }

    @Test
    public void depthCountsTheStepsFromTheRoot() {
        Node root = new Node(someState());
        Node one = child(root, Action.RIGHT, 12);
        Node two = child(one, Action.CLIMB_UP, 3);
        Node three = child(two, Action.COLLECT, 0);

        assertEquals(0, root.depth);
        assertEquals(1, one.depth);
        assertEquals(2, two.depth);
        assertEquals(3, three.depth);
    }

    @Test
    public void pathCostAddsUpTheCostOfEveryStep() {
        Node root = new Node(someState());
        Node one = child(root, Action.RIGHT, 12);
        Node two = child(one, Action.CLIMB_UP, 3);
        Node three = child(two, Action.COLLECT, 0);

        assertEquals(0, root.pathCost);
        assertEquals(12, one.pathCost);
        assertEquals(15, two.pathCost);
        assertEquals(15, three.pathCost);
    }

    @Test
    public void thePlanComesBackInTheOrderItWasTaken() {
        Node root = new Node(someState());
        Node one = child(root, Action.RIGHT, 12);
        Node two = child(one, Action.CLIMB_UP, 3);
        Node three = child(two, Action.COLLECT, 0);

        assertEquals(Arrays.asList(Action.RIGHT, Action.CLIMB_UP, Action.COLLECT), three.plan());
        assertEquals("right,climbup,collect", three.planText());

        // a node in the middle only knows the way up to itself
        assertEquals(Arrays.asList(Action.RIGHT, Action.CLIMB_UP), two.plan());
        assertEquals("right,climbup", two.planText());
    }

    @Test
    public void theRootHasAnEmptyPlan() {
        Node root = new Node(someState());

        assertTrue(root.plan().isEmpty());
        assertEquals("", root.planText());
    }

    @Test
    public void twoNodesCanHoldTheSameState() {
        // Two different paths can end in the same state. Here we pretend that going right and
        // then left brings us back: the second node holds the same state as the root, but its
        // path is longer and costs more.
        Node root = new Node(someState());
        Node there = child(root, Action.RIGHT, 12);
        Node back = child(there, Action.LEFT, 4);

        assertEquals(root.state, back.state);
        assertNotEquals(root, back);
        assertEquals(0, root.pathCost);
        assertEquals(16, back.pathCost);
    }

    @Test
    public void aVeryLongTrailStillWorks() {
        // reading the plan back must not run out of stack space, however deep the node is
        Node node = new Node(someState());
        for (int i = 0; i < 50000; i++) {
            node = child(node, Action.RIGHT, 1);
        }

        assertEquals(50000, node.depth);
        assertEquals(50000, node.pathCost);
        assertEquals(50000, node.plan().size());
    }

    @Test
    public void nodesBuiltFromThePdfPlanGiveThatPlanBack() {
        CaveRules rules = new CaveRules(new CaveMap(PDF_EXAMPLE));
        Node end = nodeAfter(PDF_EXAMPLE, PDF_PLAN);

        assertEquals(8, end.depth);
        assertEquals(1065, end.pathCost);                       // 1 life and 65 energy
        assertEquals(PDF_PLAN, end.planText());
        assertTrue(rules.isGoal(end.state));
        assertEquals(rules.replay(end.planText()), end.state);  // replaying the plan lands in the same state
    }

    @Test
    public void nodesBuiltFromTheBiggestPlanGiveThatPlanBack() {
        CaveRules rules = new CaveRules(new CaveMap(BIG_CAVE));
        Node end = nodeAfter(BIG_CAVE, BIG_PLAN);

        assertEquals(20, end.depth);
        assertEquals(2075, end.pathCost);                       // 2 lives and 75 energy
        assertEquals(BIG_PLAN, end.planText());
        assertTrue(rules.isGoal(end.state));
        assertEquals(rules.replay(end.planText()), end.state);
    }

    @Test
    public void toStringShowsTheTrail() {
        Node root = new Node(someState());
        Node child = child(root, Action.RIGHT, 12);

        assertEquals("Node(depth=0, pathCost=0, action=none, " + someState() + ")", root.toString());
        assertEquals("Node(depth=1, pathCost=12, action=right, " + someState() + ")", child.toString());
    }
}
