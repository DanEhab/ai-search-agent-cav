package tests;

import code.CaveExplorer;
import code.CaveMap;
import code.CaveRules;
import code.Search;
import code.State;
import code.Strategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for solve(): the answer text has to look the way the assignment wants it.
 */
public class SolveTests {

    private static final String PDF_EXAMPLE = "3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;";

    // every cave the public tests use, and what their cheapest plans cost in lives and energy
    // (-1 means the cave cannot be won)
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
    private static final int[] LIVES_USED = {0, 0, 0, 0, 0, 0, 2, -1, 2};
    private static final int[] ENERGY_USED = {53, 55, 99, 95, 95, 95, 75, -1, 75};

    // ---------------------------------------------------------------- what the answer looks like

    @Test
    public void theLastNumberIsHowManyNodesTheSearchExpanded() {
        for (String text : new String[] {PDF_EXAMPLE, PUBLIC_CAVES[0], PUBLIC_CAVES[3]}) {
            Search search = new Search();
            search.run(new CaveExplorer(new CaveMap(text)), Strategy.UC);
            String[] parts = CaveExplorer.solve(text, "UC").split(";");

            assertEquals(search.nodesExpanded(), Integer.parseInt(parts[3]), text);
            // that is much more than the number of steps in the plan
            assertTrue(Integer.parseInt(parts[3]) > parts[0].split(",").length, text);
        }
    }

    @Test
    public void theAnswerForThePdfExampleHasFourPartsInTheRightOrder() {
        String[] parts = CaveExplorer.solve(PDF_EXAMPLE, "UC").split(";");

        // plan; lives used; energy used; nodes expanded
        assertEquals(4, parts.length);
        assertEquals(1, Integer.parseInt(parts[1]));
        assertEquals(65, Integer.parseInt(parts[2]));
        assertTrue(Integer.parseInt(parts[3]) > 0);

        // the plan really wins the cave, which is what the PDF shows too
        CaveRules rules = new CaveRules(new CaveMap(PDF_EXAMPLE));
        State end = rules.replay(parts[0]);
        assertNotNull(end);
        assertTrue(rules.isGoal(end));
    }

    @Test
    public void theAnswerHasNoSpacesAndNoLineBreaks() {
        for (String text : new String[] {PDF_EXAMPLE, PUBLIC_CAVES[0], PUBLIC_CAVES[3]}) {
            String answer = CaveExplorer.solve(text, "UC");

            // lower case action names with commas, then three numbers
            assertTrue(answer.matches("[a-z]+(,[a-z]+)*;\\d+;\\d+;\\d+"), answer);
        }
    }

    @Test
    public void aCaveThatCannotBeWonGivesExactlyNoSolution() {
        // the cave of test_plan_uc6
        assertEquals("No Solution", CaveExplorer.solve(PUBLIC_CAVES[7], "UC"));
    }

    @Test
    public void theSameQuestionAlwaysGivesTheSameAnswer() {
        String first = CaveExplorer.solve(PUBLIC_CAVES[3], "UC");
        String second = CaveExplorer.solve(PUBLIC_CAVES[3], "UC");

        assertEquals(first, second);
    }

    // ---------------------------------------------------------------- the public caves

    @Test
    public void theNumbersInTheAnswerMatchThePlanInIt() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            for (int i = 0; i < PUBLIC_CAVES.length; i++) {
                String answer = CaveExplorer.solve(PUBLIC_CAVES[i], "UC");
                String which = "public cave number " + i;

                if (LIVES_USED[i] == -1) {
                    assertEquals("No Solution", answer, which);
                    continue;
                }
                String[] parts = answer.split(";");
                assertEquals(4, parts.length, which);

                // play the plan ourselves and see what it costs
                CaveRules rules = new CaveRules(new CaveMap(PUBLIC_CAVES[i]));
                State end = rules.replay(parts[0]);
                assertNotNull(end, which);
                assertTrue(rules.isGoal(end), which);
                assertEquals(rules.livesUsed(end), Integer.parseInt(parts[1]), which);
                assertEquals(rules.energyUsed(end), Integer.parseInt(parts[2]), which);

                // and it is the cheapest plan there is
                assertEquals(LIVES_USED[i], Integer.parseInt(parts[1]), which);
                assertEquals(ENERGY_USED[i], Integer.parseInt(parts[2]), which);
            }
        });
    }

    @Test
    public void theRealCheckerAcceptsEveryAnswer() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            for (int i = 0; i < PUBLIC_CAVES.length; i++) {
                if (LIVES_USED[i] == -1) {
                    continue;   // the checker does not judge "No Solution"
                }
                String answer = CaveExplorer.solve(PUBLIC_CAVES[i], "UC");
                Checker.ValidationResult result = Checker.validateSolution(PUBLIC_CAVES[i], answer);

                assertTrue(result.isValid, "public cave number " + i + ": the checker refuses " + answer
                        + " (" + result.errorMessage + ")");
            }
        });
    }

    @Test
    public void moreRopeMeansTheAnswerNeedsNoJump() {
        // the PDF example with 3 meters of rope instead of 2
        String[] parts = CaveExplorer.solve("3,4;0,1;100,3;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;", "UC").split(";");

        assertEquals("0", parts[1]);
        assertEquals("65", parts[2]);
        assertFalse(parts[0].contains("jumpdown"));
    }

    // ---------------------------------------------------------------- questions that make no sense

    @Test
    public void aStrategyThatDoesNotExistIsRefusedWithAClearMessage() {
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> CaveExplorer.solve(PDF_EXAMPLE, "XYZ"));

        assertTrue(error.getMessage().contains("XYZ"));
        assertTrue(error.getMessage().contains("UC"));
    }

    @Test
    public void strategyNamesAreWrittenInCapitals() {
        assertThrows(IllegalArgumentException.class, () -> CaveExplorer.solve(PDF_EXAMPLE, "uc"));
        assertThrows(IllegalArgumentException.class, () -> CaveExplorer.solve(PDF_EXAMPLE, ""));
        assertEquals(Strategy.UC, Strategy.fromText("UC"));
    }

    @Test
    public void aBrokenCaveIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> CaveExplorer.solve("hello", "UC"));
        // says 4 rows and 3 columns, but the cave is 3 rows and 4 columns
        assertThrows(IllegalArgumentException.class,
                () -> CaveExplorer.solve("4,3;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;", "UC"));
    }
}
