package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
    private static final int OUTER_PARTICLES = 24;
    private static final int INNER_PARTICLES = 12;
    private static final double BASE_RADIUS = 0.72;
    private static final double PULSE_AMOUNT = 0.13;
    private static final double HEIGHT_OFFSET = 0.13;
    private static final double HEIGHT_PULSE = 0.045;

    private static final DustParticleOptions RING_PARTICLE =
            new DustParticleOptions(new Vector3f(0.12F, 1.00F, 0.38F), 1.05F);
    private static final DustParticleOptions INNER_PARTICLE =
            new DustParticleOptions(new Vector3f(0.38F, 1.00F, 0.62F), 0.72F);
    private static final DustParticleOptions CHASE_PARTICLE =
            new DustParticleOptions(new Vector3f(0.82F, 1.00F, 0.88F), 1.45F);

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
        double pulse = Math.sin(time * 0.22);
        double radius = BASE_RADIUS + pulse * PULSE_AMOUNT;
        double y = target.y + HEIGHT_OFFSET + (pulse + 1.0) * HEIGHT_PULSE;
        double phase = time * 0.10;

        int chaseIndex = Math.floorMod((int)Math.floor(phase * OUTER_PARTICLES / (Math.PI * 2.0)), OUTER_PARTICLES);

        for (int i = 0; i < OUTER_PARTICLES; i++)
        {
            double angle = Math.PI * 2.0 * i / OUTER_PARTICLES + phase;
            double x = target.x + Math.cos(angle) * radius;
            double z = target.z + Math.sin(angle) * radius;

            DustParticleOptions particle = distanceAroundRing(i, chaseIndex) <= 1
                    ? CHASE_PARTICLE
                    : RING_PARTICLE;

            minecraft.level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
        }

        // A counter-rotating inner halo makes the destination readable from
        // high zoom levels without filling the centre or hiding the terrain.
        double innerRadius = radius * 0.58;
        for (int i = 0; i < INNER_PARTICLES; i++)
        {
            double angle = Math.PI * 2.0 * i / INNER_PARTICLES - phase * 0.65;
            minecraft.level.addParticle(INNER_PARTICLE,
                    target.x + Math.cos(angle) * innerRadius,
                    y + 0.025,
                    target.z + Math.sin(angle) * innerRadius,
                    0.0, 0.0, 0.0);
        }

        // Three sparse emissive sparks give the ring a true glow while the
        // dust layers retain a clean circular silhouette.
        for (int offset = -1; offset <= 1; offset++)
        {
            double angle = Math.PI * 2.0 * (chaseIndex + offset) / OUTER_PARTICLES + phase;
            minecraft.level.addParticle(ParticleTypes.END_ROD,
                    target.x + Math.cos(angle) * radius,
                    y + 0.035,
                    target.z + Math.sin(angle) * radius,
                    0.0, 0.002, 0.0);
        }
    }

    private static int distanceAroundRing(int a, int b)
    {
        int direct = Math.abs(a - b);
        return Math.min(direct, OUTER_PARTICLES - direct);
    }
}
