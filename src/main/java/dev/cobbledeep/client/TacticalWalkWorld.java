package dev.cobbledeep.client;

import dev.cobbledeep.pathfinding.GridPathfinder;
import dev.cobbledeep.pathfinding.GridPathfinder.Node;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** Uses loaded block collision shapes and the player's standing footprint. */
final class TacticalWalkWorld implements GridPathfinder.World
{
    final LocalPlayer player;
    final Level level;
    private final double halfWidth, height;

    TacticalWalkWorld(LocalPlayer player)
    {
        this.player = player; this.level = player.level();
        // Tiny inward epsilon avoids treating exact contact with a wall as
        // penetration when beginning a route beside it.
        halfWidth = player.getBbWidth() * 0.5 - 0.0001;
        height = Math.max(1.8, player.getBbHeight());
    }

    static Vec3 point(Node n) { return new Vec3(n.x() + 0.5, n.y(), n.z() + 0.5); }

    Node nearest(Vec3 point)
    {
        Node best = null;
        double score = Double.POSITIVE_INFINITY;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                for (Node n : surfaces(Mth.floor(point.x) + dx, Mth.floor(point.z) + dz, point.y - 1.05, point.y + 1.05))
                {
                    double d = point(n).distanceToSqr(point);
                    if (d < score) { score = d; best = n; }
                }
        return best;
    }

    @Override public List<Node> neighbours(Node from)
    {
        List<Node> result = new ArrayList<>();
        int[][] cardinal = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : cardinal)
            for (Node to : surfaces(from.x() + d[0], from.z() + d[1], from.y() - 1.0, from.y() + 1.0))
                if (canTravel(point(from), to)) result.add(to);
        // Both adjoining cardinal cells must be walkable at the same height:
        // no diagonally cutting through a wall corner or across a pit.
        List<Node> sides = List.copyOf(result);
        for (int dx : new int[] {-1, 1})
            for (int dz : new int[] {-1, 1})
            {
                Node sideX = new Node(from.x() + dx, from.y16(), from.z());
                Node sideZ = new Node(from.x(), from.y16(), from.z() + dz);
                Node to = new Node(from.x() + dx, from.y16(), from.z() + dz);
                if (sides.contains(sideX) && sides.contains(sideZ) && standable(to)
                        && canTravel(point(from), to)) result.add(to);
            }
        return result;
    }

    private List<Node> surfaces(int x, int z, double minY, double maxY)
    {
        List<Node> result = new ArrayList<>();
        for (int y = Mth.floor(minY) - 1; y <= Mth.floor(maxY); y++)
        {
            BlockPos pos = new BlockPos(x, y, z);
            if (!loaded(pos)) continue;
            for (AABB shape : level.getBlockState(pos).getCollisionShape(level, pos).toAabbs())
            {
                double top = y + shape.maxY;
                if (top < minY - 0.001 || top > maxY + 0.001) continue;
                Node n = new Node(x, (int)Math.round(top * 16), z);
                if (!result.contains(n) && standable(n)) result.add(n);
            }
        }
        return result;
    }

    private AABB body(Vec3 p)
    {
        return new AABB(p.x - halfWidth, p.y + 0.001, p.z - halfWidth,
                p.x + halfWidth, p.y + height, p.z + halfWidth);
    }

    private boolean loaded(BlockPos p)
    {
        return p.getY() >= level.getMinBuildHeight() && p.getY() < level.getMaxBuildHeight()
                && level.getWorldBorder().isWithinBounds(p) && level.hasChunkAt(p);
    }

    private boolean safe(AABB box)
    {
        for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX); x++)
            for (int y = Mth.floor(box.minY - 0.05); y <= Mth.floor(box.maxY); y++)
                for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ); z++)
                {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!loaded(p)) return false;
                    var state = level.getBlockState(p);
                    if (!state.getFluidState().isEmpty() || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                            || state.is(Blocks.CACTUS) || state.is(Blocks.MAGMA_BLOCK)
                            || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                            || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.SWEET_BERRY_BUSH)
                            || state.is(Blocks.WITHER_ROSE)) return false;
                }
        return true;
    }

    boolean standable(Node node)
    {
        AABB box = body(point(node));
        return safe(box) && level.noCollision(player, box)
                && !level.noCollision(player, box.move(0, -0.05, 0));
    }

    boolean canTravel(Vec3 from, Node to)
    {
        Vec3 end = point(to);
        double rise = end.y - from.y;
        if (Math.hypot(end.x - from.x, end.z - from.z) > 1.65
                || rise > 1.05 || rise < -1.05 || !standable(to)) return false;
        double lift = Math.max(0, rise);
        // A jump needs extra headroom above the one-block ledge. Slabs/stairs
        // use normal stepping, but still require a clear swept body volume.
        double jumpClearance = rise > 0.6 ? 0.3 : 0;
        AABB ascent = body(from).expandTowards(0, lift + jumpClearance, 0);
        AABB across = body(from.add(0, lift, 0)).expandTowards(end.x - from.x, jumpClearance, end.z - from.z);
        AABB descent = body(end).expandTowards(0, Math.max(0, -rise) + jumpClearance, 0);
        return safe(ascent) && safe(across) && safe(descent)
                && level.noCollision(player, ascent) && level.noCollision(player, across)
                && level.noCollision(player, descent);
    }
}
