package dev.cobbledeep.client;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.cobbledeep.character.CharacterCapabilities;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Renders Cobbledeep's extra player geometry while vanilla's player model
 * transforms are still active. This keeps the custom head/body pieces aligned
 * with the animated player instead of drawing them later at world origin.
 */
final class CobbledeepAppearanceRenderLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
    CobbledeepAppearanceRenderLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent)
    {
        super(parent);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch)
    {
        player.getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresent(data ->
        {
            if (!data.isCharacterCreated() || data.getRace() == null) return;

            CobbledeepPlayerGeometry.render(
                    data,
                    getParentModel(),
                    poseStack,
                    bufferSource,
                    packedLight);
        });
    }
}
