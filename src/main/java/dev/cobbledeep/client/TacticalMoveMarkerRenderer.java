package dev.cobbledeep.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** Renders the animated tactical move destination marker. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalMoveMarkerRenderer
{
    private static final int SEGMENTS = 64;
    private static final double BASE_RADIUS = 0.72;
    private static final double PULSE_AMOUNT = 0.07;
    private static final double HEIGHT_OFFSET = 0.08;
    private static final RenderType MARKER_LINES = RenderType.debugLineStrip(3.0);

    private TacticalMoveMarkerRenderer() { }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        // AFTER_ENTITIES is a reliable point for world-space debug-style lines in
        // Forge 1.21.1. Rendering on the translucent-block stage can put the line
        // buffer into the wrong render target and make the marker disappear.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (!TacticalCameraController.isEnabled()) return;

        Vec3 target = TacticalCameraController.getMovementTarget();
        if (target == null) return;

        Vec3 camera = event.getCamera().getPosition();
        double time = System.nanoTime() * 1.0e-9;
        double radius = BASE_RADIUS + Math.sin(time * 4.0) * PULSE_AMOUNT;
        double phase = time * 2.8;

        // Forge 1.21.1 exposes the active world render transform as Matrix4f.
        // Work on a copy and translate from world coordinates into camera-relative
        // coordinates, matching the rest of the level renderer.
        @SuppressWarnings("removal")
        Matrix4f pose = new Matrix4f(event.getPoseStack());
        pose.translate(
                (float)(target.x - camera.x),
                (float)(target.y - camera.y + HEIGHT_OFFSET),
                (float)(target.z - camera.z));

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(MARKER_LINES);

        // debugLineStrip renders a single continuous polyline, so emit one extra
        // vertex at 2π to close the circle cleanly.
        for (int i = 0; i <= SEGMENTS; i++)
        {
            double angle = Math.PI * 2.0 * i / SEGMENTS;
            float x = (float)(Math.cos(angle) * radius);
            float z = (float)(Math.sin(angle) * radius);

            double chase = 0.5 + 0.5 * Math.cos(angle - phase);
            int green = (int)(135.0 + chase * 120.0);
            int blue = (int)(90.0 + chase * 95.0);
            int alpha = (int)(185.0 + chase * 70.0);

            lines.addVertex(pose, x, 0.0F, z)
                    .setColor(75, green, blue, alpha)
                    .setNormal(0.0F, 1.0F, 0.0F);
        }

        buffers.endBatch(MARKER_LINES);
    }
}
