package dev.cobbledeep.exploration;

/** Bounded sampling of a two-block cell, independent of Minecraft APIs. */
public final class CellSight
{
    @FunctionalInterface
    public interface Sample { boolean visible(double x, double y, double z); }

    private CellSight() { }

    public static boolean visible(double eyeX, double eyeY, double eyeZ,
                                  int x, int y, int z, double range, Sample sample)
    {
        // Test the centre first, then inset corners. A centre can be buried
        // below a terrace while its upper or side surface is still visible.
        for (int i = -1; i < 8; i++)
        {
            double px = x + (i < 0 ? 1.0 : (i & 1) == 0 ? 0.05 : 1.95);
            double py = y + (i < 0 ? 1.0 : (i & 2) == 0 ? 0.05 : 1.95);
            double pz = z + (i < 0 ? 1.0 : (i & 4) == 0 ? 0.05 : 1.95);
            double dx = px - eyeX, dy = py - eyeY, dz = pz - eyeZ;
            if (dx * dx + dy * dy + dz * dz <= range * range
                    && sample.visible(px, py, pz)) return true;
        }
        return false;
    }
}
