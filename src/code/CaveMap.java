package code;

/**
 * The cave exactly as the input string describes it: the grid, where the explorer
 * starts, how much energy and rope it has, and where the doors and keys are.
 * Nothing in here changes once it has been built.
 */
public class CaveMap {

    public final int rows;
    public final int cols;
    public final int startX;
    public final int startY;
    public final int energy;
    public final int rope;

    // grid[y][x] is the difficulty of that cave, 0 means it is a wall
    public final int[][] grid;

    // doors[i] = {x, y}, and the same for keys
    public final int[][] doors;
    public final int[][] keys;

    public CaveMap(String text) {
        // the string looks like  H,W;X,Y;EN,RL;row;row;...;doors;keys;
        String[] parts = text.trim().split(";");

        // careful: the first number is the number of rows (the PDF calls it H)
        int[] size = pair(parts[0], "the grid size");
        rows = size[0];
        cols = size[1];

        // 3 groups before the rows, then the doors and the keys after them
        if (parts.length != rows + 5) {
            throw new IllegalArgumentException("expected " + (rows + 5) + " groups separated by ';' but found "
                    + parts.length);
        }

        int[] start = pair(parts[1], "the start position");
        startX = start[0];
        startY = start[1];

        int[] resources = pair(parts[2], "the energy and rope");
        energy = resources[0];
        rope = resources[1];

        grid = new int[rows][];
        for (int y = 0; y < rows; y++) {
            grid[y] = numbers(parts[3 + y]);
            if (grid[y].length != cols) {
                throw new IllegalArgumentException("row " + y + " has " + grid[y].length
                        + " cells but the cave has " + cols + " columns");
            }
        }

        doors = pairs(parts[3 + rows], "doors");
        keys = pairs(parts[4 + rows], "keys");

        checkInside(startX, startY, "the explorer");
        for (int[] door : doors) {
            checkInside(door[0], door[1], "a door");
        }
        for (int[] key : keys) {
            checkInside(key[0], key[1], "a key");
        }
    }

    // turns "12,3,0" into {12, 3, 0}
    private static int[] numbers(String part) {
        if (part.isEmpty()) {
            return new int[0];
        }
        String[] pieces = part.split(",");
        int[] result = new int[pieces.length];
        for (int i = 0; i < pieces.length; i++) {
            result[i] = Integer.parseInt(pieces[i].trim());
        }
        return result;
    }

    // for groups that must be exactly two numbers
    private static int[] pair(String part, String what) {
        int[] n = numbers(part);
        if (n.length != 2) {
            throw new IllegalArgumentException(what + " should be two numbers but was \"" + part + "\"");
        }
        return n;
    }

    // turns "3,2,5,6" into {{3, 2}, {5, 6}}
    private static int[][] pairs(String part, String what) {
        int[] n = numbers(part);
        if (n.length % 2 != 0) {
            throw new IllegalArgumentException("the " + what + " need an x and a y each, but \"" + part
                    + "\" has an odd number of values");
        }
        int[][] result = new int[n.length / 2][];
        for (int i = 0; i < result.length; i++) {
            result[i] = new int[] { n[2 * i], n[2 * i + 1] };
        }
        return result;
    }

    private void checkInside(int x, int y, String what) {
        if (x < 0 || x >= cols || y < 0 || y >= rows) {
            throw new IllegalArgumentException(what + " is outside the cave at (" + x + "," + y + ")");
        }
    }

    // handy for printing a cave while we are debugging
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(rows).append(" rows x ").append(cols).append(" columns\n");
        sb.append("start (").append(startX).append(",").append(startY).append(")  energy ")
                .append(energy).append("  rope ").append(rope).append("\n");
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                sb.append(String.format("%4d", grid[y][x]));
            }
            sb.append("\n");
        }
        sb.append("doors:");
        for (int[] door : doors) {
            sb.append(" (").append(door[0]).append(",").append(door[1]).append(")");
        }
        sb.append("\nkeys:");
        for (int[] key : keys) {
            sb.append(" (").append(key[0]).append(",").append(key[1]).append(")");
        }
        return sb.toString();
    }
}
