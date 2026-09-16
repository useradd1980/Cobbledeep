package dev.cobbledeep.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Renders tactical selection and movement-destination rings. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalMoveMarkerRenderer
{
    private static final int TEXTURE_SIZE = 128;
    private static final float RADIUS = 0.92F;
    private static final float DESTINATION_PULSE = 0.09F;
    private static final double HEIGHT_OFFSET = 0.10;
    private static final double FOLLOW_RESPONSE = 24.0;
    private static ResourceLocation selectionRingTexture;
    private static ResourceLocation destinationRingTexture;
    private static LocalPlayer followedPlayer;
    private static Vec3 smoothedTarget;
    private static long previousFrameNanos;

    private TacticalMoveMarkerRenderer() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        // Draw after the tactical fog composite, retaining world depth for
        // terrain occlusion.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (!TacticalCameraController.isEnabled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        Vec3 playerTarget = smoothTarget(
                minecraft.player.getPosition(event.getPartialTick()), minecraft.player);
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        Vec3 destination = TacticalCameraController.getMovementTarget();
        if (destination != null)
        {
            double time = minecraft.level.getGameTime() + event.getPartialTick();
            float pulse = (float)Math.sin(time * 0.18);
            renderRing(buffers, event, camera, destination,
                    RADIUS + pulse * DESTINATION_PULSE,
                    (float)(0.84 + (pulse + 1.0) * 0.06), true);
        }

        renderRing(buffers, event, camera, playerTarget, RADIUS, 0.92F, false);
    }

    private static void renderRing(MultiBufferSource.BufferSource buffers,
                                   RenderLevelStageEvent event, Vec3 camera, Vec3 target,
                                   float radius, float alpha, boolean destination)
    {
        // AFTER_LEVEL receives GameRenderer's effect pose, not the view matrix
        // supplied to LevelRenderer. Reconstruct that view from the camera;
        // never mutate the camera's quaternion while taking its inverse.
        Matrix4f pose = new Matrix4f().rotation(
                new Quaternionf(event.getCamera().rotation()).conjugate());
        pose.translate(
                (float)(target.x - camera.x),
                (float)(target.y - camera.y + HEIGHT_OFFSET),
                (float)(target.z - camera.z));

        RenderType renderType = RenderType.entityTranslucentEmissive(getRingTexture(destination));
        VertexConsumer vertices = buffers.getBuffer(renderType);
        ring(vertices, pose, radius, 0.0F, alpha);
        buffers.endBatch(renderType);
    }

    private static void ring(VertexConsumer vertices, Matrix4f pose, float radius, float y, float alpha)
    {
        vertex(vertices, pose, -radius, y, -radius, 0.0F, 0.0F, alpha);
        vertex(vertices, pose, -radius, y, radius, 0.0F, 1.0F, alpha);
        vertex(vertices, pose, radius, y, radius, 1.0F, 1.0F, alpha);
        vertex(vertices, pose, radius, y, -radius, 1.0F, 0.0F, alpha);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose,
                               float x, float y, float z, float u, float v, float alpha)
    {
        vertices.addVertex(pose, x, y, z)
                .setColor(0.55F, 1.0F, 0.68F, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static ResourceLocation getRingTexture(boolean destination)
    {
        ResourceLocation existing = destination ? destinationRingTexture : selectionRingTexture;
        if (existing != null) return existing;

        DynamicTexture texture = new DynamicTexture(TEXTURE_SIZE, TEXTURE_SIZE, false);
        NativeImage pixels = texture.getPixels();
        if (pixels == null) throw new IllegalStateException("Tactical ring texture has no pixel storage");
        double centre = (TEXTURE_SIZE - 1) * 0.5;
        for (int y = 0; y < TEXTURE_SIZE; y++)
            for (int x = 0; x < TEXTURE_SIZE; x++)
            {
                double distance = Math.hypot(x - centre, y - centre) / centre;
                // The player uses the inner band. Destinations add the outer
                // band while retaining exactly the same width and colour.
                double innerRing = band(0.67, 0.685, 0.710, 0.725, distance);
                double outerRing = band(0.84, 0.855, 0.880, 0.895, distance);
                double coverage = destination ? Math.max(innerRing, outerRing) : innerRing;
                int alpha = (int)Math.round(255.0 * coverage);
                pixels.setPixelRGBA(x, y, (alpha << 24) | 0x00FFFFFF);
            }
        texture.upload();
        ResourceLocation registered = Minecraft.getInstance().getTextureManager().register(
                destination ? "cobbledeep_tactical_destination_ring"
                        : "cobbledeep_tactical_selection_ring", texture);
        if (destination) destinationRingTexture = registered;
        else selectionRingTexture = registered;
        return registered;
    }

    private static Vec3 smoothTarget(Vec3 current, LocalPlayer player)
    {
        long now = System.nanoTime();
        double elapsed = (now - previousFrameNanos) / 1_000_000_000.0;
        if (followedPlayer != player || smoothedTarget == null || elapsed <= 0.0
                || elapsed > 0.25 || smoothedTarget.distanceToSqr(current) > 16.0)
        {
            followedPlayer = player;
            smoothedTarget = current;
        }
        else
        {
            double blend = 1.0 - Math.exp(-FOLLOW_RESPONSE * elapsed);
            smoothedTarget = smoothedTarget.lerp(current, blend);
        }
        previousFrameNanos = now;
        return smoothedTarget;
    }

    private static double band(double innerStart, double innerEnd,
                               double outerStart, double outerEnd, double distance)
    {
        return smoothstep(innerStart, innerEnd, distance)
                * (1.0 - smoothstep(outerStart, outerEnd, distance));
    }

    private static double smoothstep(double low, double high, double value)
    {
        double t = Math.max(0.0, Math.min(1.0, (value - low) / (high - low)));
        return t * t * (3.0 - 2.0 * t);
    }
}
