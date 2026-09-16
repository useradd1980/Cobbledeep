package dev.cobbledeep.pathfinding;

/** Detects a genuinely stationary body without depending on waypoint geometry. */
public final class MovementProgress
{
    private static final double MINIMUM_MOVEMENT_SQUARED = 0.03 * 0.03;
    private boolean initialized;
    private double x, y, z;
    private int stationaryTicks;

    public int update(double px, double py, double pz)
    {
        double dx = px - x, dy = py - y, dz = pz - z;
        if (!initialized || dx*dx + dy*dy + dz*dz >= MINIMUM_MOVEMENT_SQUARED)
        {
            initialized = true;
            x = px; y = py; z = pz;
            stationaryTicks = 0;
        }
        else stationaryTicks++;
        return stationaryTicks;
    }

    public void reset()
    {
        initialized = false;
        stationaryTicks = 0;
    }
}
