package dev.cobbledeep.client;

import com.mojang.blaze3d.platform.InputConstants;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Key mappings for the tactical camera prototype. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TacticalCameraKeys
{
    public static final String CATEGORY = "key.categories.cobbledeep";

    public static final KeyMapping TOGGLE = new KeyMapping(
            "key.cobbledeep.tactical_camera.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY);

    public static final KeyMapping ROTATE_LEFT = new KeyMapping(
            "key.cobbledeep.tactical_camera.rotate_left",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA,
            CATEGORY);

    public static final KeyMapping ROTATE_RIGHT = new KeyMapping(
            "key.cobbledeep.tactical_camera.rotate_right",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_PERIOD,
            CATEGORY);

    public static final KeyMapping RECENTER = new KeyMapping(
            "key.cobbledeep.tactical_camera.recenter",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_HOME,
            CATEGORY);

    public static final KeyMapping PLAY_PAUSE = new KeyMapping(
            "key.cobbledeep.tactical_camera.play_pause",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE,
            CATEGORY);

    public static final KeyMapping CHARACTER_SHEET = new KeyMapping(
            "key.cobbledeep.character_sheet",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY);

    private TacticalCameraKeys() { }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event)
    {
        event.register(TOGGLE);
        event.register(ROTATE_LEFT);
        event.register(ROTATE_RIGHT);
        event.register(RECENTER);
        event.register(PLAY_PAUSE);
        event.register(CHARACTER_SHEET);
    }
}
