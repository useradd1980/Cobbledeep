package dev.cobbledeep.pathfinding;

/** Limits repeated failures in one place, not recoveries over an entire trip. */
public final class RouteRecovery
{
    private int attempts;
    private double x, y, z;
    public void clear() { attempts = 0; }
    public boolean retry(double px, double py, double pz)
    {
        if (attempts > 0 && movedFromAnchor(px, py, pz)) attempts = 0;
        if (attempts >= 2) return false;
        if (attempts == 0) { x = px; y = py; z = pz; }
        attempts++;
        return true;
    }
    public void reachedWaypoint(double px, double py, double pz)
    {
        if (attempts > 0 && movedFromAnchor(px, py, pz)) attempts = 0;
    }
    private boolean movedFromAnchor(double px, double py, double pz)
    {
        double dx = px-x, dy = py-y, dz = pz-z;
        return dx*dx + dy*dy + dz*dz >= 0.75*0.75;
    }
}
