package tests;

import code.CaveMap;
import code.State;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Our own tests for the pieces we build before any searching happens.
 */
public class ModelTests {

    // the example from the assignment PDF (Figure 1)
    private static final String PDF_EXAMPLE = "3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;";

    // every cave the public tests use (copied from PublicTests)
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

    // ---------------------------------------------------------------- reading a cave

    @Test
    public void pdfExampleIsReadCorrectly() {
        CaveMap cave = new CaveMap(PDF_EXAMPLE);

        assertEquals(3, cave.rows);
        assertEquals(4, cave.cols);
        assertEquals(0, cave.startX);
        assertEquals(1, cave.startY);
        assertEquals(100, cave.energy);
        assertEquals(2, cave.rope);

        assertArrayEquals(new int[] {0, 3, 0, 0}, cave.grid[0]);
        assertArrayEquals(new int[] {4, 12, 0, 0}, cave.grid[1]);
        assertArrayEquals(new int[] {0, 7, 23, 8}, cave.grid[2]);

        assertEquals(1, cave.doors.length);
        assertArrayEquals(new int[] {3, 2}, cave.doors[0]);
        assertEquals(1, cave.keys.length);
        assertArrayEquals(new int[] {1, 0}, cave.keys[0]);
    }

    @Test
    public void firstNumberIsRowsAndSecondIsColumns() {
        // this cave is not square, so mixing them up would show immediately
        CaveMap cave = new CaveMap(PUBLIC_CAVES[0]);

        assertEquals(4, cave.rows);
        assertEquals(5, cave.cols);
        assertEquals(4, cave.grid.length);
        assertEquals(5, cave.grid[0].length);
    }

    @Test
    public void coordinatesAreXThenYWithZeroZeroTopLeft() {
        CaveMap cave = new CaveMap(PDF_EXAMPLE);

        // x is the column and y is the row, so we read grid[y][x].
        // The PDF figure shows 4 under the explorer, 8 on the door and 3 on the key.
        assertEquals(4, cave.grid[cave.startY][cave.startX]);
        assertEquals(8, cave.grid[cave.doors[0][1]][cave.doors[0][0]]);
        assertEquals(3, cave.grid[cave.keys[0][1]][cave.keys[0][0]]);
    }

    @Test
    public void severalDoorsAndKeysAreAllRead() {
        CaveMap cave = new CaveMap(PUBLIC_CAVES[6]);

        assertEquals(7, cave.rows);
        assertEquals(8, cave.cols);
        assertEquals(2, cave.doors.length);
        assertArrayEquals(new int[] {3, 2}, cave.doors[0]);
        assertArrayEquals(new int[] {5, 6}, cave.doors[1]);
        assertEquals(3, cave.keys.length);
        assertArrayEquals(new int[] {1, 0}, cave.keys[0]);
        assertArrayEquals(new int[] {5, 3}, cave.keys[1]);
        assertArrayEquals(new int[] {0, 4}, cave.keys[2]);
    }

    @Test
    public void lastSemicolonAndSpacesAroundTheTextDoNotMatter() {
        String withoutLastSemicolon = PDF_EXAMPLE.substring(0, PDF_EXAMPLE.length() - 1);

        assertEquals(new CaveMap(PDF_EXAMPLE).toString(), new CaveMap(withoutLastSemicolon).toString());
        assertEquals(new CaveMap(PDF_EXAMPLE).toString(), new CaveMap("  " + PDF_EXAMPLE + "\n").toString());
    }

    @Test
    public void everyPublicCaveLooksLikeTheAssignmentSaysItShould() {
        for (String text : PUBLIC_CAVES) {
            CaveMap cave = new CaveMap(text);
            String where = "cave " + text;

            // sizes and starting values from the PDF
            assertTrue(cave.rows >= 3 && cave.rows <= 10, where);
            assertTrue(cave.cols >= 3 && cave.cols <= 10, where);
            assertTrue(cave.energy >= 0 && cave.energy <= 500, where);
            assertTrue(cave.rope >= 0 && cave.rope <= 25, where);
            assertTrue(cave.doors.length >= 1 && cave.doors.length <= 5, where);
            assertTrue(cave.keys.length >= cave.doors.length && cave.keys.length <= 8, where);

            for (int[] row : cave.grid) {
                assertEquals(cave.cols, row.length, where);
                for (int difficulty : row) {
                    assertTrue(difficulty >= 0 && difficulty <= 45, where);
                }
            }

            // the explorer, the doors and the keys all stand in real caves, never in a wall.
            // If x and y were swapped somewhere, this is where it would show up.
            assertNotEquals(0, cave.grid[cave.startY][cave.startX], where);
            for (int[] door : cave.doors) {
                assertNotEquals(0, cave.grid[door[1]][door[0]], where);
            }
            for (int[] key : cave.keys) {
                assertNotEquals(0, cave.grid[key[1]][key[0]], where);
            }

            // a cave holds at most one thing, so no door sits where a key sits
            for (int[] door : cave.doors) {
                for (int[] key : cave.keys) {
                    assertFalse(door[0] == key[0] && door[1] == key[1], where);
                }
            }
        }
    }

    // ---------------------------------------------------------------- bad input

    @Test
    public void tooFewRowsAreRejected() {
        // says 3 rows but only two rows are there
        assertThrows(IllegalArgumentException.class,
                () -> new CaveMap("3,4;0,1;100,2;0,3,0,0;4,12,0,0;3,2;1,0;"));
    }

    @Test
    public void aRowOfTheWrongWidthIsRejected() {
        // the middle row only has 3 cells instead of 4
        assertThrows(IllegalArgumentException.class,
                () -> new CaveMap("3,4;0,1;100,2;0,3,0,0;4,12,0;0,7,23,8;3,2;1,0;"));
    }

    @Test
    public void swappedRowsAndColumnsAreRejected() {
        // "4,3" claims 4 rows, but the three rows below are four cells wide
        assertThrows(IllegalArgumentException.class,
                () -> new CaveMap("4,3;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;"));
    }

    @Test
    public void aMissingKeysGroupIsRejected() {
        // everything up to the doors is there, but the keys are missing
        assertThrows(IllegalArgumentException.class,
                () -> new CaveMap("3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;"));
    }

    @Test
    public void anExtraGroupAtTheEndIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CaveMap("3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;7,7;"));
    }

    @Test
    public void aDoorOutsideTheGridIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CaveMap("3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;9,9;1,0;"));
    }

    @Test
    public void anOddNumberOfCoordinatesIsRejected() {
        // a door needs an x and a y
        assertThrows(IllegalArgumentException.class,
                () -> new CaveMap("3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2,1;1,0;"));
    }

    @Test
    public void textThatIsNotNumbersIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CaveMap("hello"));
        assertThrows(IllegalArgumentException.class, () -> new CaveMap(""));
    }

    // ---------------------------------------------------------------- printing

    @Test
    public void toStringShowsTheWholeCave() {
        String shown = new CaveMap(PDF_EXAMPLE).toString();

        assertTrue(shown.contains("3 rows x 4 columns"));
        assertTrue(shown.contains("start (0,1)  energy 100  rope 2"));
        assertTrue(shown.contains("doors: (3,2)"));
        assertTrue(shown.contains("keys: (1,0)"));
    }

    // ---------------------------------------------------------------- states

    // a normal state to compare against: standing at (1,1) with a bit of everything left
    private static State sampleState() {
        return new State(1, 1, 88, 2, 3, false, 1, 1);
    }

    @Test
    public void startStateOfThePdfExample() {
        State start = State.initial(new CaveMap(PDF_EXAMPLE));

        assertEquals(0, start.x);
        assertEquals(1, start.y);
        assertEquals(100, start.energy);
        assertEquals(2, start.rope);
        assertEquals(3, start.lives);
        assertFalse(start.holdingKey);
        assertEquals(1, start.keysOnFloor);   // one key, still on the floor
        assertEquals(1, start.doorsLocked);   // one door, still locked
    }

    @Test
    public void startStateHasOneBitPerKeyAndDoor() {
        // this cave has 3 keys and 2 doors
        State start = State.initial(new CaveMap(PUBLIC_CAVES[6]));

        assertEquals(7, start.keysOnFloor);   // binary 111
        assertEquals(3, start.doorsLocked);   // binary 11
    }

    @Test
    public void startStateIsRightForEveryPublicCave() {
        for (String text : PUBLIC_CAVES) {
            CaveMap cave = new CaveMap(text);
            State start = State.initial(cave);

            assertEquals(cave.startX, start.x, text);
            assertEquals(cave.startY, start.y, text);
            assertEquals(cave.energy, start.energy, text);
            assertEquals(cave.rope, start.rope, text);
            assertEquals(3, start.lives, text);
            assertFalse(start.holdingKey, text);
            // one bit for every key and every door
            assertEquals(cave.keys.length, Integer.bitCount(start.keysOnFloor), text);
            assertEquals(cave.doors.length, Integer.bitCount(start.doorsLocked), text);
        }
    }

    @Test
    public void bitsTellWhichKeysAndDoorsAreLeft() {
        // keys 0 and 2 are still on the floor and key 1 is gone. Only door 1 is still locked.
        State s = new State(0, 0, 10, 0, 3, false, 5, 2);

        assertTrue(s.keyOnFloor(0));
        assertFalse(s.keyOnFloor(1));
        assertTrue(s.keyOnFloor(2));
        assertFalse(s.doorLocked(0));
        assertTrue(s.doorLocked(1));
    }

    @Test
    public void sameNumbersMakeEqualStates() {
        State a = sampleState();
        State b = sampleState();

        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, null);
        assertNotEquals(a, "not a state");
    }

    @Test
    public void changingAnySingleNumberMakesADifferentState() {
        State base = sampleState();

        assertNotEquals(base, new State(2, 1, 88, 2, 3, false, 1, 1));   // x
        assertNotEquals(base, new State(1, 2, 88, 2, 3, false, 1, 1));   // y
        assertNotEquals(base, new State(1, 1, 87, 2, 3, false, 1, 1));   // energy
        assertNotEquals(base, new State(1, 1, 88, 1, 3, false, 1, 1));   // rope
        assertNotEquals(base, new State(1, 1, 88, 2, 2, false, 1, 1));   // lives
        assertNotEquals(base, new State(1, 1, 88, 2, 3, true, 1, 1));    // key in hand
        assertNotEquals(base, new State(1, 1, 88, 2, 3, false, 0, 1));   // keys on the floor
        assertNotEquals(base, new State(1, 1, 88, 2, 3, false, 1, 0));   // doors locked
    }

    @Test
    public void sameCaveWithDifferentRopeAndKeyIsADifferentSituation() {
        // both are the explorer standing in cave (1,1) of the PDF example,
        // once after step 1 (right) and once after step 4 (climbdown)
        State afterStep1 = new State(1, 1, 88, 2, 3, false, 1, 1);
        State afterStep4 = new State(1, 1, 73, 0, 3, true, 0, 1);

        assertNotEquals(afterStep1, afterStep4);
    }

    @Test
    public void aHashSetKeepsOnlyOneCopyOfEqualStates() {
        Set<State> seen = new HashSet<>();
        seen.add(sampleState());
        seen.add(sampleState());                           // same numbers again
        seen.add(new State(1, 1, 88, 1, 3, false, 1, 1));  // one meter less rope

        assertEquals(2, seen.size());
        assertTrue(seen.contains(new State(1, 1, 88, 2, 3, false, 1, 1)));
        assertFalse(seen.contains(new State(1, 1, 88, 0, 3, false, 1, 1)));
    }

    @Test
    public void hashCodesAreSpreadOutWell() {
        // The search keeps a huge number of states in a HashSet, so different states should
        // almost never share a hash code. Try a big block of different states and count.
        Set<Integer> hashes = new HashSet<>();
        int total = 0;
        for (int energy = 0; energy <= 40; energy++) {
            for (int rope = 0; rope <= 5; rope++) {
                for (int lives = 1; lives <= 3; lives++) {
                    for (int cell = 0; cell < 49; cell++) {            // a 7x7 grid of positions
                        for (int extra = 0; extra < 16; extra++) {     // key in hand, 2 doors, 4 key masks
                            boolean holdingKey = (extra & 1) == 1;
                            int doors = (extra >> 1) & 1;
                            int keys = extra >> 2;
                            State s = new State(cell % 7, cell / 7, energy, rope, lives, holdingKey, keys, doors);
                            hashes.add(s.hashCode());
                            total++;
                        }
                    }
                }
            }
        }

        assertTrue(hashes.size() >= total * 0.99,
                "only " + hashes.size() + " different hash codes for " + total + " different states");
    }

    @Test
    public void toStringListsEveryNumber() {
        assertEquals("State(x=1, y=1, energy=88, rope=2, lives=3, holdingKey=false, keysOnFloor=1, doorsLocked=1)",
                sampleState().toString());
    }
}
