package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.exploration.ExplorationGrid;
import dev.cobbledeep.network.SyncExplorationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class ClientExploration
{
    private static ResourceLocation dimension;
    private static ExplorationGrid grid = new ExplorationGrid();

    public static void accept(ResourceLocation incoming, boolean reset,
                              List<SyncExplorationPacket.SectionData> sections)
    {
        if (reset)
        {
            grid = new ExplorationGrid();
            dimension = incoming;
        }
        if (!incoming.equals(dimension)) return;
        for (var section : sections) grid.restoreSection(section.key(), section.words());
    }

    public static ExplorationGrid grid(ResourceLocation current)
    {
        return current.equals(dimension) ? grid : null;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        dimension = null;
        grid = new ExplorationGrid();
        TacticalFogRenderer.releaseTextures();
    }
}
