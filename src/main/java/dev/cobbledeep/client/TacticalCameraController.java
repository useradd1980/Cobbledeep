package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tactical-camera prototype.
 *
 * V toggles the mode. While active, the camera is forced into third person at a
 * fixed isometric-like angle. Comma/period rotate in 90 degree steps and the
 * mouse wheel changes the tactical FOV for simple zooming.
 *
 * Vanilla third-person camera placement is calculated from the player's view
 * rotation before ViewportEvent.ComputeCameraAngles fires. To keep the camera
 * orbit fixed around the player, we temporarily replace the player's render-time
 * view rotation, then restore the real gameplay rotation immediately afterward.
 */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalCameraController
{
    private static final float TACTICAL_PITCH = 55.0F;
    private static final float ROTATION_STEP = 90.0F;
    private static final double MIN_FOV = 28.0;
    private static final double MAX_FOV = 70.0;
    private static final double FOV_STEP = 4.0;

    private static boolean enabled;
    private static float yaw = 45.0F;
    private static double tacticalFov = 48.0;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;

    private static boolean renderRotationOverridden;
    private static float savedYaw;
    private static float savedPitch;
    private static float savedYawOld;
    private static float savedPitchOld;

    private TacticalCameraController() { }

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
            }
            else
            {
                minecraft.options.setCameraType(previousCameraType);
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

        // Keep the camera detached even if the player presses the vanilla F5
        // camera-cycle key while tactical mode is active.
        if (minecraft.player != null && minecraft.options.getCameraType() != CameraType.THIRD_PERSON_BACK)
        {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    @SubscribeEvent
    public static void onRenderTickPre(TickEvent.RenderTickEvent.Pre event)
    {
        if (!enabled || renderRotationOverridden) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Camera.setup() interpolates both the current and previous player view
        // rotations. Override all four values so partial ticks cannot create an
        // arc between the player's mouse-controlled view and the tactical view.
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
            // Restore the mouse/gameplay orientation immediately after rendering.
            // The tactical camera therefore does not permanently steer the player.
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
        event.setFOV(tacticalFov);
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event)
    {
        if (!enabled) return;

        if (event.getDeltaY() > 0.0)
        {
            tacticalFov = Math.max(MIN_FOV, tacticalFov - FOV_STEP);
        }
        else if (event.getDeltaY() < 0.0)
        {
            tacticalFov = Math.min(MAX_FOV, tacticalFov + FOV_STEP);
        }
        else
        {
            return;
        }

        // Prevent tactical zoom from also changing the selected hotbar slot.
        event.setCanceled(true);
    }
}
