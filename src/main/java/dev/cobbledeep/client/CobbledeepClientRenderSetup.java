package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Captures Forge's renderer context once player renderers are available. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CobbledeepClientRenderSetup
{
    private CobbledeepClientRenderSetup() { }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event)
    {
        CobbledeepPlayerRenderEvents.initializeRenderers(event.getContext());
    }
}
