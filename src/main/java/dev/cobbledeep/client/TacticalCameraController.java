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
import org.lwjgl.system.MemoryStack;

/**
 * Tactical-camera prototype.
 *
 * V toggles tactical mode. The mouse is released so it can act as a CRPG-style
 * cursor rather than steering the player's view. Left-clicking terrain sets a
 * movement destination. Comma/period rotate the camera in 90-degree steps, the
 * mouse wheel changes real camera distance, and moving the cursor to a screen
 * edge pans a free tactical camera focus point. Hold right mouse and drag
 * horizontally to orbit that point; a right-click without dragging cancels movement.
 *
 * Click-to-move follows a bounded, collision-checked route using normal player
 * movement. Open doorways, slabs and one-block steps are supported.
 */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalCameraController
{
    private static final float TACTICAL_PITCH = 45.0F;
    private static final float TACTICAL_FOV = 50.0F;
    private static final float ROTATION_STEP = 90.0F;
    private static final double DRAG_ROTATION_SENSITIVITY = 0.25;
    private static final double DRAG_ROTATION_THRESHOLD = 4.0;

    private static final float MIN_CAMERA_DISTANCE = 6.0F;
    private static final float MAX_CAMERA_DISTANCE = 48.0F;
    private static final float DEFAULT_CAMERA_DISTANCE = 14.0F;
    private static final float CAMERA_DISTANCE_STEP = 2.0F;

    private static final double CLICK_RAY_DISTANCE = 256.0;

    private static final double EDGE_PAN_MAX_SPEED = 0.42;

    private static final double TERRAIN_FOCUS_OFFSET = 1.6;
    private static final double TERRAIN_CAMERA_CLEARANCE = 1.0;
    private static final double TERRAIN_HEIGHT_RESPONSE = 6.0;

    private static boolean enabled;
    private static boolean gamePaused;
    private static float yaw = 45.0F;
    private static float cameraDistance = DEFAULT_CAMERA_DISTANCE;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;
    private static boolean previousViewBobbing;
    private static boolean rightMouseHeld;
    private static boolean rightMouseDragged;
    private static double rightMouseLastX;
    private static double rightMousePendingDelta;
    private static LocalPlayer cameraPlayer;

    // Tactical camera focus includes eye height captured only on activation or
    // explicit recenter. It is an absolute world-space point, independent from
    // the player's current position. This is what allows edge-panning to leave
    // the player off-centre while the character continues moving underneath it.
    private static Vec3 cameraFocus;
    private static Vec3 previousCameraFocus;
    private static double terrainFocusY = Double.NaN;
    private static long terrainFrameNanos;


    private TacticalCameraController() { }

    static boolean isEnabled()
    {
        return enabled;
    }

    static Vec3 getMovementTarget()
    {
        return TacticalPathMovement.markerTarget(Minecraft.getInstance());
    }

    static boolean isShowingArrivalMarker()
    {
        return TacticalPathMovement.showingArrivalMarker(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event)
    {
        Minecraft minecraft = Minecraft.getInstance();

        // A restored world creates a new LocalPlayer while tactical-camera
        // state remains alive on the client. Re-anchor immediately so the old
        // world's absolute focus point is never carried into the new snapshot.
        if (minecraft.player == null)
        {
            cameraPlayer = null;
        }
        else if (minecraft.player != cameraPlayer)
        {
            cameraPlayer = minecraft.player;
            if (!enabled)
                enableTacticalCamera(minecraft);
            else
                recenterCamera(cameraPlayer);
        }

        if (gamePaused && (minecraft.player == null || minecraft.level == null))
            gamePaused = false;

        while (TacticalCameraKeys.TOGGLE.consumeClick())
        {
            if (minecraft.player == null) return;

            resetCameraDrag();
            enabled = !enabled;
            if (enabled)
            {
                enableTacticalCamera(minecraft);
            }
            else
            {
                setGamePaused(minecraft, false);
                stopClickMovement(minecraft);
                cameraFocus = null;
                previousCameraFocus = cameraFocus;
                terrainFocusY = Double.NaN;
                minecraft.options.setCameraType(previousCameraType);
                minecraft.options.bobView().set(previousViewBobbing);
                if (minecraft.screen == null)
                {
                    minecraft.mouseHandler.grabMouse();
                }
            }
        }

        if (!enabled) { stopClickMovement(minecraft); return; }
        if (minecraft.screen != null || minecraft.player == null || !minecraft.isWindowActive())
        {
            resetCameraDrag();
        }

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
                recenterCamera(minecraft.player);
            }
        }

        while (TacticalCameraKeys.PLAY_PAUSE.consumeClick())
        {
            if (minecraft.player != null && minecraft.level != null
                    && minecraft.screen == null && minecraft.isWindowActive())
                setGamePaused(minecraft, !gamePaused);
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

        // Save the tick start even when no pan input is present, so stopping
        // settles at the target instead of replaying the previous movement.
        previousCameraFocus = cameraFocus;
        updateEdgePan(minecraft);
        if (!gamePaused) updateClickMovement(minecraft);
    }

    private static void enableTacticalCamera(Minecraft minecraft)
    {
        enabled = true;
        previousCameraType = minecraft.options.getCameraType();
        previousViewBobbing = minecraft.options.bobView().get();
        minecraft.options.bobView().set(false);
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        recenterCamera(minecraft.player);
        if (minecraft.screen == null)
            minecraft.mouseHandler.releaseMouse();
    }

    private static void recenterCamera(LocalPlayer player)
    {
        cameraFocus = player.position().add(0.0, player.getEyeHeight(), 0.0);
        previousCameraFocus = cameraFocus;
        terrainFocusY = Double.NaN;
        resetCameraDrag();
    }

    private static void setGamePaused(Minecraft minecraft, boolean paused)
    {
        if (gamePaused == paused) return;
        var server = minecraft.getSingleplayerServer();
        if (server == null) return;

        gamePaused = paused;
        TacticalPathMovement.suspendInputs(minecraft);
        minecraft.options.keyJump.setDown(false);
        server.execute(() -> server.tickRateManager().setFrozen(paused));
        if (minecraft.player != null)
            minecraft.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(paused ? "PAUSED" : "PLAYING"), true);
    }

    private static void updateEdgePan(Minecraft minecraft)
    {
        LocalPlayer player = minecraft.player;
        // Keep evaluating the cursor after it leaves a windowed game. GLFW
        // reports coordinates below zero or beyond the window dimensions, so
        // the camera can continue panning until the cursor moves back inside.
        if (player == null || minecraft.screen != null || rightMouseHeld) return;

        if (cameraFocus == null)
        {
            cameraFocus = player.position().add(0.0, player.getEyeHeight(), 0.0);
            previousCameraFocus = cameraFocus;
            terrainFocusY = Double.NaN;
        }

        double width = minecraft.getWindow().getScreenWidth();
        double height = minecraft.getWindow().getScreenHeight();
        if (width <= 0.0 || height <= 0.0) return;

        double mouseX;
        double mouseY;
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            var cursorX = stack.mallocDouble(1);
            var cursorY = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(minecraft.getWindow().getWindow(), cursorX, cursorY);
            mouseX = cursorX.get(0);
            mouseY = cursorY.get(0);
        }

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

        // Cap the combined vector so corner panning is not faster than panning
        // along a single screen edge.
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

        // GLFW cursor coordinates range from zero to one pixel short of the
        // window size. Do not pan merely near an edge: require the cursor to
        // reach the outermost pixel. Reaching a corner activates both axes.
        if (coordinate <= 0.0) return -1.0;
        if (coordinate >= size - 1.0) return 1.0;

        return 0.0;
    }

    private static void updateClickMovement(Minecraft minecraft)
    {
        TacticalPathMovement.tick(minecraft);
    }

    private static void stopClickMovement(Minecraft minecraft)
    {
        TacticalPathMovement.stop(minecraft);
    }

    @SubscribeEvent
    public static void onMovementKey(InputEvent.Key event)
    {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (TacticalCameraKeys.PLAY_PAUSE.matches(event.getKey(), event.getScanCode()))
        {
            // Forge's keyboard event is posted after vanilla updates matching
            // key bindings, but before the next movement tick. Remove Space's
            // simultaneous jump state immediately; PLAY_PAUSE's click remains
            // queued for the tactical controller to consume.
            mc.options.keyJump.setDown(false);
            return;
        }
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        if (TacticalPathMovement.markerTarget(mc) == null) return;
        for (var key : new net.minecraft.client.KeyMapping[] {mc.options.keyUp, mc.options.keyDown,
                mc.options.keyLeft, mc.options.keyRight, mc.options.keyJump, mc.options.keyShift})
            if (key.matches(event.getKey(), event.getScanCode()))
            {
                stopClickMovement(mc);
                key.setDown(true);
                break;
            }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event)
    {
        if (!enabled) return;
        Minecraft minecraft = Minecraft.getInstance();

        // Always clear the gesture on release, including releases over a menu.
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                && event.getAction() == GLFW.GLFW_RELEASE && rightMouseHeld)
        {
            boolean worldInput = minecraft.player != null && minecraft.level != null
                    && minecraft.screen == null && minecraft.isWindowActive();
            if (worldInput)
            {
                updateCameraDrag(minecraft);
                if (!rightMouseDragged) stopClickMovement(minecraft);
                event.setCanceled(true);
            }
            resetCameraDrag();
            return;
        }

        if (event.getAction() != GLFW.GLFW_PRESS || minecraft.player == null
                || minecraft.level == null || minecraft.screen != null || !minecraft.isWindowActive()) return;

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT)
        {
            if (!rightMouseHeld)
            {
                Vec3 target = raycastCursorToWorld(minecraft);
                if (target != null) TacticalPathMovement.start(minecraft, target);
            }
            event.setCanceled(true);
            minecraft.mouseHandler.releaseMouse();
        }
        else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
        {
            rightMouseHeld = true;
            rightMouseDragged = false;
            rightMousePendingDelta = 0.0;
            rightMouseLastX = minecraft.mouseHandler.xpos();
            event.setCanceled(true);
            minecraft.mouseHandler.releaseMouse();
        }
    }

    private static void resetCameraDrag()
    {
        rightMouseHeld = false;
        rightMouseDragged = false;
        rightMousePendingDelta = 0.0;
    }

    private static void updateCameraDrag(Minecraft minecraft)
    {
        if (!rightMouseHeld) return;
        if (minecraft.player == null || minecraft.level == null
                || minecraft.screen != null || !minecraft.isWindowActive())
        {
            resetCameraDrag();
            return;
        }

        double mouseX = minecraft.mouseHandler.xpos();
        double delta = mouseX - rightMouseLastX;
        rightMouseLastX = mouseX;
        if (!rightMouseDragged)
        {
            rightMousePendingDelta += delta;
            if (Math.abs(rightMousePendingDelta) <= DRAG_ROTATION_THRESHOLD) return;
            rightMouseDragged = true;
            // Discard only the click dead zone, avoiding a jump at drag onset.
            delta = rightMousePendingDelta
                    - Math.copySign(DRAG_ROTATION_THRESHOLD, rightMousePendingDelta);
            rightMousePendingDelta = 0.0;
        }
        yaw = Mth.wrapDegrees(yaw + (float)(delta * DRAG_ROTATION_SENSITIVITY));
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
            previousCameraFocus = cameraFocus;
            terrainFocusY = Double.NaN;
        }

        // Sample mouse motion each rendered frame, not at the 20 Hz game tick.
        updateCameraDrag(Minecraft.getInstance());
        event.setYaw(yaw);
        event.setPitch(TACTICAL_PITCH);
        event.setRoll(0.0F);

        // This hook runs after vanilla Camera.setup(), before frustum/world
        // rendering. Replace its player-relative position outright; never try
        // to undo vanilla's interpolated/collision-adjusted third-person offset.
        // Use our requested angles, not the camera's still-vanilla look vector.
        Vec3 forward = Vec3.directionFromRotation(TACTICAL_PITCH, yaw);
        // Render between tick positions instead of displaying 20 discrete
        // movements per second. Terrain sampling must use this same position.
        double partialTick = Mth.clamp(event.getPartialTick(), 0.0, 1.0);
        Vec3 renderedFocus = previousCameraFocus == null ? cameraFocus
                : previousCameraFocus.lerp(cameraFocus, partialTick);
        Vec3 position = terrainAdjustedFocus(Minecraft.getInstance(), forward, renderedFocus)
                .subtract(forward.scale(cameraDistance));
        event.getCamera().setPosition(position.x, position.y, position.z);
    }

    /**
     * Outdoor surface tracking. X/Z remain controlled only by pan/recenter.
     * Client heightmaps bound the leaf-filtering scan; missing chunks are skipped.
     */
    private static Vec3 terrainAdjustedFocus(Minecraft minecraft, Vec3 forward, Vec3 renderedFocus)
    {
        long now = System.nanoTime();
        if (Double.isNaN(terrainFocusY))
        {
            terrainFocusY = renderedFocus.y;
            terrainFrameNanos = now;
        }
        double elapsed = Math.min(0.1, Math.max(0.0, (now - terrainFrameNanos) / 1.0e9));
        terrainFrameNanos = now;

        if (minecraft.level == null || minecraft.isPaused())
        {
            return new Vec3(renderedFocus.x, terrainFocusY, renderedFocus.z);
        }

        double ground = interpolatedSurfaceHeight(minecraft, renderedFocus.x, renderedFocus.z);
        // Unknown/empty columns preserve altitude, rather than dropping toward
        // the world's minimum build height at the edge of loaded terrain.
        double target = Double.isNaN(ground) ? terrainFocusY : ground + TERRAIN_FOCUS_OFFSET;

        double cameraX = renderedFocus.x - forward.x * cameraDistance;
        double cameraZ = renderedFocus.z - forward.z * cameraDistance;
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
        return new Vec3(renderedFocus.x, terrainFocusY, renderedFocus.z);
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
