package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.monster.GiantRatEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Routes an intentional tactical right-click on a nearby corpse through the
 * ordinary client-to-server entity interaction packet. The server owns its loot.
 * Other right-clicks still use the existing camera/orbit/radial-menu controls.
 */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class CorpseLootClient {
    private static final double REACH_SQR = 36.0;
    private static final double RAY_DISTANCE = 256.0;
    private static final double TACTICAL_FOV = 50.0;

    private CorpseLootClient() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClick(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || event.getAction() != GLFW.GLFW_PRESS
                || !TacticalCameraController.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null
                || mc.screen != null || !mc.isWindowActive()) return;

        GiantRatEntity corpse = corpseUnderCursor(mc);
        if (corpse == null || mc.player.distanceToSqr(corpse) > REACH_SQR) return;

        // The tactical camera normally turns right clicks into a radial menu.
        // A corpse instead opens its server-authoritative inventory directly.
        event.setCanceled(true);
        mc.gameMode.interact(mc.player, corpse, InteractionHand.MAIN_HAND);
    }

    private static GiantRatEntity corpseUnderCursor(Minecraft mc) {
        Camera camera = mc.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) return null;
        double width = mc.getWindow().getScreenWidth();
        double height = mc.getWindow().getScreenHeight();
        if (width <= 0 || height <= 0) return null;

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
        double maxDistance = block.getType() == HitResult.Type.MISS
                ? RAY_DISTANCE : start.distanceTo(block.getLocation());

        GiantRatEntity closest = null;
        AABB rayBounds = new AABB(start, end).inflate(1.0);
        for (GiantRatEntity candidate : mc.level.getEntitiesOfClass(
                GiantRatEntity.class, rayBounds, GiantRatEntity::isCorpse)) {
            var impact = candidate.getBoundingBox().inflate(candidate.getPickRadius()).clip(start, end);
            if (impact.isEmpty()) continue;
            double distance = start.distanceTo(impact.get());
            if (distance < maxDistance) {
                maxDistance = distance;
                closest = candidate;
            }
        }
        return closest;
    }
}
