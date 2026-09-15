package dev.cobbledeep.exploration;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 360-degree, eye-origin visibility. Camera state and exploration memory are irrelevant. */
public final class CharacterSight
{
    public static final double RANGE = 24.0;

    private CharacterSight() { }

    public static BlockHitResult trace(Player observer, Vec3 start, Vec3 end)
    {
        if (start.distanceToSqr(end) > RANGE * RANGE + 0.001) return null;
        Level level = observer.level();
        // Refuse missing chunks before clipping: neither client nor server should
        // load terrain just because a visibility ray crossed its boundary.
        int minX = Math.floorDiv(Mth.floor(Math.min(start.x, end.x)), 16);
        int maxX = Math.floorDiv(Mth.floor(Math.max(start.x, end.x)), 16);
        int minZ = Math.floorDiv(Mth.floor(Math.min(start.z, end.z)), 16);
        int maxZ = Math.floorDiv(Mth.floor(Math.max(start.z, end.z)), 16);
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++)
                if (!level.hasChunkAt(new BlockPos(x * 16, Mth.floor(start.y), z * 16))) return null;
        return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, observer));
    }

    public static boolean seesPoint(Player observer, Vec3 start, Vec3 end)
    {
        BlockHitResult hit = trace(observer, start, end);
        return hit != null && hit.getType() == HitResult.Type.MISS;
    }

    public static boolean seesEntity(Player observer, LivingEntity target, float partialTick)
    {
        if (observer == target) return true;
        if (observer.level() != target.level()) return false;
        Vec3 eye = observer.getEyePosition(partialTick);
        Vec3 shift = target.getPosition(partialTick).subtract(target.position());
        AABB box = target.getBoundingBox().move(shift);
        double cx = (box.minX + box.maxX) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;
        double middle = box.minY + box.getYsize() * 0.5;
        double dx = box.getXsize() * 0.35;
        double dz = box.getZsize() * 0.35;
        // Head, centre, lower body and four inset corners allow partial exposure
        // through doorways without seeing targets wholly behind a solid wall.
        Vec3[] samples = {
                target.getEyePosition(partialTick), new Vec3(cx, middle, cz),
                new Vec3(cx, box.minY + box.getYsize() * 0.15, cz),
                new Vec3(cx - dx, middle, cz - dz), new Vec3(cx + dx, middle, cz - dz),
                new Vec3(cx - dx, middle, cz + dz), new Vec3(cx + dx, middle, cz + dz)
        };
        for (Vec3 point : samples)
            if (seesPoint(observer, eye, point)) return true;
        return false;
    }
}
