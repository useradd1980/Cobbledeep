package dev.cobbledeep.exploration;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = Cobbledeep.MODID)
public final class ExplorationEvents
{
    private static final int RAYS_PER_TICK = 96;
    private static final List<BlockPos> OFFSETS = offsets();
    private static final Map<ServerPlayer, Integer> CURSORS = new WeakHashMap<>();

    private ExplorationEvents() { }

    private static List<BlockPos> offsets()
    {
        List<BlockPos> result = new ArrayList<>();
        int radius = (int) CharacterSight.RANGE / ExplorationGrid.CELL_SIZE;
        for (int x = -radius; x <= radius; x++)
            for (int y = -radius; y <= radius; y++)
                for (int z = -radius; z <= radius; z++)
                    if (x * x + y * y + z * z <= radius * radius)
                        result.add(new BlockPos(x, y, z));
        result.sort(Comparator.comparingDouble(p -> p.getX() * p.getX()
                + p.getY() * p.getY() + p.getZ() * p.getZ()));
        return List.copyOf(result);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event)
    {
        if (!(event.player instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
        ExplorationData data = ExplorationData.get(player);
        Vec3 eye = player.getEyePosition();
        int size = ExplorationGrid.CELL_SIZE;
        int baseX = Math.floorDiv(Mth.floor(eye.x), size) * size;
        int baseY = Math.floorDiv(Mth.floor(eye.y), size) * size;
        int baseZ = Math.floorDiv(Mth.floor(eye.z), size) * size;
        int cursor = CURSORS.getOrDefault(player, 0);
        // The bounded scan continues even while stationary, so opening a door
        // reveals newly exposed cells. No player/camera movement is required.
        for (int i = 0; i < RAYS_PER_TICK; i++)
        {
            BlockPos offset = OFFSETS.get(cursor);
            cursor = (cursor + 1) % OFFSETS.size();
            Vec3 end = new Vec3(baseX + offset.getX() * size + size * 0.5,
                    baseY + offset.getY() * size + size * 0.5,
                    baseZ + offset.getZ() * size + size * 0.5);
            if (end.y < player.level().getMinBuildHeight() || end.y >= player.level().getMaxBuildHeight()) continue;
            BlockHitResult hit = CharacterSight.trace(player, eye, end);
            if (hit == null) continue;
            Vec3 visibleEnd = hit.getLocation();
            // Mark only the unobstructed segment. Never continue through a wall
            // or use the terrain heightmap to infer discovery underground.
            int steps = Math.max(1, (int) Math.ceil(eye.distanceTo(visibleEnd) / 0.75));
            for (int step = 0; step <= steps; step++)
            {
                Vec3 point = eye.lerp(visibleEnd, (double) step / steps);
                data.discover(Mth.floor(point.x), Mth.floor(point.y), Mth.floor(point.z));
            }
            if (hit.getType() == HitResult.Type.BLOCK)
            {
                BlockPos surface = hit.getBlockPos();
                data.discover(surface.getX(), surface.getY(), surface.getZ());
            }
        }
        CURSORS.put(player, cursor);
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
                                            + ". Cell size: 2x2x2 blocks; sight range: 24 blocks."), false);
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
