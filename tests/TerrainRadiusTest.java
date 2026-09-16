import dev.cobbledeep.exploration.ExplorationGrid;
import dev.cobbledeep.exploration.TerrainRadius;

public final class TerrainRadiusTest
{
    private static void check(boolean ok, String message)
    {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args)
    {
        ExplorationGrid grid = new ExplorationGrid();
        TerrainRadius.visit(-10.3, 7.8, (x, z) -> {
            for (int section = -4; section < 20; section++) grid.discoverSectionColumn(x, section, z);
        });
        // Independent dense point oracle: every point in the circle must have
        // memory at the bottom, middle and top of the world, including slopes.
        for (double x = -35; x <= 15; x += .25)
            for (double z = -17; z <= 33; z += .25)
                if ((x + 10.3) * (x + 10.3) + (z - 7.8) * (z - 7.8) <= 24 * 24)
                    for (int y : new int[] {-64, 65, 319})
                        check(grid.isExplored((int)Math.floor(x), y, (int)Math.floor(z)), "No holes in disk at any elevation");
        check(!grid.isExplored(30, 65, 8), "Distant terrain remains unknown");
        check(!grid.isExplored(-10, -66, 8), "No writes below world bounds");
        check(!grid.isExplored(-10, 320, 8), "No writes above world bounds");
        long before = grid.discoveredCount();
        TerrainRadius.visit(-10.3, 7.8, (x, z) -> {
            for (int section = -4; section < 20; section++)
                check(!grid.discoverSectionColumn(x, section, z), "Repeated scan is clean");
        });
        check(before == grid.discoveredCount(), "Repeated discovery is idempotent");
        ExplorationGrid restored = new ExplorationGrid();
        for (var section : grid.sections()) restored.restoreSection(section, grid.sectionBits(section));
        check(restored.discoveredCount() == before, "Column discoveries use existing save payloads");
        check(restored.isExplored(-10, 65, 8), "Exploration survives record restoration");
        ExplorationGrid partial = new ExplorationGrid();
        partial.discover(-1, -1, -1);
        check(partial.discoverSectionColumn(-1, -1, -1), "Column extends existing individual cell");
        check(partial.discoveredCount() == 8, "Mixed discovery counts exactly eight cells");
        System.out.println("Terrain radius checks passed: full-height coverage, range, bounds, idempotence and persistence.");
    }
}
