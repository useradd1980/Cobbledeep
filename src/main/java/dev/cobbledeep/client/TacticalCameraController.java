package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Tactical-camera prototype.
 *
 * V toggles tactical mode. The mouse is released so it can act as a CRPG-style
 * cursor rather than steering the player's view. Left-clicking terrain sets a
 * movement destination. Comma/period rotate the camera in 90-degree steps, the
 * mouse wheel changes real camera distance, and moving the cursor to a screen
 * edge pans a free tactical camera focus point.
 *
 * Click-to-move is intentionally simple at this stage: it walks directly toward
 * the selected point using normal player movement/collision. Pathfinding around
 * obstacles will be layered on later.
 */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalCameraController
{
    private static final float TACTICAL_PITCH = 45.0F;
    private static final float TACTICAL_FOV = 50.0F;
    private static final float ROTATION_STEP = 90.0F;

    private static final float MIN_CAMERA_DISTANCE = 6.0F;
    private static final float MAX_CAMERA_DISTANCE = 48.0F;
    private static final float DEFAULT_CAMERA_DISTANCE = 14.0F;
    private static final float CAMERA_DISTANCE_STEP = 2.0F;

    private static final double MOVE_STOP_DISTANCE = 0.45;
    private static final double CLICK_RAY_DISTANCE = 256.0;

    private static final double EDGE_PAN_ZONE_FRACTION = 1.0 / 8.0;
    private static final double EDGE_PAN_MAX_SPEED = 0.42;

    private static final double TERRAIN_FOCUS_OFFSET = 1.6;
    private static final double TERRAIN_CAMERA_CLEARANCE = 1.0;
    private static final double TERRAIN_HEIGHT_RESPONSE = 6.0;

    private static boolean enabled;
    private static float yaw = 45.0F;
    private static float cameraDistance = DEFAULT_CAMERA_DISTANCE;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;
    private static boolean previousViewBobbing;
    private static Vec3 movementTarget;
    private static boolean clickMoveForwardHeld;

    // Tactical camera focus includes eye height captured only on activation or
    // explicit recenter. It is an absolute world-space point, independent from
    // the player's current position. This is what allows edge-panning to leave
    // the player off-centre while the character continues moving underneath it.
    private static Vec3 cameraFocus;
    private static double terrainFocusY = Double.NaN;
    private static long terrainFrameNanos;


    private TacticalCameraController() { }

    static boolean isEnabled()
    {
        return enabled;
    }

    static Vec3 getMovementTarget()
    {
        return movementTarget;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event)
    {
        Minecraft minecraft = Minecraft.getInstance();

        while (TacticalCameraKeys.TOGGLE.consumeClick())
        {
            if (minecraft.player == null) return;

            enabled = !enabled;
            if (enabled)
            {
                previousCameraType = minecraft.options.getCameraType();
                previousViewBobbing = minecraft.options.bobView().get();
                minecraft.options.bobView().set(false);
                minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                cameraFocus = minecraft.player.position().add(0.0, minecraft.player.getEyeHeight(), 0.0);
                terrainFocusY = Double.NaN;
                if (minecraft.screen == null)
                {
                    minecraft.mouseHandler.releaseMouse();
                }
            }
            else
            {
                stopClickMovement(minecraft);
                cameraFocus = null;
                terrainFocusY = Double.NaN;
                minecraft.options.setCameraType(previousCameraType);
                minecraft.options.bobView().set(previousViewBobbing);
                if (minecraft.screen == null)
                {
                    minecraft.mouseHandler.grabMouse();
                }
            }
        }

        if (!enabled) return;

        while (TacticalCameraKeys.ROTATE_LEFT.consumeClick())
        {
            yaw = Mth.wrapDegrees(yaw - ROTATION_STEP);
        }

        while (TacticalCameraKeys.ROTATE_RIGHT.consumeClick())
        {
            yaw = Mth.wrapDegrees(yaw + ROTATION_STEP);
        }

        while (TacticalCameraKeys.RECENTER.consumeClick())
        {
            if (minecraft.player != null)
            {
                cameraFocus = minecraft.player.position().add(0.0, minecraft.player.getEyeHeight(), 0.0);
                terrainFocusY = Double.NaN;
            }
        }

        if (minecraft.player != null && minecraft.options.getCameraType() != CameraType.THIRD_PERSON_BACK)
        {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        // Tactical mode uses a visible/free cursor. Vanilla will try to grab the
        // mouse whenever the world is clicked, so release it again immediately.
        if (minecraft.screen == null && minecraft.mouseHandler.isMouseGrabbed())
        {
            minecraft.mouseHandler.releaseMouse();
        }

        updateEdgePan(minecraft);
        updateClickMovement(minecraft);
    }

    private static void updateEdgePan(Minecraft minecraft)
    {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) return;

        if (cameraFocus == null)
        {
            cameraFocus = player.position().add(0.0, player.getEyeHeight(), 0.0);
            terrainFocusY = Double.NaN;
        }

        double width = minecraft.getWindow().getScreenWidth();
        double height = minecraft.getWindow().getScreenHeight();
        if (width <= 0.0 || height <= 0.0) return;

        double mouseX = minecraft.mouseHandler.xpos();
        double mouseY = minecraft.mouseHandler.ypos();

        double horizontal = edgeStrength(mouseX, width);
        double vertical = edgeStrength(mouseY, height);

        if (horizontal == 0.0 && vertical == 0.0) return;

        // Camera yaw defines screen-space directions on the X/Z plane.
        double radians = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
        // Screen-right is forward cross world-up; the opposite points left.
        Vec3 right = new Vec3(-Math.cos(radians), 0.0, -Math.sin(radians));

        // Screen top means move the focus forward into the scene. Screen bottom
        // moves backward; left/right map directly to their screen directions.
        Vec3 pan = right.scale(horizontal)
                .add(forward.scale(-vertical));

        // Preserve each axis's gradual ramp from zero, including when entering
        // a corner zone. Cap the combined vector so diagonals are not faster.
        if (pan.lengthSqr() > 1.0)
        {
            pan = pan.normalize();
        }
        // Scale world-space movement with camera distance so the apparent
        // screen-space panning speed stays useful as more terrain becomes visible.
        // Keep the original speed at the default distance and at closer zooms.
        double zoomSpeedMultiplier = Math.max(1.0, cameraDistance / DEFAULT_CAMERA_DISTANCE);
        cameraFocus = cameraFocus.add(pan.scale(EDGE_PAN_MAX_SPEED * zoomSpeedMultiplier));
    }

    private static double edgeStrength(double coordinate, double size)
    {
        if (size <= 0.0) return 0.0;

        // Use the width for horizontal edges and height for vertical edges.
        // Their overlap creates generous diagonal zones at all four corners.
        double margin = size * EDGE_PAN_ZONE_FRACTION;
        if (coordinate < margin)
        {
            return -Mth.clamp((margin - coordinate) / margin, 0.0, 1.0);
        }

        double farEdge = size - margin;
        if (coordinate > farEdge)
        {
            return Mth.clamp((coordinate - farEdge) / margin, 0.0, 1.0);
        }

        return 0.0;
    }

    private static void updateClickMovement(Minecraft minecraft)
    {
        LocalPlayer player = minecraft.player;
        if (player == null || movementTarget == null)
        {
            setClickMoveForward(minecraft, false);
            return;
        }

        double dx = movementTarget.x - player.getX();
        double dz = movementTarget.z - player.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        if (horizontalDistance <= MOVE_STOP_DISTANCE)
        {
            stopClickMovement(minecraft);
            return;
        }

        float movementYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
        player.setYRot(movementYaw);
        setClickMoveForward(minecraft, true);
    }

    private static void setClickMoveForward(Minecraft minecraft, boolean down)
    {
        if (clickMoveForwardHeld == down) return;
        minecraft.options.keyUp.setDown(down);
        clickMoveForwardHeld = down;
    }

    private static void stopClickMovement(Minecraft minecraft)
    {
        movementTarget = null;
        setClickMoveForward(minecraft, false);
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event)
    {
        if (!enabled || event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) return;

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT)
        {
            Vec3 target = raycastCursorToWorld(minecraft);
            if (target != null)
            {
                movementTarget = target;
            }

            // Left-click is movement in tactical mode, not attack/break-block.
            event.setCanceled(true);
            minecraft.mouseHandler.releaseMouse();
        }
        else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
        {
            // Reserve right-click for the later CRPG context action system. For
            // now it simply cancels an active movement order.
            stopClickMovement(minecraft);
            event.setCanceled(true);
            minecraft.mouseHandler.releaseMouse();
        }
    }

    @SubscribeEvent
    public static void onBlockHighlight(RenderHighlightEvent.Block event)
    {
        if (enabled)
        {
            // Vanilla still computes its normal centered crosshair target even
            // though tactical mode uses a free cursor and its own raycast. That
            // produces a stray block-selection wireframe near the player.
            event.setCanceled(true);
        }
    }

    private static Vec3 raycastCursorToWorld(Minecraft minecraft)
    {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) return null;

        double width = minecraft.getWindow().getScreenWidth();
        double height = minecraft.getWindow().getScreenHeight();
        if (width <= 0.0 || height <= 0.0) return null;

        double mouseX = minecraft.mouseHandler.xpos();
        double mouseY = minecraft.mouseHandler.ypos();
        double ndcX = (mouseX / width) * 2.0 - 1.0;
        double ndcY = 1.0 - (mouseY / height) * 2.0;
        double aspect = width / height;
        double tanHalfFov = Math.tan(Math.toRadians(TACTICAL_FOV * 0.5));

        Vec3 forward = new Vec3(camera.getLookVector());
        Vec3 left = new Vec3(camera.getLeftVector());
        Vec3 up = new Vec3(camera.getUpVector());

        Vec3 direction = forward
                .add(left.scale(-ndcX * tanHalfFov * aspect))
                .add(up.scale(ndcY * tanHalfFov))
                .normalize();

        Vec3 start = camera.getPosition();
        Vec3 end = start.add(direction.scale(CLICK_RAY_DISTANCE));
        HitResult hit = minecraft.level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                minecraft.player));

        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK)
        {
            return blockHit.getLocation();
        }

        return null;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event)
    {
        if (!enabled) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (cameraFocus == null)
        {
            cameraFocus = player.position().add(0.0, player.getEyeHeight(), 0.0);
            terrainFocusY = Double.NaN;
        }

        event.setYaw(yaw);
        event.setPitch(TACTICAL_PITCH);
        event.setRoll(0.0F);

        // This hook runs after vanilla Camera.setup(), before frustum/world
        // rendering. Replace its player-relative position outright; never try
        // to undo vanilla's interpolated/collision-adjusted third-person offset.
        // Use our requested angles, not the camera's still-vanilla look vector.
        Vec3 forward = Vec3.directionFromRotation(TACTICAL_PITCH, yaw);
        Vec3 position = terrainAdjustedFocus(Minecraft.getInstance(), forward)
                .subtract(forward.scale(cameraDistance));
        event.getCamera().setPosition(position.x, position.y, position.z);
    }

    /**
     * Outdoor surface tracking. X/Z remain controlled only by pan/recenter.
     * Client heightmaps bound the leaf-filtering scan; missing chunks are skipped.
     */
    private static Vec3 terrainAdjustedFocus(Minecraft minecraft, Vec3 forward)
    {
        long now = System.nanoTime();
        if (Double.isNaN(terrainFocusY))
        {
            terrainFocusY = cameraFocus.y;
            terrainFrameNanos = now;
        }
        double elapsed = Math.min(0.1, Math.max(0.0, (now - terrainFrameNanos) / 1.0e9));
        terrainFrameNanos = now;

        if (minecraft.level == null || minecraft.isPaused())
        {
            return new Vec3(cameraFocus.x, terrainFocusY, cameraFocus.z);
        }

        double ground = interpolatedSurfaceHeight(minecraft, cameraFocus.x, cameraFocus.z);
        // Unknown/empty columns preserve altitude, rather than dropping toward
        // the world's minimum build height at the edge of loaded terrain.
        double target = Double.isNaN(ground) ? terrainFocusY : ground + TERRAIN_FOCUS_OFFSET;

        double cameraX = cameraFocus.x - forward.x * cameraDistance;
        double cameraZ = cameraFocus.z - forward.z * cameraDistance;
        double cameraLift = -forward.y * cameraDistance;
        double minimumFocusY = Double.NEGATIVE_INFINITY;
        // Cover the camera's immediate footprint, including block boundaries.
        for (int ix = -1; ix <= 1; ix += 2)
        {
            for (int iz = -1; iz <= 1; iz += 2)
            {
                double surface = surfaceHeight(minecraft, Mth.floor(cameraX + ix * 0.5), Mth.floor(cameraZ + iz * 0.5));
                if (!Double.isNaN(surface))
                {
                    minimumFocusY = Math.max(minimumFocusY,
                            surface + TERRAIN_CAMERA_CLEARANCE - cameraLift);
                }
            }
        }
        target = Math.max(target, minimumFocusY);
        // Exponential smoothing gives the same response at different frame rates.
        terrainFocusY += (target - terrainFocusY) * (1.0 - Math.exp(-TERRAIN_HEIGHT_RESPONSE * elapsed));
        // At abrupt cliffs, rotation or zoom changes, clearance takes precedence
        // over smoothing. Descending still eases down instead of snapping.
        terrainFocusY = Math.max(terrainFocusY, minimumFocusY);
        return new Vec3(cameraFocus.x, terrainFocusY, cameraFocus.z);
    }

    private static double interpolatedSurfaceHeight(Minecraft minecraft, double x, double z)
    {
        // Treat column heights as samples at block centres to avoid stepwise
        // height targets when crossing from one block to the next.
        int x0 = Mth.floor(x - 0.5);
        int z0 = Mth.floor(z - 0.5);
        double tx = x - 0.5 - x0;
        double tz = z - 0.5 - z0;
        double h00 = surfaceHeight(minecraft, x0, z0);
        double h10 = surfaceHeight(minecraft, x0 + 1, z0);
        double h01 = surfaceHeight(minecraft, x0, z0 + 1);
        double h11 = surfaceHeight(minecraft, x0 + 1, z0 + 1);
        if (Double.isNaN(h00) || Double.isNaN(h10) || Double.isNaN(h01) || Double.isNaN(h11))
        {
            return Double.NaN;
        }
        return Mth.lerp(tz, Mth.lerp(tx, h00, h10), Mth.lerp(tx, h01, h11));
    }

    private static double surfaceHeight(Minecraft minecraft, int x, int z)
    {
        if (minecraft.level == null || !minecraft.level.hasChunkAt(new BlockPos(x, 0, z)))
        {
            return Double.NaN;
        }
        // MOTION_BLOCKING is synchronized to clients. NO_LEAVES is server-only:
        // its pre-created client heightmap can remain empty after chunk loading.
        // Start at the synchronized surface and skip leaves locally instead.
        int top = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        int bottom = minecraft.level.getMinBuildHeight();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = top - 1; y >= bottom; y--)
        {
            cursor.set(x, y, z);
            var state = minecraft.level.getBlockState(cursor);
            if (!state.is(BlockTags.LEAVES)
                    && (state.blocksMotion() || !state.getFluidState().isEmpty()))
            {
                // Heightmaps use the first free Y above the surface block.
                return y + 1.0;
            }
        }
        // Empty columns retain the existing altitude.
        return Double.NaN;
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event)
    {
        if (enabled)
        {
            // FOV can be computed multiple times per frame. It must not move
            // the camera or depend on whether another FOV query already ran.
            event.setFOV(TACTICAL_FOV);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event)
    {
        if (!enabled) return;

        if (event.getDeltaY() > 0.0)
        {
            cameraDistance = Math.max(MIN_CAMERA_DISTANCE, cameraDistance - CAMERA_DISTANCE_STEP);
        }
        else if (event.getDeltaY() < 0.0)
        {
            cameraDistance = Math.min(MAX_CAMERA_DISTANCE, cameraDistance + CAMERA_DISTANCE_STEP);
        }
        else
        {
            return;
        }

        event.setCanceled(true);
    }
}
