package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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

    private static final float VANILLA_THIRD_PERSON_DISTANCE = 4.0F;
    private static final float MIN_CAMERA_DISTANCE = 6.0F;
    private static final float MAX_CAMERA_DISTANCE = 48.0F;
    private static final float CAMERA_DISTANCE_STEP = 2.0F;

    private static final double MOVE_STOP_DISTANCE = 0.45;
    private static final double CLICK_RAY_DISTANCE = 256.0;

    private static final double EDGE_PAN_MARGIN = 28.0;
    private static final double EDGE_PAN_MIN_SPEED = 0.08;
    private static final double EDGE_PAN_MAX_SPEED = 0.42;

    private static boolean enabled;
    private static float yaw = 45.0F;
    private static float cameraDistance = 14.0F;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;
    private static Vec3 movementTarget;
    private static boolean clickMoveForwardHeld;

    // Tactical camera focus is independent from the player's current position.
    // This is what allows edge-panning to leave the player off-centre.
    private static Vec3 cameraFocus;

    private static boolean renderRotationOverridden;
    private static boolean extraCameraDistanceApplied;
    private static float savedYaw;
    private static float savedPitch;
    private static float savedYawOld;
    private static float savedPitchOld;

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
                minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                cameraFocus = minecraft.player.position();
                if (minecraft.screen == null)
                {
                    minecraft.mouseHandler.releaseMouse();
                }
            }
            else
            {
                stopClickMovement(minecraft);
                cameraFocus = null;
                minecraft.options.setCameraType(previousCameraType);
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
                cameraFocus = minecraft.player.position();
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
            cameraFocus = player.position();
        }

        double width = minecraft.getWindow().getScreenWidth();
        double height = minecraft.getWindow().getScreenHeight();
        if (width <= 0.0 || height <= 0.0) return;

        double mouseX = minecraft.mouseHandler.xpos();
        double mouseY = minecraft.mouseHandler.ypos();

        double horizontal = edgeStrength(mouseX, width);
        double vertical = edgeStrength(mouseY, height);

        if (horizontal == 0.0 && vertical == 0.0) return;

        double magnitude = Math.min(1.0, Math.sqrt(horizontal * horizontal + vertical * vertical));
        double speed = Mth.lerp(magnitude, EDGE_PAN_MIN_SPEED, EDGE_PAN_MAX_SPEED);

        // Camera yaw defines screen-space directions on the X/Z plane.
        double radians = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
        Vec3 right = new Vec3(Math.cos(radians), 0.0, Math.sin(radians));

        // Screen top means move the focus forward into the scene. Screen bottom
        // moves backward; left/right map directly to their screen directions.
        Vec3 pan = right.scale(horizontal)
                .add(forward.scale(-vertical));

        if (pan.lengthSqr() > 1.0e-8)
        {
            pan = pan.normalize().scale(speed);
            cameraFocus = cameraFocus.add(pan);
        }
    }

    private static double edgeStrength(double coordinate, double size)
    {
        if (coordinate < EDGE_PAN_MARGIN)
        {
            return -Mth.clamp((EDGE_PAN_MARGIN - coordinate) / EDGE_PAN_MARGIN, 0.0, 1.0);
        }

        double farEdge = size - EDGE_PAN_MARGIN;
        if (coordinate > farEdge)
        {
            return Mth.clamp((coordinate - farEdge) / EDGE_PAN_MARGIN, 0.0, 1.0);
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
    public static void onRenderTickPre(TickEvent.RenderTickEvent.Pre event)
    {
        extraCameraDistanceApplied = false;
        if (!enabled || renderRotationOverridden) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        savedYaw = player.getYRot();
        savedPitch = player.getXRot();
        savedYawOld = player.yRotO;
        savedPitchOld = player.xRotO;

        player.setYRot(yaw);
        player.setXRot(TACTICAL_PITCH);
        player.yRotO = yaw;
        player.xRotO = TACTICAL_PITCH;
        renderRotationOverridden = true;
    }

    @SubscribeEvent
    public static void onRenderTickPost(TickEvent.RenderTickEvent.Post event)
    {
        if (!renderRotationOverridden) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null)
        {
            player.setYRot(savedYaw);
            player.setXRot(savedPitch);
            player.yRotO = savedYawOld;
            player.xRotO = savedPitchOld;
        }

        renderRotationOverridden = false;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event)
    {
        if (!enabled) return;
        event.setYaw(yaw);
        event.setPitch(TACTICAL_PITCH);
        event.setRoll(0.0F);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event)
    {
        if (!enabled) return;

        event.setFOV(TACTICAL_FOV);

        if (!extraCameraDistanceApplied)
        {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            // Camera.setup() has already placed the camera relative to the player.
            // First shift its focal point from the player to the free tactical
            // focus, then pull backward to the requested zoom distance.
            if (cameraFocus == null)
            {
                cameraFocus = player.position();
            }

            Vec3 desiredWorldShift = cameraFocus.subtract(player.position());
            Camera camera = event.getCamera();

            Vec3 forward = new Vec3(camera.getLookVector());
            Vec3 up = new Vec3(camera.getUpVector());
            Vec3 left = new Vec3(camera.getLeftVector());

            float localForward = (float)desiredWorldShift.dot(forward);
            float localUp = (float)desiredWorldShift.dot(up);
            float localLeft = (float)desiredWorldShift.dot(left);

            camera.move(localForward, localUp, localLeft);

            float extraDistance = Math.max(0.0F, cameraDistance - VANILLA_THIRD_PERSON_DISTANCE);
            camera.move(-extraDistance, 0.0F, 0.0F);
            extraCameraDistanceApplied = true;
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
