package dev.cobbledeep.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.exploration.CharacterSight;
import dev.cobbledeep.relations.CharacterDisposition;
import dev.cobbledeep.relations.CharacterRelations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.WeakHashMap;

/** Renders tactical selection and movement-destination rings. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalMoveMarkerRenderer
{
    private static final int TEXTURE_SIZE = 128;
    private static final float RADIUS = 0.92F;
    private static final float DESTINATION_PULSE = 0.09F;
    private static final double HEIGHT_OFFSET = 0.10;
    private static final double FOLLOW_RESPONSE = 24.0;
    private static final float PLAYER_RED = 0.32F;
    private static final float PLAYER_GREEN = 0.82F;
    private static final float PLAYER_BLUE = 0.45F;
    private static final float FRIEND_RED = 0.20F;
    private static final float FRIEND_GREEN = 0.48F;
    private static final float FRIEND_BLUE = 1.00F;
    private static final float ENEMY_RED = 0.92F;
    private static final float ENEMY_GREEN = 0.16F;
    private static final float ENEMY_BLUE = 0.13F;
    private static ResourceLocation selectionRingTexture;
    private static ResourceLocation destinationRingTexture;
    private static ResourceLocation mergedDestinationRingTexture;
    private static LocalPlayer followedPlayer;
    private static Vec3 smoothedTarget;
    private static long previousFrameNanos;
    private static final Map<LivingEntity, FollowState> NPC_TARGETS = new WeakHashMap<>();

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

        renderNpcRings(minecraft, buffers, event, camera);

        Vec3 destination = TacticalCameraController.getMovementTarget();
        if (destination != null)
        {
            double time = minecraft.level.getGameTime() + event.getPartialTick();
            float pulse = (float)Math.sin(time * 0.18);
            boolean merged = TacticalCameraController.isShowingArrivalMarker();
            renderRing(buffers, event, camera, merged ? playerTarget : destination,
                    RADIUS + pulse * DESTINATION_PULSE,
                    (float)(0.84 + (pulse + 1.0) * 0.06), merged ? 2 : 1,
                    PLAYER_RED, PLAYER_GREEN, PLAYER_BLUE);
        }

        renderRing(buffers, event, camera, playerTarget, RADIUS, 0.92F, 0,
                PLAYER_RED, PLAYER_GREEN, PLAYER_BLUE);
    }

    private static void renderNpcRings(Minecraft minecraft, MultiBufferSource.BufferSource buffers,
                                       RenderLevelStageEvent event, Vec3 camera)
    {
        var area = minecraft.player.getBoundingBox().inflate(CharacterSight.RANGE);
        for (LivingEntity entity : minecraft.level.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate != minecraft.player && candidate.isAlive()))
        {
            CharacterDisposition disposition = CharacterRelations.disposition(entity);
            if (!CharacterRelations.hasNpcRing(disposition)
                    || !CharacterSight.seesEntity(minecraft.player, entity, event.getPartialTick())) continue;

            float red = disposition == CharacterDisposition.HOSTILE ? ENEMY_RED
                    : disposition == CharacterDisposition.FRIENDLY ? FRIEND_RED : PLAYER_RED;
            float green = disposition == CharacterDisposition.HOSTILE ? ENEMY_GREEN
                    : disposition == CharacterDisposition.FRIENDLY ? FRIEND_GREEN : PLAYER_GREEN;
            float blue = disposition == CharacterDisposition.HOSTILE ? ENEMY_BLUE
                    : disposition == CharacterDisposition.FRIENDLY ? FRIEND_BLUE : PLAYER_BLUE;
            Vec3 entityTarget = smoothNpcTarget(
                    entity.getPosition(event.getPartialTick()), entity);
            renderRing(buffers, event, camera, entityTarget,
                    RADIUS, 0.92F, 0, red, green, blue);
        }
    }

    private static void renderRing(MultiBufferSource.BufferSource buffers,
                                   RenderLevelStageEvent event, Vec3 camera, Vec3 target,
                                   float radius, float alpha, int style,
                                   float red, float green, float blue)
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

        RenderType renderType = RenderType.entityTranslucentEmissive(getRingTexture(style));
        VertexConsumer vertices = buffers.getBuffer(renderType);
        ring(vertices, pose, radius, 0.0F, alpha, red, green, blue);
        buffers.endBatch(renderType);
    }

    private static void ring(VertexConsumer vertices, Matrix4f pose, float radius, float y, float alpha,
                             float red, float green, float blue)
    {
        vertex(vertices, pose, -radius, y, -radius, 0.0F, 0.0F, alpha, red, green, blue);
        vertex(vertices, pose, -radius, y, radius, 0.0F, 1.0F, alpha, red, green, blue);
        vertex(vertices, pose, radius, y, radius, 1.0F, 1.0F, alpha, red, green, blue);
        vertex(vertices, pose, radius, y, -radius, 1.0F, 0.0F, alpha, red, green, blue);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose,
                               float x, float y, float z, float u, float v, float alpha,
                               float red, float green, float blue)
    {
        vertices.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static ResourceLocation getRingTexture(int style)
    {
        ResourceLocation existing = style == 0 ? selectionRingTexture
                : style == 1 ? destinationRingTexture : mergedDestinationRingTexture;
        if (existing != null) return existing;

        DynamicTexture texture = new DynamicTexture(TEXTURE_SIZE, TEXTURE_SIZE, false);
        NativeImage pixels = texture.getPixels();
        if (pixels == null) throw new IllegalStateException("Tactical ring texture has no pixel storage");
        double centre = (TEXTURE_SIZE - 1) * 0.5;
        for (int y = 0; y < TEXTURE_SIZE; y++)
            for (int x = 0; x < TEXTURE_SIZE; x++)
            {
                double distance = Math.hypot(x - centre, y - centre) / centre;
                // Selection uses the inner band, a distant destination uses
                // both, and a merged destination uses only its outer band.
                double innerRing = band(0.67, 0.685, 0.710, 0.725, distance);
                double outerRing = band(0.84, 0.855, 0.880, 0.895, distance);
                double coverage = style == 0 ? innerRing
                        : style == 1 ? Math.max(innerRing, outerRing) : outerRing;
                int alpha = (int)Math.round(255.0 * coverage);
                pixels.setPixelRGBA(x, y, (alpha << 24) | 0x00FFFFFF);
            }
        texture.upload();
        ResourceLocation registered = Minecraft.getInstance().getTextureManager().register(
                style == 0 ? "cobbledeep_tactical_selection_ring"
                        : style == 1 ? "cobbledeep_tactical_destination_ring"
                        : "cobbledeep_tactical_merged_destination_ring", texture);
        if (style == 0) selectionRingTexture = registered;
        else if (style == 1) destinationRingTexture = registered;
        else mergedDestinationRingTexture = registered;
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

    private static Vec3 smoothNpcTarget(Vec3 current, LivingEntity entity)
    {
        long now = System.nanoTime();
        FollowState state = NPC_TARGETS.get(entity);
        if (state == null)
        {
            state = new FollowState(current, now);
            NPC_TARGETS.put(entity, state);
            return current;
        }

        double elapsed = (now - state.previousFrameNanos) / 1_000_000_000.0;
        if (elapsed <= 0.0 || elapsed > 0.25 || state.position.distanceToSqr(current) > 16.0)
            state.position = current;
        else
        {
            double blend = 1.0 - Math.exp(-FOLLOW_RESPONSE * elapsed);
            state.position = state.position.lerp(current, blend);
        }
        state.previousFrameNanos = now;
        return state.position;
    }

    private static final class FollowState
    {
        private Vec3 position;
        private long previousFrameNanos;

        private FollowState(Vec3 position, long previousFrameNanos)
        {
            this.position = position;
            this.previousFrameNanos = previousFrameNanos;
        }
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
