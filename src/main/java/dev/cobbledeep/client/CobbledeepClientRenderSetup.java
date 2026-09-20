package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registers the player renderer and the tactical party GUI layer on the mod bus. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CobbledeepClientRenderSetup
{
    private CobbledeepClientRenderSetup() { }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event)
    {
        CobbledeepPlayerRenderEvents.initializeRenderers(event.getContext());
    }

    @SubscribeEvent
    public static void onAddGuiLayers(AddGuiOverlayLayersEvent event)
    {
        // Forge 1.21.1 uses LayeredDraw rather than the removed RenderGuiEvent.
        // Keep the portrait strip in the post-sleep HUD stack so it draws once
        // per frame and respects Minecraft's normal hide-GUI behaviour.
        event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK,
                ResourceLocation.fromNamespaceAndPath(Cobbledeep.MODID, "party_portrait_bar"),
                (graphics, partialTick) -> PartyPortraitBar.onHud(graphics));
    }
}
