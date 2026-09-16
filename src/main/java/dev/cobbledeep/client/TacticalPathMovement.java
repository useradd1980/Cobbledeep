package dev.cobbledeep.client;

import dev.cobbledeep.pathfinding.GridPathfinder;
import dev.cobbledeep.pathfinding.GridPathfinder.Node;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Follows collision-checked waypoints using ordinary player input, never teleportation. */
final class TacticalPathMovement
{
    private static TacticalWalkWorld world;
    private static GridPathfinder.Search search;
    private static List<Node> route = List.of();
    private static Vec3 target;
    private static int waypoint, stalled, retries;
    private static double bestDistance;
    private static boolean forwardHeld, jumpHeld;

    static Vec3 target() { return target; }

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
        route = List.of(); waypoint = stalled = 0; bestDistance = Double.POSITIVE_INFINITY;
        world = new TacticalWalkWorld(mc.player);
        Node start = world.nearest(mc.player.position());
        Node goal = new Node((int)Math.floor(target.x), (int)Math.round(target.y * 16), (int)Math.floor(target.z));
        if (start == null || !world.canTravel(mc.player.position(), start) || !world.standable(goal))
        {
            fail(mc, "No safe route from here.");
            return;
        }
        search = new GridPathfinder.Search(world, start, goal, 4096, 64);
        message(mc, "Finding route...");
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
            if (status == GridPathfinder.Status.SEARCHING) return;
            if (status != GridPathfinder.Status.FOUND)
            {
                fail(mc, status == GridPathfinder.Status.LIMIT
                        ? "Route search limit reached. Try a closer destination." : "No safe walking route found.");
                return;
            }
            route = search.path(); search = null;
            message(mc, "");
        }
        Node node = route.get(waypoint);
        Vec3 point = TacticalWalkWorld.point(node);
        double distance = Math.hypot(point.x - mc.player.getX(), point.z - mc.player.getZ());
        if (distance <= 0.18 && Math.abs(point.y - mc.player.getY()) <= 0.35)
        {
            if (++waypoint >= route.size()) { stop(mc); return; }
            node = route.get(waypoint); point = TacticalWalkWorld.point(node);
            distance = Math.hypot(point.x - mc.player.getX(), point.z - mc.player.getZ());
            bestDistance = Double.POSITIVE_INFINITY; stalled = 0;
        }
        if (distance < bestDistance - 0.025) { bestDistance = distance; stalled = 0; }
        else stalled++;
        if (mc.player.onGround() && (!world.canTravel(mc.player.position(), node) || stalled >= 30))
        {
            if (++retries > 2) fail(mc, "Route blocked. Choose another destination.");
            else plan(mc);
            return;
        }
        // Also stop if a fall or displacement prevents progress while airborne.
        if (stalled >= 80 || distance > 2.5 || mc.player.getY() < point.y - 1.25)
        { fail(mc, "Movement interrupted. Choose another destination."); return; }
        double dx = point.x - mc.player.getX(), dz = point.z - mc.player.getZ();
        mc.player.setYRot((float)Math.toDegrees(Math.atan2(-dx, dz)));
        boolean jump = point.y - mc.player.getY() > 0.6 && distance < 1.35 && mc.player.onGround();
        input(mc, true, jump);
    }

    static void stop(Minecraft mc)
    {
        input(mc, false, false);
        target = null; world = null; search = null; route = List.of();
        waypoint = stalled = retries = 0;
    }

    private static void input(Minecraft mc, boolean forward, boolean jump)
    {
        if (forwardHeld != forward) { mc.options.keyUp.setDown(forward); forwardHeld = forward; }
        if (jumpHeld != jump) { mc.options.keyJump.setDown(jump); jumpHeld = jump; }
    }

    private static void fail(Minecraft mc, String text) { stop(mc); message(mc, text); }
    private static void message(Minecraft mc, String text)
    {
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(text), true);
    }
}
