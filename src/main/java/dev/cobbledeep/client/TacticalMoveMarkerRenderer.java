package dev.cobbledeep.client;

import com.mojang.blaze3d.vertex.PoseStack;
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

/** Renders the animated tactical move destination marker. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalMoveMarkerRenderer
{
    private static final int SEGMENTS = 48;
    private static final double BASE_RADIUS = 0.68;
    private static final double PULSE_AMOUNT = 0.06;
    private static final double HEIGHT_OFFSET = 0.035;

    private TacticalMoveMarkerRenderer() { }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!TacticalCameraController.isEnabled()) return;

        Vec3 target = TacticalCameraController.getMovementTarget();
        if (target == null) return;

        Vec3 camera = event.getCamera().getPosition();
        double time = System.nanoTime() * 1.0e-9;
        double radius = BASE_RADIUS + Math.sin(time * 4.0) * PULSE_AMOUNT;
        double phase = time * 2.6;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(
                target.x - camera.x,
                target.y - camera.y + HEIGHT_OFFSET,
                target.z - camera.z);

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i < SEGMENTS; i++)
        {
            double a0 = Math.PI * 2.0 * i / SEGMENTS;
            double a1 = Math.PI * 2.0 * (i + 1) / SEGMENTS;

            float x0 = (float)(Math.cos(a0) * radius);
            float z0 = (float)(Math.sin(a0) * radius);
            float x1 = (float)(Math.cos(a1) * radius);
            float z1 = (float)(Math.sin(a1) * radius);

            double mid = (a0 + a1) * 0.5;
            double chase = 0.5 + 0.5 * Math.cos(mid - phase);
            int brightness = (int)(110.0 + chase * 145.0);
            int alpha = (int)(150.0 + chase * 105.0);

            lines.addVertex(pose, x0, 0.0F, z0)
                    .setColor(70, brightness, 95, alpha)
                    .setNormal(0.0F, 1.0F, 0.0F);
            lines.addVertex(pose, x1, 0.0F, z1)
                    .setColor(70, brightness, 95, alpha)
                    .setNormal(0.0F, 1.0F, 0.0F);
        }

        buffers.endBatch(RenderType.lines());
        poseStack.popPose();
    }
}
