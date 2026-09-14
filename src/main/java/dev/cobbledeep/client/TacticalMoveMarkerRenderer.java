package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * Renders the tactical move destination marker.
 *
 * The earlier world-line implementation depended on Forge's 1.21.1 render-stage
 * matrices and was not reliably visible with the tactical camera. This version
 * deliberately uses client-side particles instead, which are rendered by
 * Minecraft's normal particle pipeline and therefore remain visible regardless
 * of the custom camera distance/angle.
 */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalMoveMarkerRenderer
{
    private static final int PARTICLES = 24;
    private static final double BASE_RADIUS = 0.72;
    private static final double PULSE_AMOUNT = 0.06;
    private static final double HEIGHT_OFFSET = 0.10;

    private static final DustParticleOptions RING_PARTICLE =
            new DustParticleOptions(new Vector3f(0.20F, 0.90F, 0.32F), 0.75F);
    private static final DustParticleOptions CHASE_PARTICLE =
            new DustParticleOptions(new Vector3f(0.65F, 1.00F, 0.72F), 1.05F);

    private static int tickCounter;

    private TacticalMoveMarkerRenderer() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event)
    {
        if (!TacticalCameraController.isEnabled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused()) return;

        Vec3 target = TacticalCameraController.getMovementTarget();
        if (target == null) return;

        // Every other client tick is enough to keep the ring visually continuous
        // without flooding the particle engine with redundant particles.
        tickCounter++;
        if ((tickCounter & 1) != 0) return;

        double time = minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        double radius = BASE_RADIUS + Math.sin(time * 0.22) * PULSE_AMOUNT;
        double phase = time * 0.12;

        int chaseIndex = Math.floorMod((int)Math.floor(phase * PARTICLES / (Math.PI * 2.0)), PARTICLES);

        for (int i = 0; i < PARTICLES; i++)
        {
            double angle = Math.PI * 2.0 * i / PARTICLES + phase;
            double x = target.x + Math.cos(angle) * radius;
            double y = target.y + HEIGHT_OFFSET;
            double z = target.z + Math.sin(angle) * radius;

            DustParticleOptions particle = distanceAroundRing(i, chaseIndex) <= 1
                    ? CHASE_PARTICLE
                    : RING_PARTICLE;

            minecraft.level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
        }
    }

    private static int distanceAroundRing(int a, int b)
    {
        int direct = Math.abs(a - b);
        return Math.min(direct, PARTICLES - direct);
    }
}
