package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.monster.GiantRatEntity;
import dev.cobbledeep.network.OpenCorpseLootPacket;
import dev.cobbledeep.network.RPGNetwork;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Click-to-loot walks to an adjacent block before asking the server for loot. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class CorpseLootClient {
    private static final double RAY_DISTANCE = 256.0;
    private static final double TACTICAL_FOV = 50.0;
    private static final double CORPSE_HALF_WIDTH = 0.95;
    private static final double CORPSE_HEIGHT = 0.85;

    /** The corpse selected by the player's most recent loot command. */
    private static GiantRatEntity pendingCorpse;
    /** Do not start pathfinding or send a loot packet while simulation is frozen. */
    private static boolean startAfterUnpause;

    private CorpseLootClient() {}

    private static boolean gameFrozen(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        return server != null && server.tickRateManager().isFrozen();
    }

    /** Cancel a pending loot order when another movement or combat action takes over. */
    static void cancel(Minecraft mc) {
        if (pendingCorpse == null) return;
        pendingCorpse = null;
        startAfterUnpause = false;
        TacticalPathMovement.stop(mc);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS || !TacticalCameraController.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();

        // Left-clicking the world issues a different movement order; right-clicking
        // somewhere else can begin a camera drag or open a different action wheel.
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            cancel(mc);
            return;
        }
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
        if (mc.player == null || mc.level == null || mc.screen != null || !mc.isWindowActive()) {
            cancel(mc);
            return;
        }

        GiantRatEntity corpse = corpseUnderCursor(mc);
        if (corpse == null) {
            cancel(mc);
            return;
        }

        // Consume the press before tactical-camera orbit/radial-menu handling.
        event.setCanceled(true);
        cancel(mc);
        if (mc.gameMode == null) return;
        TacticalCombatController.cancel(mc);
        pendingCorpse = corpse;

        // A frozen world cannot advance the walking route or service an entity
        // menu interaction. Remember the order and begin it after unpausing.
        if (gameFrozen(mc)) {
            startAfterUnpause = true;
            return;
        }
        beginLootOrder(mc, corpse);
    }

    /** Called only when the simulation is running. */
    private static void beginLootOrder(Minecraft mc, GiantRatEntity corpse) {
        if (corpse.canLootFrom(mc.player)) {
            pendingCorpse = null;
            requestLoot(corpse);
            return;
        }

        // Walk to the centre of a block directly beside the corpse rather than
        // stopping several blocks away or trying to stand inside the creature.
        Vec3 destination = adjacentBlockOnPlayerSide(mc, corpse);
        TacticalPathMovement.start(mc, destination);
        if (TacticalPathMovement.target() == null) pendingCorpse = null;
    }

    private static Vec3 adjacentBlockOnPlayerSide(Minecraft mc, GiantRatEntity corpse) {
        var corpseBlock = corpse.blockPosition();
        double deltaX = mc.player.getX() - corpse.getX();
        double deltaZ = mc.player.getZ() - corpse.getZ();
        int dx = 0;
        int dz = 0;
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            dx = deltaX >= 0.0 ? 1 : -1;
        } else {
            dz = deltaZ >= 0.0 ? 1 : -1;
        }
        return new Vec3(corpseBlock.getX() + dx + 0.5,
                corpse.getY(), corpseBlock.getZ() + dz + 0.5);
    }

    /** Manual keyboard movement takes precedence over an unfinished loot order. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMovementKey(InputEvent.Key event) {
        if (pendingCorpse == null || event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft mc = Minecraft.getInstance();
        for (var key : new net.minecraft.client.KeyMapping[] {
                mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft,
                mc.options.keyRight, mc.options.keyJump, mc.options.keyShift}) {
            if (key.matches(event.getKey(), event.getScanCode())) {
                cancel(mc);
                return;
            }
        }
    }

    /** Run after tactical camera/path movement so arrival is detected this tick. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        GiantRatEntity corpse = pendingCorpse;
        if (corpse == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (!TacticalCameraController.isEnabled() || mc.player == null || mc.level == null
                || mc.gameMode == null || !mc.player.isAlive() || mc.screen != null
                || !mc.isWindowActive() || corpse.level() != mc.level
                || corpse.isRemoved() || !corpse.isCorpse()) {
            cancel(mc);
            return;
        }

        // Retain the selected corpse while paused; an unstarted route has no
        // movement target yet and must not be mistaken for a failed route.
        if (gameFrozen(mc)) {
            TacticalPathMovement.suspendInputs(mc);
            return;
        }
        if (startAfterUnpause) {
            startAfterUnpause = false;
            beginLootOrder(mc, corpse);
            return;
        }
        if (corpse.canLootFrom(mc.player)) {
            // Clear before opening the menu: opening a screen ends walking.
            pendingCorpse = null;
            TacticalPathMovement.stop(mc);
            requestLoot(corpse);
            return;
        }
        // A replaced, failed or finished route must not leave an armed loot
        // request that opens later after unrelated player movement.
        if (TacticalPathMovement.target() == null) pendingCorpse = null;
    }

    private static void requestLoot(GiantRatEntity corpse) {
        RPGNetwork.CHANNEL.send(new OpenCorpseLootPacket(corpse.getId()),
                PacketDistributor.SERVER.noArg());
    }

    private static GiantRatEntity corpseUnderCursor(Minecraft mc) {
        Camera camera = mc.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) return null;
        double width = mc.getWindow().getScreenWidth();
        double height = mc.getWindow().getScreenHeight();
        if (width <= 0.0 || height <= 0.0) return null;

        double ndcX = 2.0 * mc.mouseHandler.xpos() / width - 1.0;
        double ndcY = 1.0 - 2.0 * mc.mouseHandler.ypos() / height;
        double tangent = Math.tan(Math.toRadians(TACTICAL_FOV / 2.0));
        Vec3 direction = new Vec3(camera.getLookVector())
                .add(new Vec3(camera.getLeftVector()).scale(-ndcX * tangent * width / height))
                .add(new Vec3(camera.getUpVector()).scale(ndcY * tangent)).normalize();
        Vec3 start = camera.getPosition();
        Vec3 end = start.add(direction.scale(RAY_DISTANCE));
        HitResult block = mc.level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        double closestDistance = block.getType() == HitResult.Type.MISS
                ? RAY_DISTANCE : start.distanceTo(block.getLocation());

        GiantRatEntity closest = null;
        AABB rayBounds = new AABB(start, end).inflate(1.5);
        for (GiantRatEntity candidate : mc.level.getEntitiesOfClass(
                GiantRatEntity.class, rayBounds, GiantRatEntity::isCorpse)) {
            AABB selection = new AABB(
                    candidate.getX() - CORPSE_HALF_WIDTH,
                    candidate.getY() - 0.05,
                    candidate.getZ() - CORPSE_HALF_WIDTH,
                    candidate.getX() + CORPSE_HALF_WIDTH,
                    candidate.getY() + CORPSE_HEIGHT,
                    candidate.getZ() + CORPSE_HALF_WIDTH);
            var intersection = selection.clip(start, end);
            if (intersection.isEmpty()) continue;
            double distance = start.distanceTo(intersection.get());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = candidate;
            }
        }
        return closest;
    }
}
