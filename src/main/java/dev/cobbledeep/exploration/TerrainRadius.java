package dev.cobbledeep.exploration;

/** Terrain discovery is a horizontal disk; creature sight is independent. */
public final class TerrainRadius
{
    public static final double RANGE = 24.0;
    @FunctionalInterface
    public interface Column { void discover(int x, int z); }

    private TerrainRadius() { }

    public static void visit(double playerX, double playerZ, Column column)
    {
        int size = ExplorationGrid.CELL_SIZE;
        int minX = (int) Math.floor((playerX - RANGE) / size);
        int maxX = (int) Math.floor((playerX + RANGE) / size);
        int minZ = (int) Math.floor((playerZ - RANGE) / size);
        int maxZ = (int) Math.floor((playerZ + RANGE) / size);
        for (int cx = minX; cx <= maxX; cx++)
            for (int cz = minZ; cz <= maxZ; cz++)
            {
                int x = cx * size, z = cz * size;
                double dx = Math.max(x - playerX, Math.max(0, playerX - (x + size)));
                double dz = Math.max(z - playerZ, Math.max(0, playerZ - (z + size)));
                if (dx * dx + dz * dz <= RANGE * RANGE) column.discover(x, z);
            }
    }
}
