import dev.cobbledeep.exploration.ExplorationGrid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Dependency-free regression tests for the actual discovery/storage index. */
public final class ExplorationGridTest
{
    private static int checks;

    private static void check(boolean value, String message)
    {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception
    {
        ExplorationGrid grid = new ExplorationGrid();
        check(grid.discover(0, 64, 0), "First observation changes data");
        check(!grid.discover(1, 65, 1), "Same cell is not counted twice");
        check(!grid.isExplored(0, 40, 0), "Surface must not reveal cave below");
        check(!grid.isExplored(-1, 64, 0), "Negative cell is distinct");
        check(grid.discover(-1, -1, -1), "Negative coordinates supported");
        check(grid.isExplored(-2, -2, -2), "Floor division for negative coordinates");
        check(!grid.isExplored(-3, -2, -2), "Adjacent negative cell stays unknown");

        ExplorationGrid dense = new ExplorationGrid();
        for (int x = 0; x < 16; x += 2)
            for (int y = 0; y < 16; y += 2)
                for (int z = 0; z < 16; z += 2)
                    check(dense.discover(x, y, z), "Section cell index must be unique");
        check(dense.discoveredCount() == 512 && dense.sections().size() == 1,
                "512 distinct cells in one section");
        check(dense.sectionBits(new ExplorationGrid.Section(0, 0, 0)).length == 8,
                "Full section occupies eight longs (64 bytes)");
        check(dense.discover(16, 0, 0) && dense.sections().size() == 2,
                "Crossing a chunk boundary creates a separate section");

        // An independent cell-coordinate set is the oracle, including boundaries
        // and widely separated positive/negative sections.
        Random random = new Random(731);
        ExplorationGrid varied = new ExplorationGrid();
        Set<String> expected = new HashSet<>();
        int[][] observations = new int[10000][3];
        for (int[] position : observations)
        {
            for (int axis = 0; axis < 3; axis++) position[axis] = random.nextInt(513) - 256;
            String cell = Math.floorDiv(position[0], 2) + ":" + Math.floorDiv(position[1], 2)
                    + ":" + Math.floorDiv(position[2], 2);
            check(varied.discover(position[0], position[1], position[2]) == expected.add(cell),
                    "Discovery must match independent cell-coordinate oracle");
        }
        check(varied.discoveredCount() == expected.size(), "Accurate total");

        // Round-trip the exact section coordinates and long[] payloads written
        // by ExplorationData's NBT adapter; this is not an NBT/Forge runtime test.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes))
        {
            output.writeInt(varied.sections().size());
            for (ExplorationGrid.Section section : varied.sections())
            {
                output.writeInt(section.x()); output.writeInt(section.y()); output.writeInt(section.z());
                long[] words = varied.sectionBits(section);
                output.writeInt(words.length);
                for (long word : words) output.writeLong(word);
            }
        }
        ExplorationGrid restored = new ExplorationGrid();
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())))
        {
            int count = input.readInt();
            for (int i = 0; i < count; i++)
            {
                ExplorationGrid.Section section = new ExplorationGrid.Section(
                        input.readInt(), input.readInt(), input.readInt());
                long[] words = new long[input.readInt()];
                for (int j = 0; j < words.length; j++) words[j] = input.readLong();
                restored.restoreSection(section, words);
                restored.restoreSection(section, words);
            }
        }
        check(restored.discoveredCount() == varied.discoveredCount(), "Reload/duplicate sections preserve count");
        for (int[] p : observations) check(restored.isExplored(p[0], p[1], p[2]), "Reload preserves discovery");
        ExplorationGrid.Section first = restored.sections().iterator().next();
        long[] detachedCopy = restored.sectionBits(first);
        long count = restored.discoveredCount();
        java.util.Arrays.fill(detachedCopy, 0L);
        check(restored.discoveredCount() == count && restored.sectionBits(first).length > 0,
                "Export must not expose mutable storage");

        ExplorationGrid otherPlayerOrDimension = new ExplorationGrid();
        check(!otherPlayerOrDimension.isExplored(0, 64, 0), "Separate records do not share discovery");
        boolean rejected = false;
        try { restored.restoreSection(first, new long[9]); }
        catch (IllegalArgumentException expectedError) { rejected = true; }
        check(rejected, "Reject oversized persisted sections");
        System.out.println("Passed " + checks + " exploration grid checks.");
    }
}
