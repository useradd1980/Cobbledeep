package dev.cobbledeep.exploration;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.network.RPGNetwork;
import dev.cobbledeep.network.SyncExplorationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.WeakHashMap;

/** Bounded snapshots/deltas; exploration remains server-owned. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID)
public final class ExplorationSync
{
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    private static final class State
    {
        final ResourceLocation dimension;
        final LinkedHashSet<ExplorationGrid.Section> pending = new LinkedHashSet<>();
        boolean reset = true;
        State(ServerPlayer player, ExplorationData data)
        {
            dimension = player.serverLevel().dimension().location();
            pending.addAll(data.grid().sections());
        }
    }

    public static void tick(ServerPlayer player, ExplorationData data)
    {
        State state = STATES.get(player);
        if (state == null || !state.dimension.equals(player.serverLevel().dimension().location()))
        {
            state = new State(player, data);
            STATES.put(player, state);
        }
        state.pending.addAll(data.drainSync());
        if (!state.reset && state.pending.isEmpty()) return;
        var entries = new ArrayList<SyncExplorationPacket.SectionData>();
        var iterator = state.pending.iterator();
        while (iterator.hasNext() && entries.size() < SyncExplorationPacket.MAX_SECTIONS)
        {
            var key = iterator.next();
            entries.add(new SyncExplorationPacket.SectionData(key, data.grid().sectionBits(key)));
            iterator.remove();
        }
        RPGNetwork.CHANNEL.send(new SyncExplorationPacket(state.dimension, state.reset, entries),
                PacketDistributor.PLAYER.with(player));
        state.reset = false;
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { STATES.remove(event.getEntity()); }
    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { STATES.remove(event.getEntity()); }
    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { STATES.remove(event.getEntity()); }
    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { STATES.clear(); }
}
