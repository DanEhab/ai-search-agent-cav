package tests;

import code.Action;
import code.CaveMap;
import code.CaveRules;
import code.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the rules of the cave: when an action is allowed and what it changes.
 */
public class RulesTests {

    // The PDF example. Most tests use it, so here it is drawn out (# is a wall, the number
    // is the difficulty of the cave):
    //
    //          x=0     x=1     x=2     x=3
    //   y=0     #     3 key     #       #
    //   y=1   4 start  12       #       #
    //   y=2     #       7      23     8 door
    private static final String PDF_EXAMPLE = "3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;";

    // the cave with 7 rows that has 3 keys and 2 doors
    private static final String BIG_CAVE =
            "7,8;0,1;120,5;0,3,0,0,8,4,0,2;4,9,5,0,2,0,7,0;0,1,8,8,6,9,6,0;0,2,7,3,4,4,0,2;7,0,1,3,2,0,3,9;7,0,2,6,0,1,4,0;5,0,9,4,4,3,8,5;3,2,5,6;1,0,5,3,0,4;";

    // plans that are known to work, each one for the cave named above it
    private static final String PDF_PLAN = "right,climbup,collect,climbdown,jumpdown,right,right,unlock";

    // the cave of test_plan_uc_cost1, whose cheapest plan costs 0 lives and 99 energy
    private static final String FIVE_BY_SIX_CAVE =
            "5,6;0,4;300,15;6,10,14,8,28,12;5,0,0,7,0,9;4,8,24,18,6,11;7,0,12,0,0,13;3,5,9,15,35,7;5,0;3,2;";
    private static final String FIVE_BY_SIX_PLAN =
            "climbup,climbup,right,right,right,collect,right,right,climbup,climbup,unlock";

    // the cheapest plan for BIG_CAVE, the one test_plan_uc_cost2 is about: 2 lives and 75 energy
    private static final String BIG_PLAN = "right,climbup,collect,climbdown,climbdown,right,right,unlock,climbdown,"
            + "right,right,collect,left,climbdown,left,jumpdown,jumpdown,right,right,unlock";

    private final CaveRules rules = new CaveRules(new CaveMap(PDF_EXAMPLE));

    // a state in the PDF example with nothing in hand, the key still on the floor and the door still locked
    private static State at(int x, int y, int energy, int rope, int lives) {
        return new State(x, y, energy, rope, lives, false, 1, 1);
    }

    // Runs a plan from the start of a cave. Gives back the final state, or null if some step is not allowed.
    private static State run(CaveMap cave, Action... plan) {
        CaveRules caveRules = new CaveRules(cave);
        State s = State.initial(cave);
        for (Action action : plan) {
            s = caveRules.apply(s, action);
            if (s == null) {
                return null;
            }
        }
        return s;
    }

    // ---------------------------------------------------------------- left and right

    @Test
    public void walkingCostsTheEnergyOfTheCaveYouEnter() {
        // cave (1,1) has difficulty 12 and cave (0,1) has difficulty 4
        assertEquals(at(1, 1, 88, 2, 3), rules.apply(at(0, 1, 100, 2, 3), Action.RIGHT));
        assertEquals(at(0, 1, 84, 2, 3), rules.apply(at(1, 1, 88, 2, 3), Action.LEFT));
    }

    @Test
    public void youCannotWalkIntoAWallOrOffTheGrid() {
        assertNull(rules.apply(at(1, 1, 100, 2, 3), Action.RIGHT));   // (2,1) is a wall
        assertNull(rules.apply(at(0, 1, 100, 2, 3), Action.LEFT));    // x would be -1
        assertNull(rules.apply(at(3, 2, 100, 2, 3), Action.RIGHT));   // x would be 4
    }

    @Test
    public void youNeedEnoughEnergyToEnterACave() {
        // the cave on the left, (0,1), has difficulty 4
        assertNull(rules.apply(at(1, 1, 3, 2, 3), Action.LEFT));
        // exactly 4 is enough, and then nothing is left
        assertEquals(at(0, 1, 0, 2, 3), rules.apply(at(1, 1, 4, 2, 3), Action.LEFT));
    }

    // ---------------------------------------------------------------- climbing

    @Test
    public void climbingUpUsesRopeAndEnergy() {
        // cave (1,0) has difficulty 3
        assertEquals(at(1, 0, 85, 1, 3), rules.apply(at(1, 1, 88, 2, 3), Action.CLIMB_UP));
    }

    @Test
    public void climbingDownUsesRopeAndEnergy() {
        // cave (1,2) has difficulty 7
        assertEquals(at(1, 2, 81, 1, 3), rules.apply(at(1, 1, 88, 2, 3), Action.CLIMB_DOWN));
    }

    @Test
    public void climbingNeedsRope() {
        assertNull(rules.apply(at(1, 1, 88, 0, 3), Action.CLIMB_UP));
        assertNull(rules.apply(at(1, 1, 88, 0, 3), Action.CLIMB_DOWN));
        // one meter is enough, and then it is gone
        assertEquals(at(1, 0, 85, 0, 3), rules.apply(at(1, 1, 88, 1, 3), Action.CLIMB_UP));
    }

    @Test
    public void youCannotClimbIntoAWallOrOffTheGrid() {
        assertNull(rules.apply(at(0, 1, 100, 5, 3), Action.CLIMB_UP));     // (0,0) is a wall
        assertNull(rules.apply(at(0, 1, 100, 5, 3), Action.CLIMB_DOWN));   // (0,2) is a wall
        assertNull(rules.apply(at(1, 0, 100, 5, 3), Action.CLIMB_UP));     // y would be -1
        assertNull(rules.apply(at(1, 2, 100, 5, 3), Action.CLIMB_DOWN));   // y would be 3
    }

    @Test
    public void climbingNeedsEnoughEnergyToo() {
        assertNull(rules.apply(at(1, 1, 2, 5, 3), Action.CLIMB_UP));       // cave (1,0) needs 3
        assertNull(rules.apply(at(1, 1, 6, 5, 3), Action.CLIMB_DOWN));     // cave (1,2) needs 7
    }

    // ---------------------------------------------------------------- jumping

    @Test
    public void jumpingDownCostsALifeAndEnergyButNoRope() {
        // like step 5 of the PDF example: the rope is gone, so the explorer jumps
        assertEquals(at(1, 2, 66, 0, 2), rules.apply(at(1, 1, 73, 0, 3), Action.JUMP_DOWN));
        // with rope in the bag, the rope stays untouched
        assertEquals(at(1, 2, 81, 2, 2), rules.apply(at(1, 1, 88, 2, 3), Action.JUMP_DOWN));
    }

    @Test
    public void youCannotJumpWithOnlyOneLifeLeft() {
        assertNull(rules.apply(at(1, 1, 88, 2, 1), Action.JUMP_DOWN));
        // with two lives it works, and the last life stays
        assertEquals(at(1, 2, 81, 2, 1), rules.apply(at(1, 1, 88, 2, 2), Action.JUMP_DOWN));
    }

    @Test
    public void youCannotJumpIntoAWallOrOffTheGrid() {
        assertNull(rules.apply(at(0, 1, 100, 2, 3), Action.JUMP_DOWN));    // (0,2) is a wall
        assertNull(rules.apply(at(1, 2, 100, 2, 3), Action.JUMP_DOWN));    // y would be 3
    }

    @Test
    public void jumpingNeedsEnoughEnergyToo() {
        assertNull(rules.apply(at(1, 1, 6, 2, 3), Action.JUMP_DOWN));      // cave (1,2) needs 7
    }

    // ---------------------------------------------------------------- collecting keys

    @Test
    public void collectingPicksUpTheKey() {
        // the key lies in cave (1,0). Nothing else changes: same place, same energy, rope and lives.
        State before = new State(1, 0, 85, 1, 3, false, 1, 1);
        State expected = new State(1, 0, 85, 1, 3, true, 0, 1);

        assertEquals(expected, rules.apply(before, Action.COLLECT));
    }

    @Test
    public void collectingNeedsAKeyInTheCave() {
        assertNull(rules.apply(at(1, 1, 88, 2, 3), Action.COLLECT));
    }

    @Test
    public void youCanCarryOnlyOneKey() {
        assertNull(rules.apply(new State(1, 0, 85, 1, 3, true, 1, 1), Action.COLLECT));
    }

    @Test
    public void aKeyThatWasAlreadyTakenIsGone() {
        assertNull(rules.apply(new State(1, 0, 85, 1, 3, false, 0, 1), Action.COLLECT));
    }

    // ---------------------------------------------------------------- unlocking doors

    @Test
    public void unlockingOpensTheDoorAndUsesUpTheKey() {
        // the door is in cave (3,2)
        State before = new State(3, 2, 35, 0, 2, true, 0, 1);
        State expected = new State(3, 2, 35, 0, 2, false, 0, 0);

        assertEquals(expected, rules.apply(before, Action.UNLOCK));
    }

    @Test
    public void unlockingNeedsAKeyInHand() {
        assertNull(rules.apply(new State(3, 2, 35, 0, 2, false, 0, 1), Action.UNLOCK));
    }

    @Test
    public void unlockingNeedsADoorInTheCave() {
        assertNull(rules.apply(new State(1, 1, 88, 2, 3, true, 1, 1), Action.UNLOCK));
    }

    @Test
    public void anOpenDoorCannotBeUnlockedAgain() {
        assertNull(rules.apply(new State(3, 2, 35, 0, 2, true, 1, 0), Action.UNLOCK));
    }

    // ---------------------------------------------------------------- other checks

    @Test
    public void theOldStateIsNeverChanged() {
        State before = at(0, 1, 100, 2, 3);
        rules.apply(before, Action.RIGHT);

        assertEquals(at(0, 1, 100, 2, 3), before);
    }

    @Test
    public void everyKeyAndDoorUsesItsOwnBit() {
        // in this cave the keys are in (1,0) (5,3) (0,4) and the doors are in (3,2) (5,6)
        CaveRules big = new CaveRules(new CaveMap(BIG_CAVE));

        // all 3 keys on the floor is binary 111. Taking key 0, 1 or 2 clears just that bit.
        assertEquals(new State(1, 0, 50, 5, 3, true, 6, 3), big.apply(new State(1, 0, 50, 5, 3, false, 7, 3), Action.COLLECT));
        assertEquals(new State(5, 3, 50, 5, 3, true, 5, 3), big.apply(new State(5, 3, 50, 5, 3, false, 7, 3), Action.COLLECT));
        assertEquals(new State(0, 4, 50, 5, 3, true, 3, 3), big.apply(new State(0, 4, 50, 5, 3, false, 7, 3), Action.COLLECT));

        // both doors locked is binary 11. Opening door 0 or 1 clears just that bit.
        assertEquals(new State(3, 2, 50, 5, 3, false, 7, 2), big.apply(new State(3, 2, 50, 5, 3, true, 7, 3), Action.UNLOCK));
        assertEquals(new State(5, 6, 50, 5, 3, false, 7, 1), big.apply(new State(5, 6, 50, 5, 3, true, 7, 3), Action.UNLOCK));
    }

    @Test
    public void actionNamesAreSpelledLikeTheAssignment() {
        StringBuilder names = new StringBuilder();
        for (Action action : Action.values()) {
            if (names.length() > 0) {
                names.append(",");
            }
            names.append(action);
        }

        assertEquals("left,right,climbup,climbdown,jumpdown,collect,unlock", names.toString());
    }

    // ---------------------------------------------------------------- whole plans

    @Test
    public void thePdfExamplePlanWorksStepByStep() {
        CaveMap cave = new CaveMap(PDF_EXAMPLE);
        State end = run(cave, Action.RIGHT, Action.CLIMB_UP, Action.COLLECT, Action.CLIMB_DOWN,
                Action.JUMP_DOWN, Action.RIGHT, Action.RIGHT, Action.UNLOCK);

        // the PDF says this plan uses 1 life and 65 energy: 2 lives and 100 - 65 = 35 energy are left
        assertEquals(new State(3, 2, 35, 0, 2, false, 0, 0), end);
    }

    @Test
    public void aLongerPlanOnAnotherCaveWorks() {
        // the cave of test_plan_uc_cost1, whose cheapest plan uses 0 lives and 99 energy
        CaveMap cave = new CaveMap(FIVE_BY_SIX_CAVE);
        State end = run(cave, Action.CLIMB_UP, Action.CLIMB_UP, Action.RIGHT, Action.RIGHT, Action.RIGHT,
                Action.COLLECT, Action.RIGHT, Action.RIGHT, Action.CLIMB_UP, Action.CLIMB_UP, Action.UNLOCK);

        // 300 - 99 = 201 energy left, 4 meters of rope used and no life lost
        assertEquals(new State(5, 0, 201, 11, 3, false, 0, 0), end);
    }

    @Test
    public void aPlanWithTwoKeysAndTwoJumpsWorks() {
        // the cheapest plan for the biggest public cave: 2 doors, 2 keys, 2 jumps, all the rope
        CaveMap cave = new CaveMap(BIG_CAVE);
        State end = run(cave, Action.RIGHT, Action.CLIMB_UP, Action.COLLECT, Action.CLIMB_DOWN, Action.CLIMB_DOWN,
                Action.RIGHT, Action.RIGHT, Action.UNLOCK, Action.CLIMB_DOWN, Action.RIGHT, Action.RIGHT,
                Action.COLLECT, Action.LEFT, Action.CLIMB_DOWN, Action.LEFT, Action.JUMP_DOWN, Action.JUMP_DOWN,
                Action.RIGHT, Action.RIGHT, Action.UNLOCK);

        // 120 - 75 = 45 energy left, no rope, 1 life, and the third key (binary 100) was never needed
        assertEquals(new State(5, 6, 45, 0, 1, false, 4, 0), end);
    }

    @Test
    public void theSamePlanFailsWhenTheRopeIsTooShort() {
        // the PDF example again, but with 1 meter of rope instead of 2
        CaveMap shortRope = new CaveMap("3,4;0,1;100,1;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;");

        // the first three steps are fine, the climb up uses the only meter...
        assertNotNull(run(shortRope, Action.RIGHT, Action.CLIMB_UP, Action.COLLECT));
        // ...so climbing back down is not possible any more
        assertNull(run(shortRope, Action.RIGHT, Action.CLIMB_UP, Action.COLLECT, Action.CLIMB_DOWN));
    }

    // ---------------------------------------------------------------- winning

    @Test
    public void weWinWhenNoDoorIsLockedAnyMore() {
        assertTrue(rules.isGoal(new State(3, 2, 35, 0, 2, false, 0, 0)));
        // it makes no difference where we stand or whether we still hold a key
        assertTrue(rules.isGoal(new State(1, 1, 50, 0, 3, true, 0, 0)));
        // a door that is still locked means we are not done
        assertFalse(rules.isGoal(new State(3, 2, 35, 0, 2, true, 0, 1)));
    }

    @Test
    public void everyDoorMustBeOpen() {
        // the big cave has 2 doors. Binary 01 means door 0 is open but door 1 is still locked.
        CaveRules big = new CaveRules(new CaveMap(BIG_CAVE));

        assertFalse(big.isGoal(new State(5, 6, 50, 0, 3, false, 7, 3)));
        assertFalse(big.isGoal(new State(5, 6, 50, 0, 3, false, 7, 1)));
        assertTrue(big.isGoal(new State(5, 6, 50, 0, 3, false, 7, 0)));
    }

    @Test
    public void noCaveStartsAlreadyWon() {
        for (String text : new String[] {PDF_EXAMPLE, BIG_CAVE, FIVE_BY_SIX_CAVE}) {
            CaveMap cave = new CaveMap(text);
            assertFalse(new CaveRules(cave).isGoal(State.initial(cave)));
        }
    }

    // ---------------------------------------------------------------- what a step costs

    @Test
    public void aWalkCostsTheDifficultyOfTheCaveYouEnter() {
        State before = at(0, 1, 100, 2, 3);

        assertEquals(12, rules.stepCost(before, rules.apply(before, Action.RIGHT)));
    }

    @Test
    public void climbingCostsEnergyButTheRopeIsNotCounted() {
        State before = at(1, 1, 88, 2, 3);

        assertEquals(3, rules.stepCost(before, rules.apply(before, Action.CLIMB_UP)));
        assertEquals(7, rules.stepCost(before, rules.apply(before, Action.CLIMB_DOWN)));
    }

    @Test
    public void aJumpCostsALifePlusTheDifficultyOfTheLandingCave() {
        State before = at(1, 1, 73, 0, 3);

        assertEquals(1007, rules.stepCost(before, rules.apply(before, Action.JUMP_DOWN)));
    }

    @Test
    public void collectingAndUnlockingAreFree() {
        State atKey = new State(1, 0, 85, 1, 3, false, 1, 1);
        State atDoor = new State(3, 2, 35, 0, 2, true, 0, 1);

        assertEquals(0, rules.stepCost(atKey, rules.apply(atKey, Action.COLLECT)));
        assertEquals(0, rules.stepCost(atDoor, rules.apply(atDoor, Action.UNLOCK)));
    }

    @Test
    public void oneLifeCostsMoreThanAllTheEnergyThereCanBe() {
        // a cave never starts with more than 500 energy
        assertTrue(CaveRules.LIFE_COST > 500);
    }

    @Test
    public void theStepsOfThePdfPlanCostWhatWeExpect() {
        String[] words = PDF_PLAN.split(",");
        int[] expected = {12, 3, 0, 12, 1007, 23, 8, 0};
        State s = State.initial(new CaveMap(PDF_EXAMPLE));
        int total = 0;

        for (int i = 0; i < words.length; i++) {
            State next = rules.apply(s, Action.fromText(words[i]));
            assertEquals(expected[i], rules.stepCost(s, next), "step " + (i + 1) + " (" + words[i] + ")");
            total += rules.stepCost(s, next);
            s = next;
        }

        // that is 1 life and 65 energy
        assertEquals(1065, total);
    }

    @Test
    public void stepCostsAddUpToTheLivesAndEnergyUsed() {
        // For any plan, the cost of all its steps is LIFE_COST * lives used + energy used.
        // The totals are what the public tests expect: 1 life and 65 energy, 0 and 99, 2 and 75.
        String[] caves = {PDF_EXAMPLE, FIVE_BY_SIX_CAVE, BIG_CAVE};
        String[] plans = {PDF_PLAN, FIVE_BY_SIX_PLAN, BIG_PLAN};
        int[] totals = {1065, 99, 2075};

        for (int i = 0; i < caves.length; i++) {
            CaveMap cave = new CaveMap(caves[i]);
            CaveRules caveRules = new CaveRules(cave);
            State s = State.initial(cave);
            int sum = 0;
            for (String word : plans[i].split(",")) {
                State next = caveRules.apply(s, Action.fromText(word));
                sum += caveRules.stepCost(s, next);
                s = next;
            }

            assertEquals(totals[i], sum, plans[i]);
            assertEquals(CaveRules.LIFE_COST * caveRules.livesUsed(s) + caveRules.energyUsed(s), sum, plans[i]);
        }
    }

    @Test
    public void livesAndEnergyUsedAreCountedFromTheStart() {
        State start = State.initial(new CaveMap(PDF_EXAMPLE));
        State end = rules.replay(PDF_PLAN);

        assertEquals(0, rules.livesUsed(start));
        assertEquals(0, rules.energyUsed(start));
        assertEquals(1, rules.livesUsed(end));
        assertEquals(65, rules.energyUsed(end));
    }

    // ---------------------------------------------------------------- replaying a plan written as text

    @Test
    public void replayRunsAPlanWrittenAsText() {
        State end = rules.replay(PDF_PLAN);

        assertEquals(new State(3, 2, 35, 0, 2, false, 0, 0), end);
        assertTrue(rules.isGoal(end));
    }

    @Test
    public void replayGivesNullWhenAStepIsNotAllowed() {
        assertNull(rules.replay("left"));           // off the grid
        assertNull(rules.replay("right,right"));    // cave (2,1) is a wall

        // with 1 meter of rope the climb back down is not possible
        CaveRules shortRope = new CaveRules(new CaveMap("3,4;0,1;100,1;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;"));
        assertNull(shortRope.replay("right,climbup,collect,climbdown"));
    }

    @Test
    public void replayOfAnEmptyPlanIsTheStartState() {
        assertEquals(State.initial(new CaveMap(PDF_EXAMPLE)), rules.replay(""));
    }

    @Test
    public void replayDoesNotMindSpacesAroundTheWords() {
        assertEquals(rules.replay("right,climbup"), rules.replay(" right , climbup "));
    }

    @Test
    public void replayRejectsNamesThatAreNotActions() {
        assertThrows(IllegalArgumentException.class, () -> rules.replay("right,fly"));
        assertThrows(IllegalArgumentException.class, () -> rules.replay("RIGHT"));     // names are lower case
        assertThrows(IllegalArgumentException.class, () -> rules.replay("right,,left"));
    }

    @Test
    public void everyActionNameReadsBackAsTheSameAction() {
        for (Action action : Action.values()) {
            assertEquals(action, Action.fromText(action.toString()));
        }
    }

    @Test
    public void theCheapestPlansCostWhatThePublicTestsExpect() {
        // test_plan_uc_cost1 expects 0 lives and 99 energy
        CaveRules fiveBySix = new CaveRules(new CaveMap(FIVE_BY_SIX_CAVE));
        State end = fiveBySix.replay(FIVE_BY_SIX_PLAN);
        assertTrue(fiveBySix.isGoal(end));
        assertEquals(0, fiveBySix.livesUsed(end));
        assertEquals(99, fiveBySix.energyUsed(end));

        // test_plan_uc_cost2 expects 2 lives and 75 energy
        CaveRules big = new CaveRules(new CaveMap(BIG_CAVE));
        end = big.replay(BIG_PLAN);
        assertTrue(big.isGoal(end));
        assertEquals(2, big.livesUsed(end));
        assertEquals(75, big.energyUsed(end));
    }
    // ---------------------------------------------------------------- the real checker

    // The checker from the course is the judge of the rules. Every plan below was tried on it,
    // and our rules have to say the same thing as the checker does.


    // A plan that our rules allow and that opens every door. The checker has to accept it
    // with the lives and energy we work out ourselves.
    private static void checkerAccepts(String caveText, String plan) {
        CaveRules caveRules = new CaveRules(new CaveMap(caveText));
        State end = caveRules.replay(plan);

        assertNotNull(end, "our rules refuse " + plan);
        assertTrue(caveRules.isGoal(end), "our rules say a door is still locked after " + plan);
        String answer = plan + ";" + caveRules.livesUsed(end) + ";" + caveRules.energyUsed(end) + ";1";
        Checker.ValidationResult result = Checker.validateSolution(caveText, answer);
        assertTrue(result.isValid, "the checker refuses " + answer + " (" + result.errorMessage + ")");
    }

    // A plan that our rules do not allow, or that leaves a door locked. The checker has to refuse it too.
    // "claimed" is the lives and energy the plan would cost if it were allowed, so the checker can only
    // refuse it because of the plan itself and not because of wrong numbers.
    private static void checkerRefuses(String caveText, String plan, String claimed) {
        CaveRules caveRules = new CaveRules(new CaveMap(caveText));
        State end = caveRules.replay(plan);

        assertTrue(end == null || !caveRules.isGoal(end), "our rules accept " + plan);
        Checker.ValidationResult result = Checker.validateSolution(caveText, plan + ";" + claimed + ";1");
        assertFalse(result.isValid, "the checker accepts " + plan);
    }

    @Test
    public void theCheckerAcceptsEveryPlanOurRulesAllow() {
        checkerAccepts(PDF_EXAMPLE, PDF_PLAN);

        // exactly 65 energy is enough for that plan, so energy may drop to 0
        checkerAccepts("3,4;0,1;65,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;", PDF_PLAN);

        // a shaft 4 caves deep: two jumps leave one life, and jumping needs no rope (rope is 0 here)
        checkerAccepts("4,3;1,0;100,0;1,1,1;0,1,0;0,1,0;0,1,0;1,2;0,0;", "left,collect,right,jumpdown,jumpdown,unlock");

        // exactly enough energy for the last step of each kind of move
        checkerAccepts("3,3;1,1;5,1;0,5,0;0,2,0;0,0,0;1,0;1,1;", "collect,climbup,unlock");
        checkerAccepts("3,3;1,1;7,1;0,0,0;0,2,0;0,7,0;1,2;1,1;", "collect,climbdown,unlock");
        checkerAccepts("3,3;1,1;7,0;0,0,0;0,2,0;0,7,0;1,2;1,1;", "collect,jumpdown,unlock");

        // two doors that need two different keys
        checkerAccepts("3,4;0,0;100,0;1,1,1,1;0,0,0,0;0,0,0,0;2,0,0,0;1,0,3,0;",
                "right,collect,right,unlock,right,collect,left,left,left,unlock");

        // the cheapest plans of two public caves
        checkerAccepts(FIVE_BY_SIX_CAVE, FIVE_BY_SIX_PLAN);
        checkerAccepts(BIG_CAVE, BIG_PLAN);
    }

    @Test
    public void theCheckerRefusesEveryPlanOurRulesRefuse() {
        // 1 meter of rope is not enough for climbing up and then down
        checkerRefuses("3,4;0,1;100,1;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;", PDF_PLAN, "1;65");

        // 64 energy is one short for the last step
        checkerRefuses("3,4;0,1;64,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;", PDF_PLAN, "1;65");

        // a third jump would use the last life
        checkerRefuses("4,3;1,0;100,0;1,1,1;0,1,0;0,1,0;0,1,0;1,3;0,0;",
                "left,collect,right,jumpdown,jumpdown,jumpdown,unlock", "3;5");

        // one energy short for the last step of each kind of move
        checkerRefuses("3,3;1,1;4,1;0,5,0;0,2,0;0,0,0;1,0;1,1;", "collect,climbup,unlock", "0;5");
        checkerRefuses("3,3;1,1;6,1;0,0,0;0,2,0;0,7,0;1,2;1,1;", "collect,climbdown,unlock", "0;7");
        checkerRefuses("3,3;1,1;6,0;0,0,0;0,2,0;0,7,0;1,2;1,1;", "collect,jumpdown,unlock", "1;7");

        // climbing without any rope
        checkerRefuses("3,3;1,1;50,0;0,0,0;0,2,0;0,7,0;1,2;1,1;", "collect,climbdown,unlock", "0;7");
        checkerRefuses("3,3;1,1;50,0;0,5,0;0,2,0;0,0,0;1,0;1,1;", "collect,climbup,unlock", "0;5");

        // taking the same key a second time, after it was used
        checkerRefuses("3,4;0,0;100,0;1,1,1,1;0,0,0,0;0,0,0,0;2,0,0,0;1,0,3,0;",
                "right,collect,right,unlock,left,collect,left,unlock", "0;4");

        // carrying two keys at once
        checkerRefuses("3,3;0,0;100,0;1,1,1;0,0,0;0,0,0;2,0;0,0,1,0;", "collect,right,collect,right,unlock", "0;2");

        // unlocking with empty hands
        checkerRefuses(PDF_EXAMPLE, "right,unlock", "0;12");

        // every move is allowed, but the door is still locked at the end
        checkerRefuses(PDF_EXAMPLE, "right", "0;12");
    }
}
