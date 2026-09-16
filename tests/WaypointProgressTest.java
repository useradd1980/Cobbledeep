import dev.cobbledeep.pathfinding.WaypointProgress;
import static dev.cobbledeep.pathfinding.WaypointProgress.State.*;

public final class WaypointProgressTest
{
    private static int checks;
    private static void check(boolean condition, String message)
    { checks++; if (!condition) throw new AssertionError(message); }

    public static void main(String[] args)
    {
        // Jump/drop trajectories cross the waypoint plane before settling at
        // its height. They must never become APPROACH again solely from that.
        for (int direction : new int[] {-1, 1})
            for (int axis = 0; axis < 2; axis++)
                for (double base : new double[] {-100.5, 0.5, 100.5})
                {
                    double tx = base + (axis == 0 ? direction : 0);
                    double tz = base + (axis == 1 ? direction : 0);
                    for (double[] pose : new double[][] {{1.05,1.8},{1.15,1.5},{1.30,1.2}})
                    {
                        double x = base + (axis == 0 ? direction * pose[0] : 0);
                        double z = base + (axis == 1 ? direction * pose[0] : 0);
                        var state = WaypointProgress.classify(base,base,tx,1,tz,x,pose[1],z,false);
                        check(state == (pose[1] > 1.35 ? WAIT_FOR_HEIGHT : REACHED), "Jump overshoot never commands reversal");
                    }
                    for (double[] pose : new double[][] {{1.05,.9},{1.15,.6},{1.30,.2}})
                    {
                        double x = base + (axis == 0 ? direction * pose[0] : 0);
                        double z = base + (axis == 1 ? direction * pose[0] : 0);
                        var state = WaypointProgress.classify(base,base,tx,0,tz,x,pose[1],z,false);
                        check(state == (pose[1] > .35 ? WAIT_FOR_HEIGHT : REACHED), "Drop overshoot waits or advances");
                    }
                }
        check(WaypointProgress.classify(0,0,1,0,0,.5,0,0,false) == APPROACH, "Do not skip a waypoint before reaching it");
        check(WaypointProgress.classify(0,0,1,0,0,1.2,0,.5,false) == APPROACH, "Lateral displacement is not forward progress");
        check(WaypointProgress.classify(0,0,1,0,0,3,0,0,false) == APPROACH, "Large displacement cannot consume route");
        check(WaypointProgress.classify(0,0,1,0,0,1.5,0,0,true) == APPROACH, "Final destination has tighter overshoot tolerance");
        check(WaypointProgress.classify(0,0,1,0,1,1.15,0,1.15,false) == REACHED, "Diagonal segment progress");
        check(WaypointProgress.classify(1,1,1,0,1,1,0,1,false) == REACHED, "Zero-length first segment is safe");
        check(WaypointProgress.straight(0,0,1,0,2,0), "Straight stairs permit checked lookahead");
        check(!WaypointProgress.straight(0,0,1,0,1,1), "A corner must not be cut while airborne");
        check(!WaypointProgress.straight(0,0,1,0,0,0), "A switchback must not be skipped");
        check(WaypointProgress.waitingForClimb(65,64), "Full block ascent continues climbing");
        check(WaypointProgress.waitingForClimb(64.5,64), "Half block ascent continues forward");
        check(!WaypointProgress.waitingForClimb(64.35,64), "Arrival tolerance completes a climb");
        check(!WaypointProgress.waitingForClimb(63,64), "A descent never uses uphill drive");
        check(WaypointProgress.completedClimb(REACHED,1), "normal uphill arrival starts landing transition");
        check(WaypointProgress.completedClimb(WAIT_FOR_HEIGHT,1), "airborne uphill lookahead starts landing transition");
        check(!WaypointProgress.completedClimb(APPROACH,1), "an unarrived uphill node is not complete");
        check(!WaypointProgress.completedClimb(REACHED,-1), "a completed descent is not an uphill landing");
        check(WaypointProgress.canLookAhead(true,false,true), "grounded safe corner can advance");
        check(WaypointProgress.canLookAhead(false,true,true), "airborne straight run can advance");
        check(!WaypointProgress.canLookAhead(false,false,true), "airborne corner cannot be cut");
        check(!WaypointProgress.canLookAhead(true,true,false), "collision blocks grounded lookahead");
        check(WaypointProgress.retryLandingEdgeFromPath(true,6,false), "settled climb retries rejected live edge");
        check(!WaypointProgress.retryLandingEdgeFromPath(false,6,false), "airborne edge does not use landing fallback");
        check(!WaypointProgress.retryLandingEdgeFromPath(true,0,false), "expired transition does not use fallback");
        check(!WaypointProgress.retryLandingEdgeFromPath(true,6,true), "clear live edge needs no fallback");
        System.out.println("Passed " + checks + " waypoint progression checks.");
    }
}
