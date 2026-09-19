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

/** Visual prototype: render the authored Blockbench polygon geometry and idle/walk keyframes. */
public final class GiantRatRenderer extends EntityRenderer<GiantRatEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "cobbledeep", "textures/entity/giant_rat.png");
    private static final ResourceLocation GEOMETRY = ResourceLocation.fromNamespaceAndPath(
            "cobbledeep", "models/entity/giant_rat.json");
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
        boolean walking = rat.getDeltaMovement().horizontalDistanceSqr() > 0.0004;
        String animation = walking ? "animation.giant_rat.walk" : "animation.giant_rat_movements";
        model.render(pose, output, packedLight, OverlayTexture.NO_OVERLAY, animation,
                (rat.tickCount + partialTick) / 20.0f);
        pose.popPose();
        super.render(rat, yaw, partialTick, pose, buffers, packedLight);
    }
}
