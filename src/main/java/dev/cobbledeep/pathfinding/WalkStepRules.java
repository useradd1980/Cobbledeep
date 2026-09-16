package dev.cobbledeep.pathfinding;

import dev.cobbledeep.pathfinding.GridPathfinder.Node;
import java.util.List;

/** Shared movement bounds, independent of Minecraft collision APIs. */
public final class WalkStepRules
{
    public static final double MAX_EDGE_DISTANCE = 1.65;

    private WalkStepRules() { }

    public static boolean supportedDiagonal(Node from, Node to, List<Node> walkableSides)
    {
        if (Math.abs(to.x() - from.x()) != 1 || Math.abs(to.z() - from.z()) != 1
                || to.y16() < from.y16() || to.y16() - from.y16() > 16) return false;
        boolean sideX = false, sideZ = false;
        for (Node side : walkableSides)
        {
            // Both side columns must have safe ground within this step's
            // elevation band. A lower pit or higher wall cannot qualify.
            if (side.y16() < from.y16() || side.y16() > to.y16()) continue;
            if (side.x() == to.x() && side.z() == from.z()) sideX = true;
            if (side.x() == from.x() && side.z() == to.z()) sideZ = true;
        }
        return sideX && sideZ;
    }

    public static boolean shouldJump(double rise, double distance, boolean grounded)
    {
        // Diagonal centres are sqrt(2) blocks apart. Start their jump as soon
        // as the validated edge is entered, just as on a cardinal one-block step.
        return grounded && rise > 0.6 && rise <= 1.05 && distance <= MAX_EDGE_DISTANCE;
    }
}
