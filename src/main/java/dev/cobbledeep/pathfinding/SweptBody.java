package dev.cobbledeep.pathfinding;

/** Continuous moving-box collision, excluding mere face/edge contact. */
public final class SweptBody
{
    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) { }
    private SweptBody() { }

    public static boolean hits(Box body, double dx, double dy, double dz, Box obstacle)
    {
        double[] origin = {body.minX(), body.minY(), body.minZ()};
        double[] velocity = {dx, dy, dz};
        double[] low = {obstacle.minX() - (body.maxX()-body.minX()),
                obstacle.minY() - (body.maxY()-body.minY()), obstacle.minZ() - (body.maxZ()-body.minZ())};
        double[] high = {obstacle.maxX(), obstacle.maxY(), obstacle.maxZ()};
        double enter = 0, exit = 1;
        for (int axis = 0; axis < 3; axis++)
        {
            low[axis] += 1e-7; high[axis] -= 1e-7;
            if (Math.abs(velocity[axis]) < 1e-12)
            {
                if (origin[axis] <= low[axis] || origin[axis] >= high[axis]) return false;
                continue;
            }
            double a = (low[axis] - origin[axis]) / velocity[axis];
            double b = (high[axis] - origin[axis]) / velocity[axis];
            enter = Math.max(enter, Math.min(a,b));
            exit = Math.min(exit, Math.max(a,b));
            if (enter >= exit) return false;
        }
        return enter < exit;
    }
}
