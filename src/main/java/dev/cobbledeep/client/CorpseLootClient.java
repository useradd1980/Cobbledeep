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

/** Click-to-loot uses the tactical movement system for distant corpses. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class CorpseLootClient {
    // The server allows six blocks; stop a little closer to allow for client/server lag.
    private static final double REACH_SQR = 25.0;
    private static final double RAY_DISTANCE = 256.0;
    private static final double TACTICAL_FOV = 50.0;
    private static final double CORPSE_HALF_WIDTH = 0.95;
    private static final double CORPSE_HEIGHT = 0.85;
    private static final double APPROACH_OFFSET = 3.0;

    /** The corpse selected by the player's most recent loot command. */
    private static GiantRatEntity pendingCorpse;

    private CorpseLootClient() {}

    /** Cancel a pending loot order when another movement or combat action takes over. */
    static void cancel(Minecraft mc) {
        if (pendingCorpse == null) return;
        pendingCorpse = null;
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
        if (mc.player.distanceToSqr(corpse) <= REACH_SQR) {
            requestLoot(corpse);
            return;
        }

        // Reuse the existing A* click-to-move controller. Aim at walkable ground
        // on the player's side of the body, rather than trying to stand inside it.
        // The tick handler opens the loot as soon as the player enters reach;
        // it does not require reaching the exact route endpoint.
        Vec3 away = mc.player.position().subtract(corpse.position());
        Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
        Vec3 direction = horizontal.lengthSqr() > 1.0e-6
                ? horizontal.normalize() : new Vec3(1.0, 0.0, 0.0);
        Vec3 destination = corpse.position().add(direction.scale(APPROACH_OFFSET));
        pendingCorpse = corpse;
        TacticalCombatController.cancel(mc);
        TacticalPathMovement.start(mc, destination);
        // start() clears its target on an invalid destination or a failed route.
        if (TacticalPathMovement.target() == null) pendingCorpse = null;
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
        if (mc.player.distanceToSqr(corpse) <= REACH_SQR) {
            // Clear before opening the menu: opening a screen ends walking.
            pendingCorpse = null;
            TacticalPathMovement.stop(mc);
            requestLoot(corpse);
            return;
        }
        // Another action or a failed/finished route must not leave an armed
        // loot request that opens unexpectedly after the player moves manually.
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
