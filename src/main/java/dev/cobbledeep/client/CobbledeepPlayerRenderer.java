package dev.cobbledeep.client;

import dev.cobbledeep.character.CharacterCapabilities;
import dev.cobbledeep.client.screen.AppearanceSkinTexture;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Vanilla-compatible player renderer that substitutes Cobbledeep's generated
 * appearance texture when the player has a completed Cobbledeep character.
 *
 * All of PlayerRenderer's normal animation, armor, held-item and render-layer
 * behavior is retained; only the base player texture is replaced here.
 */
public final class CobbledeepPlayerRenderer extends PlayerRenderer
{
    public CobbledeepPlayerRenderer(EntityRendererProvider.Context context, boolean slim)
    {
        super(context, slim);
        addLayer(new CobbledeepAppearanceRenderLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer player)
    {
        ResourceLocation vanillaTexture = super.getTextureLocation(player);
        ResourceLocation[] result = { vanillaTexture };

        player.getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresent(data ->
        {
            if (data.isCharacterCreated())
            {
                result[0] = AppearanceSkinTexture.get(data.getAppearance());
            }
        });

        return result[0];
    }
}
