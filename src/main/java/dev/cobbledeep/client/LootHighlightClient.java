package dev.cobbledeep.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Shared hold-to-highlight input for lootable world objects (corpses, future chests). */
public final class LootHighlightClient {
    private LootHighlightClient() {}

    /** Poll the physical key while rendering, so releasing Tab or losing focus
     * immediately removes highlights without leaving a toggled state behind. */
    public static boolean isHeld() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.screen == null
                && mc.isWindowActive() && mc.getWindow().getWindow() != 0L
                && GLFW.glfwGetKey(mc.getWindow().getWindow(), GLFW.GLFW_KEY_TAB) == GLFW.GLFW_PRESS;
    }
}
