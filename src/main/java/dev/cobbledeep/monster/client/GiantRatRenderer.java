package dev.cobbledeep.monster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cobbledeep.monster.GiantRatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/** Renders the authored polygon meshes and selects locomotion/death animation states. */
public final class GiantRatRenderer extends EntityRenderer<GiantRatEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "cobbledeep", "textures/entity/giant_rat.png");
    private static final ResourceLocation GEOMETRY = ResourceLocation.fromNamespaceAndPath(
            "cobbledeep", "models/entity/giant_rat.json");
    private static final String WALK = "animation.giant_rat.walk";
    private static final String IDLE = "animation.giant_rat_movements";
    private static final String DEATH = "animation.giant_rat_death";
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
        if (model == null) {
            model = RatMeshModel.load(Minecraft.getInstance().getResourceManager(), GEOMETRY);
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
            // This clip is non-looping: the mesh loader clamps time at its final frame.
            // Death progress is synced and the last frame is restored when a chunk reloads.
            animation = DEATH;
            animationTime = (rat.getDeathAnimationTicks() + partialTick) / 20.0f;
        } else {
            // Client interpolation often makes getDeltaMovement() appear to be zero for a
            // walking mob. Use the built-in limb animation and observed positional change too.
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
        pose.popPose();
        super.render(rat, yaw, partialTick, pose, buffers, packedLight);
    }
}
