package dev.cobbledeep.monster.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.monster.GiantRatRegistration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GiantRatClientEvents {
    private GiantRatClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GiantRatRegistration.GIANT_RAT.get(), GiantRatRenderer::new);
    }
}
