package dev.cobbledeep.pathfinding;

/** Limits repeated failures in one place, not recoveries over an entire trip. */
public final class RouteRecovery
{
    private int attempts;
    private double x, y, z;
    public void clear() { attempts = 0; }
    public boolean retry(double px, double py, double pz)
    {
        if (attempts >= 2) return false;
        attempts++; x = px; y = py; z = pz;
        return true;
    }
    public void reachedWaypoint(double px, double py, double pz)
    {
        double dx = px-x, dy = py-y, dz = pz-z;
        if (dx*dx + dy*dy + dz*dz >= 0.75*0.75) attempts = 0;
    }
}
