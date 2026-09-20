package dev.cobbledeep.monster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cobbledeep.client.LootHighlightClient;
import dev.cobbledeep.exploration.CharacterSight;
import dev.cobbledeep.monster.GiantRatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Renders the authored polygon meshes and selects synchronized combat animations. */
public final class GiantRatRenderer extends EntityRenderer<GiantRatEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "cobbledeep", "textures/entity/giant_rat.png");
    // A solid-white texture keeps the blue loot highlight independent of the rat's fur.
    private static final ResourceLocation HIGHLIGHT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "cobbledeep", "textures/entity/loot_highlight_white.png");
    private static final ResourceLocation GEOMETRY = ResourceLocation.fromNamespaceAndPath(
            "cobbledeep", "models/entity/giant_rat.json");
    private static final String WALK = "animation.giant_rat.walk";
    private static final String IDLE = "animation.giant_rat_movements";
    private static final String DETECTION = "animation.giant_rat_detection";
    private static final String ATTACK = "animation.giant_rat_attack";
    private static final String HIT = "animation.giant_rat_hit";
    private static final String DEATH = "animation.giant_rat_death";
    private static final int LOOT_HIGHLIGHT_COLOR = 0xE900BFFF;
    private static final int FULL_BRIGHT = 0x00F000F0;
    private RatMeshModel model;

    public GiantRatRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.25f;
    }

    @Override
    public ResourceLocation getTextureLocation(GiantRatEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(GiantRatEntity rat, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            // Terrain can remain explored after the character moves away, but
            // creatures and corpses must only be visible in the character's
            // *current* sight. Use the same 24-block radius and LoS check as
            // tactical NPC rings; the camera's much longer view is not sight.
            Vec3 observer = minecraft.player.getPosition(partialTick);
            Vec3 target = rat.getPosition(partialTick);
            double dx = target.x - observer.x;
            double dz = target.z - observer.z;
            if (dx * dx + dz * dz > CharacterSight.RANGE * CharacterSight.RANGE
                    || !CharacterSight.seesEntity(minecraft.player, rat, partialTick)) {
                return; // Also suppresses corpse Tab highlight and entity shadow.
            }
        }

        if (model == null) {
            model = RatMeshModel.load(minecraft.getResourceManager(), GEOMETRY);
        }
        pose.pushPose();
        float bodyYaw = Mth.rotLerp(partialTick, rat.yBodyRotO, rat.yBodyRot);
        pose.mulPose(new Quaternionf().rotationY((180f - bodyYaw) * ((float) Math.PI / 180f)));
        // The custom export uses Blockbench coordinates: 16 model units == one Minecraft block.
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        VertexConsumer output = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        final String animation;
        final float animationTime;
        if (rat.isCorpse()) {
            // Death is highest priority; persistent corpses hold the final frame.
            animation = DEATH;
            animationTime = (rat.getDeathAnimationTicks() + partialTick) / 20.0f;
        } else if (rat.getHitAnimationTicks() > 0) {
            animation = HIT;
            animationTime = elapsed(GiantRatEntity.HIT_ANIMATION_TICKS,
                    rat.getHitAnimationTicks(), partialTick);
        } else if (rat.getAttackAnimationTicks() > 0) {
            animation = ATTACK;
            animationTime = elapsed(GiantRatEntity.ATTACK_ANIMATION_TICKS,
                    rat.getAttackAnimationTicks(), partialTick);
        } else if (rat.getDetectionAnimationTicks() > 0) {
            animation = DETECTION;
            animationTime = elapsed(GiantRatEntity.DETECTION_ANIMATION_TICKS,
                    rat.getDetectionAnimationTicks(), partialTick);
        } else {
            // Client interpolation often makes getDeltaMovement() appear to be zero for a
            // walking mob. Use built-in limb animation and observed positional change too.
            double dx = rat.getX() - rat.xo;
            double dz = rat.getZ() - rat.zo;
            boolean walking = rat.walkAnimation.speed(partialTick) > 0.015f
                    || dx * dx + dz * dz > 0.00001
                    || rat.getDeltaMovement().horizontalDistanceSqr() > 0.00001;
            animation = walking ? WALK : IDLE;
            animationTime = (rat.tickCount + partialTick) / 20.0f;
        }

        model.render(pose, output, packedLight, OverlayTexture.NO_OVERLAY,
                animation, animationTime);

        // The corpse-only Tab highlight uses the same final death pose.
        if (rat.isCorpse() && LootHighlightClient.isHeld()) {
            pose.pushPose();
            pose.scale(1.025f, 1.025f, 1.025f);
            VertexConsumer highlight = buffers.getBuffer(RenderType.entityTranslucent(HIGHLIGHT_TEXTURE));
            model.render(pose, highlight, FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    animation, animationTime, LOOT_HIGHLIGHT_COLOR);
            pose.popPose();
        }
        pose.popPose();
        super.render(rat, yaw, partialTick, pose, buffers, packedLight);
    }

    /** Convert a synced remaining-ticks countdown into forward-moving clip time. */
    private static float elapsed(int duration, int remaining, float partialTick) {
        return Math.max(0.0f, (duration - remaining + partialTick) / 20.0f);
    }
}
