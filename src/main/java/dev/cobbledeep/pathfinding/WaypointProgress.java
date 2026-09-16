package dev.cobbledeep.pathfinding;

/** Segment-aware arrival: a jump may pass a waypoint before reaching its height. */
public final class WaypointProgress
{
    public enum State { APPROACH, WAIT_FOR_HEIGHT, REACHED }

    private WaypointProgress() { }

    public static State classify(double fromX, double fromZ, double targetX, double targetY,
                                 double targetZ, double x, double y, double z, boolean last)
    {
        double dx = targetX - fromX, dz = targetZ - fromZ;
        double length = Math.hypot(dx, dz);
        boolean horizontal = Math.hypot(targetX - x, targetZ - z) <= 0.22;
        if (!horizontal && length > 0.001)
        {
            double along = ((x - fromX) * dx + (z - fromZ) * dz) / length;
            double across = Math.abs((x - fromX) * dz - (z - fromZ) * dx) / length;
            horizontal = along >= length && along <= length + (last ? 0.35 : 0.75) && across <= 0.22;
        }
        if (!horizontal) return State.APPROACH;
        return Math.abs(targetY - y) <= 0.35 ? State.REACHED : State.WAIT_FOR_HEIGHT;
    }

    public static boolean straight(double fromX, double fromZ, double x, double z, double nextX, double nextZ)
    {
        double ax = x - fromX, az = z - fromZ, bx = nextX - x, bz = nextZ - z;
        return ax * bx + az * bz > 0 && Math.abs(ax * bz - az * bx) < 0.0001;
    }

    public static boolean waitingForClimb(double targetY, double y)
    {
        return targetY - y > 0.35;
    }

    public static boolean completedClimb(State state, double rise)
    {
        return state != State.APPROACH && rise > 0.35;
    }
}
