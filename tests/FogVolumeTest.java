import dev.cobbledeep.exploration.ExplorationGrid;
import dev.cobbledeep.exploration.FogVolume;
import java.util.BitSet;

public final class FogVolumeTest
{
    private static void check(boolean ok, String message)
    {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args)
    {
        byte[] pixels = new byte[FogVolume.LENGTH];
        ExplorationGrid grid = new ExplorationGrid();
        grid.discover(0, 64, 0);
        grid.discover(-2, 64, -2);
        grid.discover(0, 40, 0);
        FogVolume.fill(pixels, grid, -64, 0, -64, (x, y, z) -> x == 0 && y == 64 && z == 0);
        check(pixels[FogVolume.index(64, 32, 64)] == 2, "Visible surface is clear");
        check(pixels[FogVolume.index(63, 32, 63)] == 1, "Known but occluded cell is dim");
        check(pixels[FogVolume.index(64, 20, 64)] == 1, "Cave has its own visibility state");
        check(pixels[FogVolume.index(64, 21, 64)] == 0, "Unexplored vertical neighbour stays hidden");
        FogVolume.fill(pixels, grid, -64, 0, -64, (x, y, z) -> false);
        check(pixels[FogVolume.index(64, 32, 64)] == 1, "Loss of sight dims without erasing memory");
        long count = grid.discoveredCount();
        FogVolume.fill(pixels, grid, 10000, 10000, 10000, (x, y, z) -> { throw new AssertionError("Off-window sight query"); });
        for (byte pixel : pixels) check(pixel == 0, "Panning into unknown space must be opaque");
        check(grid.discoveredCount() == count, "Camera movement never writes discovery");
        // Current LOS may brighten a cell before its server snapshot arrives.
        FogVolume.fill(pixels, null, -64, 0, -64, (x, y, z) -> false);
        FogVolume.applySight(pixels, -64, 0, -64, 0, 65, 0, 24,
                (x, y, z) -> x == 0 && y == 64 && z == 0);
        check(pixels[FogVolume.index(64, 32, 64)] == 2, "Sight does not wait for persistent memory");
        check(pixels[FogVolume.index(65, 32, 64)] == 0, "Sight does not fabricate neighbouring memory");
        FogVolume.fill(pixels, grid, -64, 0, -64, (x, y, z) -> false);
        FogVolume.applySight(pixels, -64, 0, -64, 0, 65, 0, 24, (x, y, z) -> false);
        check(pixels[FogVolume.index(64, 32, 64)] == 1, "Blocked known room returns to dim state");
        check(grid.discoveredCount() == count, "Current sight never changes saved exploration");
        FogVolume.fill(pixels, null, -64, 0, -64, (x, y, z) -> true);
        for (byte pixel : pixels) check(pixel == 0, "Missing/reset snapshot fails closed");

        BitSet addresses = new BitSet(FogVolume.LENGTH);
        for (int y = 0; y < FogVolume.SIZE; y++)
            for (int z = 0; z < FogVolume.SIZE; z++)
                for (int x = 0; x < FogVolume.SIZE; x++)
                {
                    int address = FogVolume.index(x, y, z);
                    check(address >= 0 && address < FogVolume.LENGTH && !addresses.get(address), "Unique voxel address");
                    check(address == (address / FogVolume.WIDTH) * FogVolume.WIDTH + address % FogVolume.WIDTH,
                            "Shader texel addressing matches CPU layout");
                    addresses.set(address);
                }
        check(addresses.cardinality() == FogVolume.LENGTH, "Complete atlas coverage");
        System.out.println("Fog volume checks passed (all " + FogVolume.LENGTH + " voxel addresses).");
    }
}
