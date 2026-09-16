package dev.cobbledeep.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Minecraft;
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

/** Renders a clean, full-bright tactical destination ring. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalMoveMarkerRenderer
{
    private static final int TEXTURE_SIZE = 128;
    private static final double BASE_RADIUS = 0.92;
    private static final double PULSE_AMOUNT = 0.09;
    private static final double HEIGHT_OFFSET = 0.075;
    private static ResourceLocation ringTexture;

    private TacticalMoveMarkerRenderer() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        // Draw after the tactical fog composite, retaining world depth for
        // terrain occlusion.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (!TacticalCameraController.isEnabled()) return;

        Vec3 target = TacticalCameraController.getMovementTarget();
        if (target == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        double time = minecraft.level.getGameTime() + event.getPartialTick();
        float radius = (float)(BASE_RADIUS + Math.sin(time * 0.18) * PULSE_AMOUNT);
        float alpha = (float)(0.82 + (Math.sin(time * 0.18) + 1.0) * 0.07);
        Vec3 camera = event.getCamera().getPosition();

        // AFTER_LEVEL receives GameRenderer's effect pose, not the view matrix
        // supplied to LevelRenderer. Reconstruct that view from the camera;
        // never mutate the camera's quaternion while taking its inverse.
        Matrix4f pose = new Matrix4f().rotation(
                new Quaternionf(event.getCamera().rotation()).conjugate());
        pose.translate(
                (float)(target.x - camera.x),
                (float)(target.y - camera.y + HEIGHT_OFFSET),
                (float)(target.z - camera.z));

        RenderType renderType = RenderType.entityTranslucentEmissive(getRingTexture());
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(renderType);

        // A faint, slightly larger copy supplies a restrained halo. Both
        // layers use one smooth ring texture, so there are no dusty particles.
        ring(vertices, pose, radius * 1.10F, 0.0F, 0.24F);
        ring(vertices, pose, radius, 0.004F, alpha);
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
                .setColor(0.21F, 1.0F, 0.45F, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static ResourceLocation getRingTexture()
    {
        if (ringTexture != null) return ringTexture;

        DynamicTexture texture = new DynamicTexture(TEXTURE_SIZE, TEXTURE_SIZE, false);
        NativeImage pixels = texture.getPixels();
        if (pixels == null) throw new IllegalStateException("Destination ring texture has no pixel storage");
        double centre = (TEXTURE_SIZE - 1) * 0.5;
        for (int y = 0; y < TEXTURE_SIZE; y++)
            for (int x = 0; x < TEXTURE_SIZE; x++)
            {
                double distance = Math.hypot(x - centre, y - centre) / centre;
                // Smooth inner and outer edges around a narrow, solid band.
                double inner = smoothstep(0.69, 0.73, distance);
                double outer = 1.0 - smoothstep(0.88, 0.93, distance);
                int alpha = (int)Math.round(255.0 * inner * outer);
                pixels.setPixelRGBA(x, y, (alpha << 24) | 0x00FFFFFF);
            }
        texture.upload();
        ringTexture = Minecraft.getInstance().getTextureManager()
                .register("cobbledeep_tactical_destination_ring", texture);
        return ringTexture;
    }

    private static double smoothstep(double low, double high, double value)
    {
        double t = Math.max(0.0, Math.min(1.0, (value - low) / (high - low)));
        return t * t * (3.0 - 2.0 * t);
    }
}
