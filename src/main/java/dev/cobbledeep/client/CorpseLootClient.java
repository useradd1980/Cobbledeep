package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.monster.GiantRatEntity;
import dev.cobbledeep.network.OpenCorpseLootPacket;
import dev.cobbledeep.network.RPGNetwork;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Routes tactical-camera clicks on persistent rat corpses to the server.
 * The rendered mesh is substantially larger than the live entity's collision
 * box, so corpse selection uses a separate, visual-sized selection volume.
 */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class CorpseLootClient {
    private static final double REACH_SQR = 36.0;
    private static final double RAY_DISTANCE = 256.0;
    private static final double TACTICAL_FOV = 50.0;
    private static final double CORPSE_HALF_WIDTH = 0.95;
    private static final double CORPSE_HEIGHT = 0.85;

    private CorpseLootClient() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClick(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || event.getAction() != GLFW.GLFW_PRESS
                || !TacticalCameraController.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null
                || !mc.isWindowActive()) return;

        GiantRatEntity corpse = corpseUnderCursor(mc);
        if (corpse == null) return;

        // Consume the click before the tactical camera starts a right-drag or
        // opens the living-creature radial menu.
        event.setCanceled(true);
        if (mc.player.distanceToSqr(corpse) > REACH_SQR) {
            mc.player.displayClientMessage(Component.literal("Move closer to loot the corpse"), true);
            return;
        }
        // Vanilla's entity interaction path is unreliable for an entity that
        // has zero health. Send only its entity id; the server independently
        // validates that it is a nearby corpse before opening its inventory.
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
        // Search nearby entities, then ray-test their enlarged corpse selection
        // volume rather than their tiny, live-rat collision box.
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
