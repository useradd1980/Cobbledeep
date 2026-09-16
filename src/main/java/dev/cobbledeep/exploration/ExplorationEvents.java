package dev.cobbledeep.exploration;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = Cobbledeep.MODID)
public final class ExplorationEvents
{
    private record Scan(int x, int z, long tick) { }
    private static final Map<ServerPlayer, Scan> CURSORS = new WeakHashMap<>();

    private ExplorationEvents() { }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event)
    {
        if (!(event.player instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
        ExplorationData data = ExplorationData.get(player);
        int x = Mth.floor(player.getX()), z = Mth.floor(player.getZ());
        long tick = player.level().getGameTime();
        Scan previous = CURSORS.get(player);
        // Scan on block movement and once per second while still, so newly
        // received chunks join the record without forcing any chunk loads.
        if (previous == null || previous.x() != x || previous.z() != z
                || tick < previous.tick() || tick - previous.tick() >= 20)
        {
            TerrainRadius.visit(player.getX(), player.getZ(), (bx, bz) ->
            {
                if (player.level().hasChunkAt(new BlockPos(bx, player.getBlockY(), bz)))
                    data.discoverColumn(bx, bz, player.level().getMinBuildHeight(),
                            player.level().getMaxBuildHeight());
            });
            CURSORS.put(player, new Scan(x, z, tick));
        }
        ExplorationSync.tick(player, data);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        CURSORS.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        CURSORS.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        CURSORS.clear();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal("cobbledeep")
                .then(Commands.literal("exploration")
                        .then(Commands.literal("status").executes(context ->
                        {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ExplorationGrid grid = ExplorationData.get(player).grid();
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Explored " + grid.discoveredCount() + " cells in " + grid.sections().size()
                                            + " sections of " + player.serverLevel().dimension().location()
                                            + ". Cell size: 2x2x2 blocks; terrain radius: 24 horizontal blocks."), false);
                            return 1;
                        }))
                        .then(Commands.literal("check")
                                .then(Commands.argument("position", BlockPosArgument.blockPos()).executes(context ->
                                {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    BlockPos pos = BlockPosArgument.getBlockPos(context, "position");
                                    boolean explored = ExplorationData.get(player).grid()
                                            .isExplored(pos.getX(), pos.getY(), pos.getZ());
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            pos.toShortString() + ": " + (explored ? "explored" : "unexplored")), false);
                                    return explored ? 1 : 0;
                                })))));
    }
}
