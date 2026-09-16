package dev.cobbledeep.client;

import dev.cobbledeep.pathfinding.GridPathfinder;
import dev.cobbledeep.pathfinding.GridPathfinder.Node;
import dev.cobbledeep.pathfinding.MovementProgress;
import dev.cobbledeep.pathfinding.WaypointProgress;
import dev.cobbledeep.pathfinding.WalkStepRules;
import dev.cobbledeep.pathfinding.RouteRecovery;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Follows collision-checked waypoints using ordinary player input, never teleportation. */
final class TacticalPathMovement
{
    private static final int POST_CLIMB_GRACE_TICKS = 6;
    private static final long ARRIVAL_MARKER_NANOS = 500_000_000L;
    private static TacticalWalkWorld world;
    private static GridPathfinder.Search search;
    private static boolean searchWhileMoving;
    private static List<Node> route = List.of();
    private static Vec3 target;
    private static Vec3 arrivedTarget;
    private static long arrivalMarkerUntil;
    private static Vec3 pathStart;
    private static double previousWaitY = Double.NaN;
    private static int waypoint, stalled, blockedTicks, postClimbGraceTicks;
    private static final RouteRecovery recovery = new RouteRecovery();
    private static final MovementProgress movement = new MovementProgress();
    private static boolean forwardHeld, jumpHeld;

    static Vec3 target() { return target; }

    static Vec3 markerTarget()
    {
        if (target != null) return target;
        if (arrivedTarget != null && System.nanoTime() < arrivalMarkerUntil) return arrivedTarget;
        arrivedTarget = null;
        return null;
    }

    static void start(Minecraft mc, Vec3 clicked)
    {
        stop(mc);
        if (mc.player == null || !mc.player.onGround() || mc.player.getAbilities().flying)
        {
            message(mc, "Choose a route while standing on the ground.");
            return;
        }
        world = new TacticalWalkWorld(mc.player);
        Node destination = world.nearest(clicked);
        if (destination == null) { fail(mc, "No walkable ground at that destination."); return; }
        target = TacticalWalkWorld.point(destination);
        plan(mc);
    }

    private static void plan(Minecraft mc)
    {
        input(mc, false, false);
        route = List.of(); waypoint = stalled = 0;
        movement.reset();
        previousWaitY = Double.NaN;
        blockedTicks = postClimbGraceTicks = 0;
        beginSearch(mc, false, "Finding route...");
    }

    private static void beginSearch(Minecraft mc, boolean whileMoving, String status)
    {
        if (!whileMoving) input(mc, false, false);
        world = new TacticalWalkWorld(mc.player);
        Node start = world.nearestReachable(mc.player.position());
        Node goal = new Node((int)Math.floor(target.x), (int)Math.round(target.y * 16), (int)Math.floor(target.z));
        if (start == null || !world.canTravel(mc.player.position(), start) || !world.standable(goal))
        {
            fail(mc, "No safe route from here.");
            return;
        }
        search = new GridPathfinder.Search(world, start, goal, 4096, 64);
        searchWhileMoving = whileMoving && !route.isEmpty();
        message(mc, status);
    }

    static void tick(Minecraft mc)
    {
        if (target == null) return;
        if (mc.player == null || mc.level != world.level || mc.player != world.player
                || !mc.player.isAlive() || mc.screen != null || !mc.isWindowActive()
                || mc.player.getAbilities().flying || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown() || mc.options.keyDown.isDown()
                || mc.options.keyShift.isDown() || (mc.options.keyJump.isDown() && !jumpHeld))
        {
            stop(mc);
            return;
        }
        if (search != null)
        {
            var status = search.step(48);
            if (status == GridPathfinder.Status.SEARCHING)
            {
                if (!searchWhileMoving) return;
            }
            else if (status != GridPathfinder.Status.FOUND)
            {
                fail(mc, status == GridPathfinder.Status.LIMIT
                        ? "Route search limit reached. Try a closer destination." : "No safe walking route found.");
                return;
            }
            else
            {
                List<Node> replacement = search.path();
                search = null;
                searchWhileMoving = false;

                // A rolling search began behind the moving player. Join the
                // furthest replacement node that is now directly reachable,
                // avoiding a turn back toward the old search origin.
                int join = -1;
                for (int i = 0; i < replacement.size(); i++)
                    if (world.canTravel(mc.player.position(), replacement.get(i))) join = i;
                if (join < 0)
                {
                    // The player outran this search along the old route. Start
                    // another rolling search instead of walking backward.
                    beginSearch(mc, true, "Updating route (refreshing search)...");
                }
                else
                {
                    route = List.copyOf(replacement.subList(join, replacement.size()));
                    pathStart = mc.player.position();
                    waypoint = 0;
                    movement.reset();
                    stalled = blockedTicks = postClimbGraceTicks = 0;
                    previousWaitY = Double.NaN;
                    message(mc, "");
                }
            }
        }
        Node node = route.get(waypoint);
        Vec3 point = TacticalWalkWorld.point(node);
        while (true)
        {
            Vec3 from = waypoint == 0 ? pathStart : TacticalWalkWorld.point(route.get(waypoint - 1));
            var progress = WaypointProgress.classify(from.x, from.z, point.x, point.y, point.z,
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), waypoint == route.size() - 1);
            if (progress == WaypointProgress.State.APPROACH) break;
            if (progress == WaypointProgress.State.WAIT_FOR_HEIGHT)
            {
                // Reaching the horizontal plane of an uphill node does not
                // mean the climb has finished. Preserve the validated edge's
                // heading and movement until the player's feet rise onto it.
                // Aiming back at the node centre here caused the old stop,
                // replan and retry cycle at final steps and uphill corners.
                if (WaypointProgress.waitingForClimb(point.y, mc.player.getY())
                        && world.canTravel(from, node))
                {
                    if (Double.isNaN(previousWaitY) || Math.abs(previousWaitY - mc.player.getY()) > 0.025) stalled = 0;
                    else stalled++;
                    previousWaitY = mc.player.getY();
                    double edgeX = point.x - from.x, edgeZ = point.z - from.z;
                    mc.player.setYRot((float)Math.toDegrees(Math.atan2(-edgeX, edgeZ)));
                    boolean jump = WalkStepRules.shouldJump(point.y - mc.player.getY(),
                            Math.hypot(point.x - mc.player.getX(), point.z - mc.player.getZ()), mc.player.onGround());
                    input(mc, true, jump);
                    if (stalled >= 30 && mc.player.onGround() && search == null)
                        replan(mc, false, "Finding route (uphill height wait)...");
                    else if (stalled >= 80) fail(mc, "Movement interrupted. Choose another destination.");
                    return;
                }
                // Continue along a straight stair run only when the existing
                // collision checks approve the next edge from the actual body.
                // At corners or blocked edges, let gravity finish the step
                // without turning back toward the waypoint we just passed.
                boolean continueAhead = false;
                String landingWaitCause = "route end";
                if (waypoint + 1 < route.size())
                {
                    Node next = route.get(waypoint + 1);
                    Vec3 nextPoint = TacticalWalkWorld.point(next);
                    boolean straight = WaypointProgress.straight(from.x, from.z,
                            point.x, point.z, nextPoint.x, nextPoint.z);
                    boolean travelClear = world.canTravel(mc.player.position(), next);
                    if (WaypointProgress.retryLandingEdgeFromPath(mc.player.onGround(),
                            postClimbGraceTicks, travelClear))
                    {
                        // This branch returns before the general post-climb
                        // fallback below. Retry the same planned edge from its
                        // canonical node so a settling body cannot reject it.
                        travelClear = world.canTravel(point, next);
                    }
                    continueAhead = WaypointProgress.canLookAhead(mc.player.onGround(), straight, travelClear);
                    landingWaitCause = !travelClear ? "next edge rejected" : "airborne turn";
                }
                if (!continueAhead)
                {
                    if (Double.isNaN(previousWaitY) || Math.abs(previousWaitY - mc.player.getY()) > 0.025) stalled = 0;
                    else stalled++;
                    previousWaitY = mc.player.getY();
                    input(mc, false, false);
                    if (stalled >= 30 && mc.player.onGround() && search == null)
                        replan(mc, false, "Finding route (landing height wait: " + landingWaitCause + ")...");
                    else if (stalled >= 80) fail(mc, "Movement interrupted. Choose another destination.");
                    return;
                }
            }
            if (progress == WaypointProgress.State.REACHED && waypoint > 0)
                recovery.reachedWaypoint(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            // Straight uphill runs can advance from WAIT_FOR_HEIGHT while the
            // player is above the landing plane. That is still a completed
            // climb and needs the same post-landing transition as REACHED.
            if (WaypointProgress.completedClimb(progress, point.y - from.y))
                postClimbGraceTicks = POST_CLIMB_GRACE_TICKS;
            if (++waypoint >= route.size()) { arrive(mc); return; }
            node = route.get(waypoint); point = TacticalWalkWorld.point(node);
            movement.reset();
            stalled = 0;
            previousWaitY = Double.NaN;
        }
        previousWaitY = Double.NaN;
        double distance = Math.hypot(point.x - mc.player.getX(), point.z - mc.player.getZ());
        int stationaryTicks = movement.update(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        boolean nextEdgeClear = world.canTravel(mc.player.position(), node);
        if (!nextEdgeClear && postClimbGraceTicks > 0 && mc.player.onGround())
        {
            // Immediately after landing, the live body can still touch the
            // ledge behind it. Recheck the planned edge from its graph origin;
            // this still catches a door/block placed into the route itself.
            Vec3 edgeOrigin = waypoint == 0 ? pathStart : TacticalWalkWorld.point(route.get(waypoint - 1));
            nextEdgeClear = world.canTravel(edgeOrigin, node);
        }
        if (mc.player.onGround() && !nextEdgeClear)
        {
            // Recheck a brief contact before spending a route recovery. Keep
            // movement released throughout: this never drives through a wall.
            input(mc, false, false);
            if (++blockedTicks >= 3 && search == null)
                replan(mc, false, "Finding route (edge clearance)...");
            return;
        }
        blockedTicks = 0;
        // The climb may be marked reached near the jump apex. Preserve the
        // full grace interval until the player actually touches down.
        if (postClimbGraceTicks > 0 && mc.player.onGround()) postClimbGraceTicks--;
        if (mc.player.onGround() && stationaryTicks >= 30 && search == null)
        {
            // The next edge is still safe, so keep following it while A*
            // prepares a fresher route from the player's current position.
            replan(mc, true, "Updating route (no movement)...");
            return;
        }
        // Also stop if a fall or displacement prevents progress while airborne.
        if (stationaryTicks >= 80 || distance > 2.5 || mc.player.getY() < point.y - 1.25)
        { fail(mc, "Movement interrupted. Choose another destination."); return; }
        double dx = point.x - mc.player.getX(), dz = point.z - mc.player.getZ();
        mc.player.setYRot((float)Math.toDegrees(Math.atan2(-dx, dz)));
        boolean jump = WalkStepRules.shouldJump(point.y - mc.player.getY(), distance, mc.player.onGround());
        input(mc, true, jump);
    }

    static void stop(Minecraft mc)
    {
        input(mc, false, false);
        target = null; pathStart = null; world = null; search = null; route = List.of();
        arrivedTarget = null; arrivalMarkerUntil = 0L;
        searchWhileMoving = false;
        movement.reset();
        previousWaitY = Double.NaN;
        waypoint = stalled = blockedTicks = postClimbGraceTicks = 0;
        recovery.clear();
    }

    private static void arrive(Minecraft mc)
    {
        Vec3 reached = target;
        stop(mc);
        arrivedTarget = reached;
        arrivalMarkerUntil = System.nanoTime() + ARRIVAL_MARKER_NANOS;
    }

    private static void input(Minecraft mc, boolean forward, boolean jump)
    {
        if (forwardHeld != forward) { mc.options.keyUp.setDown(forward); forwardHeld = forward; }
        if (jumpHeld != jump) { mc.options.keyJump.setDown(jump); jumpHeld = jump; }
    }

    private static void fail(Minecraft mc, String text) { stop(mc); message(mc, text); }
    private static void replan(Minecraft mc, boolean whileMoving, String status)
    {
        if (!recovery.retry(mc.player.getX(), mc.player.getY(), mc.player.getZ()))
            fail(mc, "Unable to continue from here. Choose another destination.");
        else
        {
            stalled = blockedTicks = postClimbGraceTicks = 0;
            beginSearch(mc, whileMoving, status);
        }
    }
    private static void message(Minecraft mc, String text)
    {
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(text), true);
    }
}
